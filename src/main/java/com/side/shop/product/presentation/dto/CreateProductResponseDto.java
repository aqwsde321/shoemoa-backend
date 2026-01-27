package com.side.shop.product.presentation.dto;

import lombok.Data;

@Data
public class CreateProductResponseDto {
    private Long productId;

    public CreateProductResponseDto(Long productId) {
        this.productId = productId;
    }
}
