package com.side.shop.product.presentation.dto;

import lombok.Data;

@Data
public class CreateProductDto {
    private String name;
    private String brand;
    private String description;
    private String color;
    private int price;
}
