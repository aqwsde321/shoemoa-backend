package com.side.shop.product.presentation;

import com.side.shop.product.application.ProductService;
import com.side.shop.product.presentation.dto.CreateProductDto;
import com.side.shop.product.presentation.dto.CreateProductResponseDto;
import com.side.shop.product.presentation.dto.ProductSearchCond;
import com.side.shop.product.presentation.dto.ProductSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 생성
    @PostMapping()
    public ResponseEntity<CreateProductResponseDto> createProduct(@RequestBody CreateProductDto dto) {
        Long productId = productService.createProduct(dto);
        return ResponseEntity.ok(new CreateProductResponseDto(productId));
    }

    @GetMapping()
    public ResponseEntity<Page<ProductSearchResult>> searchProducts(
            ProductSearchCond condition, @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductSearchResult> result = productService.searchProducts(condition, pageable);

        return ResponseEntity.ok(result);
    }
}
