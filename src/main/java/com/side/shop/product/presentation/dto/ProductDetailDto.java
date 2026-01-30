package com.side.shop.product.presentation.dto;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDetailDto {
    private String name;
    private String brand;
    private String color;
    private int price;

    private List<ProductOptionDetailDto> options;

    public ProductDetailDto(Product product) {
        this.name = product.getName();
        this.brand = product.getBrand();
        this.color = product.getColor();
        this.price = product.getPrice();

        this.options = product.getOptions().stream().map(ProductOptionDetailDto::new).toList();

    }
}

