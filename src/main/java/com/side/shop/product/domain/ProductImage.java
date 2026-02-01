package com.side.shop.product.domain;

import com.side.shop.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private String imageUrl;
    private int sortOrder;
    private boolean thumbnail;

    public static ProductImage create(String url, int order, boolean thumbnail) {
        ProductImage img = new ProductImage();
        img.imageUrl = url;
        img.sortOrder = order;
        img.thumbnail = thumbnail;
        return img;
    }

    public void assignProduct(Product product) {
        this.product = product;
    }
}
