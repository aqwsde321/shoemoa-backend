package com.side.shop.product.presentation;

import com.side.shop.product.application.ProductService;
import com.side.shop.product.presentation.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 생성
    //    @PostMapping()
    //    public ResponseEntity<CreateProductResponseDto> createProduct(@RequestBody CreateProductDto dto) {
    //        Long productId = productService.createProduct(dto);
    //        return ResponseEntity.ok(new CreateProductResponseDto(productId));
    //    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateProductResponseDto> createProduct(
            @RequestPart("data") CreateProductDto dto, @RequestPart("images") List<MultipartFile> images) {
        Long productId = productService.createProduct(dto, images);
        return ResponseEntity.ok(new CreateProductResponseDto(productId));
    }

    @GetMapping()
    public ResponseEntity<Page<ProductSearchResult>> searchProducts(
            ProductSearchCond condition, @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductSearchResult> result = productService.searchProducts(condition, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailDto> getProductDetail(@PathVariable Long productId) {
        ProductDetailDto productDetail = productService.getProductDetail(productId);
        return ResponseEntity.ok(productDetail);
    }
}
