package com.side.shop.product.infrastructure;

import static com.side.shop.product.presentation.dto.ProductSearchCond.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import com.side.shop.product.presentation.dto.ProductSearchCond;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

//@DataJpaTest
@SpringBootTest
@Transactional
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ProductRepositoryImpl productRepositoryImpl(EntityManager em) {
            return new ProductRepositoryImpl(em);
        }
    }

    @BeforeEach
    void setUp() {
        //테스트 데이터 셋업
        for (int i = 1; i <= 15; i++) {
            Product product = Product.create("상품" + i, "나이키", "설명" + i, 1000 * i);
            // 각 상품마다 옵션 2개씩 추가
            // 짝수 상품은 Red, 홀수 상품은 Blue
            String color = (i % 2 == 0) ? "Red" : "Blue";
            int price = 1000 * i; // 1000, 2000, 3000 ... 15000

            product.addOption(ProductOption.create(100, "White", 10));
            product.addOption(ProductOption.create(105, color, 10));
            productRepository.save(product);
        }

        // 영속성 컨텍스트 초기화
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Batch Size 적용 확인 - 상품 조회 후 옵션 접근 시 IN 쿼리 발생")
    void batch_size_test() {
        // given
        ProductSearchCond cond = new ProductSearchCond();
        PageRequest pageRequest = PageRequest.of(0, 2);

        // when
        Page<Product> result = productRepository.searchProducts(cond, pageRequest);
        List<Product> products = result.getContent();

        // then
        assertThat(products).hasSize(2);

        System.out.println("========== 옵션 접근 시작 ==========");
        for (Product product : products) {
            System.out.println("product.getName() = " + product.getName());
            int optionSize = product.getOptions().size();
            assertThat(optionSize).isEqualTo(2);
        }
        System.out.println("========== 옵션 접근 종료 ==========");
    }

    @Test
    @DisplayName("상품명 검색 - '상품1' 검색 시 상품1, 상품10~15 조회")
    void search_by_name() {
        // given
        ProductSearchCond cond = new ProductSearchCond();
        cond.setName("상품1"); // "상품1", "상품10", "상품11" ... "상품15"

        PageRequest pageRequest = PageRequest.of(0, 20);

        // when
        Page<Product> result = productRepository.searchProducts(cond, pageRequest);

        // then
        // 상품1, 10, 11, 12, 13, 14, 15 -> 총 7개
        assertThat(result.getContent()).hasSize(7);
        assertThat(result.getContent()).extracting("name").contains("상품1", "상품10", "상품15");
    }

    @Test
    @DisplayName("색상 검색 - 'Red' (짝수 상품)")
    void search_by_color() {
        // given
        ProductSearchCond cond = new ProductSearchCond();
        cond.setColor("Red");

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.searchProducts(cond, pageRequest);

        // then
        // 2, 4, 6, 8, 10, 12, 14 -> 총 7개
        assertThat(result.getContent()).hasSize(7);
        assertThat(result.getContent()).allMatch(p -> p.getOptions().stream()
                .anyMatch(o -> o.getColor().equals("Red")));
    }

    @Test
    @DisplayName("가격 범위 검색 - 5000원 ~ 10000원")
    void search_by_price() {
        // given
        ProductSearchCond cond = new ProductSearchCond();
        cond.setMinPrice(5000);
        cond.setMaxPrice(10000);

        PageRequest pageRequest = PageRequest.of(0, 20);

        // when
        Page<Product> result = productRepository.searchProducts(cond, pageRequest);

        // then
        // 가격은 1000 * i
        // 5000(상품5) ~ 10000(상품10) -> 5, 6, 7, 8, 9, 10 -> 총 6개
        assertThat(result.getContent()).hasSize(6);
        assertThat(result.getContent()).extracting("name").contains("상품5", "상품10");
    }

    @Test
    @DisplayName("정렬 - 가격 내림차순")
    void sort_by_price_desc() {
        // given
        ProductSearchCond cond = new ProductSearchCond();
        cond.setSortType(ProductSearchCond.SortType.PRICE_DESC);

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.searchProducts(cond, pageRequest);

        // then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getContent().get(0)).extracting("price").isEqualTo(15000);
        assertThat(result.getContent().get(9)).extracting("price").isEqualTo(6000);
    }

    @Test
    @DisplayName("색상, 가격 범위 - 복잡한 검색, 정렬 적용")
    void search_by_color_and_price_sort_by_price_desc() {
        // given
        ProductSearchCond cond = new ProductSearchCond();
        cond.setColor("Blue");
        cond.setMinPrice(5000);
        cond.setMaxPrice(14000);
        cond.setSortType(ProductSearchCond.SortType.PRICE_DESC);

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.searchProducts(cond, pageRequest);

        // then
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent().get(0)).extracting("price").isEqualTo(13000);
        assertThat(result.getContent().get(4)).extracting("price").isEqualTo(5000);
    }

}
