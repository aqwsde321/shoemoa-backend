package com.side.shop.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("상품 저장 및 조회")
    void save_and_find() {
        // given
        Product product = Product.create("아디다스 운동화", "아디다스", "따뜻한 신발입니다.", "black", 50000);
        productRepository.save(product);

        // when
        Product findProduct = productRepository.findById(product.getId()).orElseThrow();

        // then
        assertThat(findProduct).isEqualTo(product);
        assertThat(findProduct.getName()).isEqualTo("아디다스 운동화");
    }

    @Test
    @DisplayName("상품과 옵션 함께 저장 (Cascade)")
    void save_with_options() {
        // given
        Product product = Product.create("나이키 운동화", "나이키", "러닝화입니다.", "black", 100000);
        ProductOption option1 = ProductOption.create(220, 10);
        ProductOption option2 = ProductOption.create(230, 5);

        product.addOption(option1);
        product.addOption(option2);

        productRepository.save(product);

        // when
        em.flush();
        em.clear();

        Product findProduct = productRepository.findById(product.getId()).orElseThrow();

        // then
        assertThat(findProduct.getOptions()).hasSize(2);
        assertThat(findProduct.getOption(option1.getId()).getProductSize()).isEqualTo(220);
    }

    @Test
    @DisplayName("상품, 옵션 모두 조회 (N+1 문제 확인 및 Fetch Join 테스트)")
    void fetch_join_test() {
        // given
        Product product1 = Product.create("상품A", "나이키", "상품A desc", "yellow", 100000);
        product1.addOption(ProductOption.create(220, 1));
        product1.addOption(ProductOption.create(230, 3));
        productRepository.save(product1);

        Product product2 = Product.create("상품B", "아디다스", "상품B desc", "white", 150000);
        product2.addOption(ProductOption.create(220, 1));
        productRepository.save(product2);

        em.flush();
        em.clear();

        // when
        // 일반 findAll은 N+1 발생 가능성 있음 (Lazy Loading)
        // Fetch Join 사용 시 한 번의 쿼리로 조회
        List<Product> products = productRepository.findAllFetchJoin();

        // then
        assertThat(products).hasSize(2);
        // 이미 로딩되었으므로 추가 쿼리 없이 접근 가능
        assertThat(products.get(0).getOptions().get(0).getProductSize()).isEqualTo(220);

        for (Product product : products) {
            System.out.println("product = " + product.getName());
            for (ProductOption option : product.getOptions()) {
                System.out.println("option: size=" + option.getProductSize());
            }
        }
    }

    @Test
    @DisplayName("상품ID로 상품 상세 조회")
    void get_product_detail() {
        // given
        Product product = Product.create("뉴발란스 860 V2 블랙", "뉴발란스", "상세 설명", "black", 169000);
        Product savedProduct = productRepository.save(product);
        ProductOption option1 = ProductOption.create(220, 10);
        ProductOption option2 = ProductOption.create(230, 20);
        ProductOption option3 = ProductOption.create(240, 30);
        product.addOption(option1);
        product.addOption(option2);
        product.addOption(option3);

        // when
        Product productDetail =
                productRepository.findDetailById(product.getId()).orElseThrow();

        // then
        assertThat(productDetail.getId()).isEqualTo(savedProduct.getId());
        assertThat(productDetail.getOptions().size()).isEqualTo(3);
    }
}
