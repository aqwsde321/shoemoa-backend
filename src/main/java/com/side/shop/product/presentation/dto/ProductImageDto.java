package com.side.shop.product.presentation.dto;

import com.side.shop.product.domain.ProductImage;
import lombok.Data;

@Data
public class ProductImageDto {
    private String imageUrl;
    private int sortOrder;
    private boolean thumbnail;

    public ProductImageDto(ProductImage img) {
        this.imageUrl = img.getImageUrl();
        this.sortOrder = img.getSortOrder();
        this.thumbnail = img.isThumbnail();
    }
}
