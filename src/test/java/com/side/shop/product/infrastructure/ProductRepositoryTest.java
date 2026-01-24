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
        Product product = Product.create("후드티", "따뜻한 후드티");
        productRepository.save(product);

        // when
        Product findProduct = productRepository.findById(product.getId()).orElseThrow();

        // then
        assertThat(findProduct).isEqualTo(product);
        assertThat(findProduct.getName()).isEqualTo("후드티");
    }

    @Test
    @DisplayName("상품과 옵션 함께 저장 (Cascade)")
    void save_with_options() {
        // given
        Product product = Product.create("청바지", "스판 청바지");
        ProductOption option1 = ProductOption.create(30, "Blue", 10, 30000);
        ProductOption option2 = ProductOption.create(32, "Blue", 5, 30000);

        product.addOption(option1);
        product.addOption(option2);

        productRepository.save(product);

        // when
        em.flush();
        em.clear();

        Product findProduct = productRepository.findById(product.getId()).orElseThrow();

        // then
        assertThat(findProduct.getOptions()).hasSize(2);
        assertThat(findProduct.getOption(option1.getId()).getSize()).isEqualTo(30);
    }

    @Test
    @DisplayName("상품, 옵션 모두 조회 (N+1 문제 확인 및 Fetch Join 테스트)")
    void fetch_join_test() {
        // given
        Product product1 = Product.create("상품A", "상품A desc");
        product1.addOption(ProductOption.create(1, "Red", 1, 1000));
        product1.addOption(ProductOption.create(1, "Black", 3, 1000));
        productRepository.save(product1);

        Product product2 = Product.create("상품B", "상품B desc");
        product2.addOption(ProductOption.create(2, "Blue", 1, 2000));
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
        assertThat(products.get(0).getOptions().get(0).getColor()).isEqualTo("Red");

        for (Product product : products) {
            System.out.println("product = " + product.getName());
            for (ProductOption option : product.getOptions()) {
                System.out.println("option color= " + option.getColor() + ", size=" + option.getSize());
            }
        }
    }
}
