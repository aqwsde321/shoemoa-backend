package com.side.shop.product.infrastructure;

import com.side.shop.product.domain.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    // 상품 상세 images 전용 쿼리
    List<ProductImage> findByProductIdOrderBySortOrder(Long productId);
}
