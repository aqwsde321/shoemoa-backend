package com.side.shop.product.presentation;

import com.side.shop.product.application.ProductService;
import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import com.side.shop.product.presentation.dto.CreateProductDto;
import com.side.shop.product.presentation.dto.CreateProductResponseDto;
import com.side.shop.product.presentation.dto.ProductSearchCond;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    //상품 생성
    @PostMapping()
    public ResponseEntity<CreateProductResponseDto> createProduct(@RequestBody CreateProductDto dto) {
        Long productId = productService.createProduct(dto);
        return ResponseEntity.ok(new CreateProductResponseDto(productId));
    }

    @GetMapping()
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) ProductSearchCond.SortType sortType,
            @PageableDefault(size = 20) Pageable pageable) {
        System.out.println("color = " + color);
        System.out.println("name = " + name);

        ProductSearchCond condition = new ProductSearchCond();
        condition.setName(name);
        condition.setSize(size);
        condition.setMinPrice(minPrice);
        condition.setMaxPrice(maxPrice);
        condition.setColor(color);
        condition.setSortType(sortType);

        Page<Product> result = productService.searchProducts(condition, pageable);
//        List<Product> content = result.getContent();
//        for (Product product : content) {
//            System.out.println("product.getName() = " + product.getName());
//            for (ProductOption option : product.getOptions()) {
//                System.out.println("option.getSize() = " + option.getSize());
//                System.out.println("option.getStock() = " + option.getStock());
//            }
//
//        }
        //Product -> ProductSearchResult 로 변환
        return ResponseEntity.ok(result);
    }


}
