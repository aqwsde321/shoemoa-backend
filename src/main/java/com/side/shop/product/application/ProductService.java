package com.side.shop.product.application;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import com.side.shop.product.infrastructure.ProductRepository;
import com.side.shop.product.presentation.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Long createProduct(CreateProductDto dto) {
        Product product =
                Product.create(dto.getName(), dto.getBrand(), dto.getDescription(), dto.getColor(), dto.getPrice());
        productRepository.save(product);

        return product.getId();
    }

    public Page<ProductSearchResult> searchProducts(ProductSearchCond condition, Pageable pageable) {
        Page<Product> page = productRepository.searchProducts(condition, pageable);

        return page.map(ProductSearchResult::new);
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
