package com.side.shop.cart.domain;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = LAZY)
    private Cart cart;

    @ManyToOne(fetch = LAZY)
    private Product product;

    @ManyToOne(fetch = LAZY)
    private ProductOption productOption;

    private int quantity;

    public static CartItem create(Product product, ProductOption option, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        CartItem item = new CartItem();
        item.product = product;
        item.productOption = option;
        item.quantity = quantity;
        return item;
    }

    public void assignCart(Cart cart) {
        this.cart = cart;
    }

    public boolean isSameOption(CartItem other) {
        return this.productOption.getId().equals(other.productOption.getId());
    }

    public void changeQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public void increase(int delta) {
        this.quantity += delta;
    }
}
