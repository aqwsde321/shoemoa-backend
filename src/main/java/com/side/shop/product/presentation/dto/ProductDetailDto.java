package com.side.shop.product.presentation.dto;

import com.side.shop.product.domain.Product;
import java.util.List;
import lombok.Data;

@Data
public class ProductDetailDto {
    private String name;
    private String brand;
    private String color;
    private int price;

    private List<ProductOptionDetailDto> options;
    private List<ProductImageDto> images;

    public ProductDetailDto(Product product) {
        this.name = product.getName();
        this.brand = product.getBrand();
        this.color = product.getColor();
        this.price = product.getPrice();

        this.images = product.getImages().stream().map(ProductImageDto::new).toList();
        this.options =
                product.getOptions().stream().map(ProductOptionDetailDto::new).toList();
    }
}
