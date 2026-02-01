package com.side.shop.product.presentation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CreateProductDto {
    private String name;
    private String brand;
    private String description;
    private String color;
    private int price;

    public CreateProductDto(String name, String brand, String description, String color, int price) {
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.color = color;
        this.price = price;
    }
}
