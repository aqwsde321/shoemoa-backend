package com.side.shop.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.side.shop.product.domain.Product;
import com.side.shop.product.infrastructure.ProductRepository;
import com.side.shop.product.presentation.dto.CreateProductDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {
    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductService productService;

    @Test
    @DisplayName("상품 생성 시 이미지 URL 저장")
    void create_product_save_image_url() {
        // given
        CreateProductDto dto = new CreateProductDto("나이키 에어포스", "나이키", "설명입니다.", "white", 100000);

        MockMultipartFile image1 =
                new MockMultipartFile("images", "shoe1.jpg", "image/jpeg", "fake-image-1".getBytes());

        MockMultipartFile image2 =
                new MockMultipartFile("images", "shoe2.jpg", "image/jpeg", "fake-image-2".getBytes());

        // when
        Long productId = productService.createProduct(dto, List.of(image1, image2));

        // then
        Product product = productRepository.findById(productId).get();

        assertThat(product.getImages()).hasSize(2);
        assertThat(product.getImages()).anyMatch(image -> image.getImageUrl().contains("https://fake/"));
    }
}
