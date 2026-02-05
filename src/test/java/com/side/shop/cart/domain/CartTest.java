package com.side.shop.cart.domain;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    @DisplayName("장바구니 생성 성공")
    void cart_create_success() {
        Cart cart = Cart.create(1L);

        assertThat(cart.getMemberId()).isEqualTo(1L);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("같은 옵션 상품 추가시 수량만 증가")
    void same_option_add_quantity() {
        Cart cart = Cart.create(1L);

        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);
        ProductOption option = ProductOption.create(240, 10);
        ReflectionTestUtils.setField(option, "id", 10L);

        CartItem item1 = CartItem.create(product, option, 2);
        CartItem item2 = CartItem.create(product, option, 3);

        cart.addItem(item1);
        cart.addItem(item2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("다른 옵션이면 새 아이템 추가")
    void other_option_add_new_item() {
        Cart cart = Cart.create(1L);

        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);

        ProductOption option1 = ProductOption.create(240, 10);
        ProductOption option2 = ProductOption.create(250, 10);
        ReflectionTestUtils.setField(option1, "id", 1L);
        ReflectionTestUtils.setField(option2, "id", 2L);

        cart.addItem(CartItem.create(product, option1, 1));
        cart.addItem(CartItem.create(product, option2, 1));

        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("수량을 0으로 변경하면 아이템 삭제")
    void quantity_change_to_zero_remove_item() {
        Cart cart = Cart.create(1L);
        Product product = Product.create("나이키 에어맥스 95", "나이키", "나이키 신발", "white", 200000);
        ProductOption option = ProductOption.create(240, 10);
        ReflectionTestUtils.setField(option, "id", 1L);

        CartItem item = CartItem.create(product, option, 2);
        cart.addItem(item);

        ReflectionTestUtils.setField(item, "id", 100L);

        cart.changeItemQuantity(100L, 0, null);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 아이템 변경시 예외 발생")
    void change_item_quantity_fail() {
        Cart cart = Cart.create(1L);

        assertThatThrownBy(() -> cart.changeItemQuantity(999L, 3, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("장바구니에 없는 상품입니다.");
    }

}