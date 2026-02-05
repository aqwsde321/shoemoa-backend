package com.side.shop.cart.presentation.dto;

import lombok.Data;

@Data
public class AddCartItemRequest {
    private Long productId;
    private Long productOptionId;
    private int quantity;

    public AddCartItemRequest(Long productId, Long productOptionId, int quantity) {
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
    }
}
