package com.side.shop.cart.domain;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    @Test
    @DisplayName("생성 실패 - 수량이 1보다 작으면 예외 발생")
    void create_cart_item_fail() {
        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);
        ProductOption option = ProductOption.create(240, 10);

        assertThatThrownBy(() -> CartItem.create(product, option, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("수량 변경 성공")
    void change_quantity_success() {
        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);
        ProductOption option = ProductOption.create(240, 10);
        CartItem item = CartItem.create(product, option, 1);

        item.changeQuantity(3);

        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("수량 변경 실패 - 수량이 1보다 작으면 예외 발생")
    void change_quantity_fail() {
        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);
        ProductOption option = ProductOption.create(240, 10);
        CartItem item = CartItem.create(product, option, 2);

        assertThatThrownBy(() ->item.changeQuantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1 이상이어야 합니다.");

    }

    @Test
    @DisplayName("CartItem은 productOption의 id가 같으면 같은 옵션이다")
    void is_same_true() {
        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);

        ProductOption option = ProductOption.create(240, 10);
        ReflectionTestUtils.setField(option, "id", 1L);

        CartItem item1 = CartItem.create(product, option, 1);
        CartItem item2 = CartItem.create(product, option, 2);

        assertThat(item1.isSameOption(item2)).isTrue();
    }

}