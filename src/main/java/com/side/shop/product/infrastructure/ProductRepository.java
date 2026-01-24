package com.side.shop.product.infrastructure;

import com.side.shop.product.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    // 상품 조회시 옵션까지 함께 조회 (N+1 문제 해결을 위한 Fetch Join)
    @Query("select p from Product p join fetch p.options")
    List<Product> findAllFetchJoin();
}
