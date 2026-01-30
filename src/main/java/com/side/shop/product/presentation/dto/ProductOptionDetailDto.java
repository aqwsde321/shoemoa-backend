package com.side.shop.product.presentation.dto;

import com.side.shop.product.domain.ProductOption;
import lombok.Data;

@Data
public class ProductOptionDetailDto {
    private int size;
    private int stock;

    public ProductOptionDetailDto(ProductOption productOption) {
        this.size = productOption.getSize();
        this.stock = productOption.getStock();
    }
}
