package com.side.shop.product.presentation.dto;

import lombok.Data;

@Data
public class UpdateProductDto {
    private Long id;
    private String name;
    private String description;
}
