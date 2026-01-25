package com.side.shop.product.presentation.dto;

import lombok.Data;

@Data
public class UpdateProductOptionDto {
    private Long id;
    private int size;
    private int stock;
    private String color;
}
