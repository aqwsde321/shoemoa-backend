package com.side.shop.product.presentation.dto;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductSearchResult {
    private Long id;
    private String name;
    private String brand;
    private String description;
    private String color;
    private int price;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

    @QueryProjection
    public ProductSearchResult(
            Long id,
            String name,
            String brand,
            String description,
            String color,
            int price,
            String thumbnailUrl,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.color = color;
        this.price = price;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt;
    }
}
