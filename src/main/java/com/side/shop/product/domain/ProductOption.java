package com.side.shop.product.domain;

import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;

import com.side.shop.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ProductOption extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = LAZY)
    private Product product;

    private int productSize;
    private int stock;

    public static ProductOption create(int size, int stock) {
        if (stock < 0) throw new IllegalArgumentException("재고는 0보다 작을 수 없습니다.");

        return new ProductOption(size, stock);
    }

    private ProductOption(int size, int stock) {
        this.productSize = size;
        this.stock = stock;
    }

    public void assignProduct(Product product) {
        this.product = product;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("수량은 0보다 작거나 같을 수 없습니다.");
        if (stock < quantity) throw new IllegalStateException("재고가 부족합니다.");
        stock -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("수량은 0보다 작거나 같을 수 없습니다.");
        stock += quantity;
    }

    public void updateInfo(int size, int stock) {
        if (stock < 0) throw new IllegalArgumentException("재고는 0보다 작을 수 없습니다.");
        this.productSize = size;
        this.stock = stock;
    }
}
