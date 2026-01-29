package com.side.shop.product.presentation.dto;

import com.side.shop.product.domain.Product;
import lombok.Data;

@Data
public class ProductSearchResult {
    private Long id;
    private String name;
    private String brand;
    private String description;
    private String color;
    private int price;

    public ProductSearchResult(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.brand = product.getBrand();
        this.description = product.getDescription();
        this.color = product.getColor();
        this.price = product.getPrice();
    }
}
