package com.side.shop.product.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int size;
    private int stock;
    private int price;
    private String color;

    public static ProductOption create(
            int size,
            String color,
            int stock,
            int price
    ) {
        if (stock < 0) throw new IllegalArgumentException("재고는 0보다 작을 수 없습니다.");
        if (price <= 0) throw new IllegalArgumentException("가격은 0보다 작거나 같을 수 없습니다.");

        return new ProductOption(size, color, stock, price);
    }

    private ProductOption(int size, String color, int stock, int price) {
        this.size = size;
        this.color = color;
        this.stock = stock;
        this.price = price;
    }

    public void assignProduct(Product product) {
        this.product = product;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException();
        if (stock < quantity) throw new IllegalStateException("재고 부족");
        stock -= quantity;
    }
}
