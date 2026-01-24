package com.side.shop.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("상품 생성 성공")
    void create_product_success() {
        Product product = Product.create("나이키 운동화", "편안한 운동화입니다.");

        assertThat(product.getName()).isEqualTo("나이키 운동화");
        assertThat(product.getDescription()).isEqualTo("편안한 운동화입니다.");
        assertThat(product.getOptions()).isEmpty();
    }

    @Test
    @DisplayName("상품명이 비어있으면 생성 실패")
    void create_product_fail_empty_name() {
        assertThatThrownBy(() -> Product.create(null, "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품명은 필수입니다.");

        assertThatThrownBy(() -> Product.create("", "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품명은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 추가 성공 - 양방향 연관관계 설정 확인")
    void add_option_success() {
        // given
        Product product = Product.create("티셔츠", "기본 티셔츠");
        ProductOption option = ProductOption.create(95, "White", 100, 15000);

        // when
        product.addOption(option);

        // then
        assertThat(product.getOptions()).hasSize(1);
        assertThat(product.getOptions().get(0)).isEqualTo(option);
        assertThat(option.getProduct()).isEqualTo(product);
    }

    //ReflectionTestUtils 사용
    @Test
    @DisplayName("옵션 ID로 조회 성공 (Reflection 사용)")
    void get_option_success() {
        // given
        Product product = Product.create("상품", "설명");
        ProductOption option = ProductOption.create(100, "Red", 10, 1000);

        // 도메인 테스트에서는 DB가 없으므로 ID가 null입니다.
        // ReflectionTestUtils를 사용하여 강제로 ID를 주입해 테스트할 수 있습니다.
        ReflectionTestUtils.setField(option, "id", 1L);

        product.addOption(option);

        // when
        ProductOption findOption = product.getOption(option.getId());

        // then
        assertThat(findOption).isEqualTo(option);
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID 조회 시 예외 발생")
    void get_option_fail() {
        Product product = Product.create("상품", "설명");

        assertThatThrownBy(() -> product.getOption(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("옵션 없음");
    }

}
