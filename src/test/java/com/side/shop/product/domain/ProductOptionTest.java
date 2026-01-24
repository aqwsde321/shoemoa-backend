package com.side.shop.product.domain;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductOptionTest {

    @Test
    @DisplayName("옵션 생성 실패 - 재고, 가격이 0보다 작으면 예외 발생")
    void create_option_fail() {
        assertThatThrownBy(() -> ProductOption.create(100, "Red", -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재고는 0보다 작을 수 없습니다.");
        assertThatThrownBy(() -> ProductOption.create(100, "Red", 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("가격은 0보다 작거나 같을 수 없습니다.");
        assertThatThrownBy(() -> ProductOption.create(100, "Red", 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("가격은 0보다 작거나 같을 수 없습니다.");
    }
}
