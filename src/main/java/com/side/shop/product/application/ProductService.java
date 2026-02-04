package com.side.shop.product.application;

import com.side.shop.common.application.ImageUploader;
import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductImage;
import com.side.shop.product.domain.ProductOption;
import com.side.shop.product.infrastructure.ProductImageRepository;
import com.side.shop.product.infrastructure.ProductRepository;
import com.side.shop.product.presentation.dto.*;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ImageUploader imageUploader;

    //    @Transactional
    //    public Long createProduct(CreateProductDto dto) {
    //        Product product =
    //                Product.create(dto.getName(), dto.getBrand(), dto.getDescription(), dto.getColor(),
    // dto.getPrice());
    //        productRepository.save(product);
    //
    //        return product.getId();
    //    }

    @Transactional
    public Long createProduct(CreateProductDto dto, List<MultipartFile> images) {
        Product product =
                Product.create(dto.getName(), dto.getBrand(), dto.getDescription(), dto.getColor(), dto.getPrice());
        // NPE 방어
        List<CreateProductOptionDto> options =
                Optional.ofNullable(dto.getOptions()).orElse(List.of());
        for (CreateProductOptionDto option : options) {
            product.addOption(ProductOption.create(option.getSize(), option.getStock()));
        }
        productRepository.save(product);

        /* TODO
         * S3 업로드는 트랜잭션 롤백 대상이 아님
         * 중간에 S3는 성공했는데 DB가 롤백되면 S3에 고아 이미지 남음
         * - 실패하면 그냥 전체 실패로 보고 수동 정리
         * - 이미지 업로드를 트랜잭션 밖에서
         * - 이벤트 기반(@TransactionalEventListener(AFTER_COMMIT))
         */
        // 1. S3 업로드
        List<String> imageUrls = imageUploader.uploadProductImages(product.getId(), images);

        // 2. Entity에 위임
        product.addImages(imageUrls);

        return product.getId();
    }

    public Page<ProductSearchResult> searchProducts(ProductSearchCond condition, Pageable pageable) {

        return productRepository.searchProducts(condition, pageable);
    }

    public ProductDetailDto getProductDetail(Long productId) {
        Product product = productRepository
                .findDetailById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrder(productId);

        return ProductDetailDto.of(product, images);
    }

    @Transactional
    public Long createOptions(Long productId, List<CreateProductOptionDto> options) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + productId));

        for (CreateProductOptionDto option : options) {
            ProductOption productOption = ProductOption.create(option.getSize(), option.getStock());
            product.addOption(productOption);
        }
        return productId;
    }

    @Transactional
    public Long updateProduct(UpdateProductDto dto) {
        Product product = productRepository
                .findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + dto.getId()));

        product.updateInfo(dto.getName(), dto.getDescription());

        return dto.getId();
    }

    @Transactional
    public Long updateOptions(Long productId, List<UpdateProductOptionDto> options) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + productId));

        for (UpdateProductOptionDto option : options) {
            product.getOption(option.getId()).updateInfo(option.getSize(), option.getStock());
        }

        return productId;
    }

    @Transactional
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    @Transactional
    public void deleteOptions(Long productId, List<Long> optionIds) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + productId));

        for (Long optionId : optionIds) {
            ProductOption option = product.getOption(optionId);
            product.removeOption(option);
        }
    }

    public Product getProduct(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + productId));
    }
}
