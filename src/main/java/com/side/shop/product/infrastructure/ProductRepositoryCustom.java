package com.side.shop.product.infrastructure;

import com.side.shop.product.domain.Product;
import com.side.shop.product.presentation.dto.ProductSearchCond;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> searchProducts(ProductSearchCond condition, Pageable pageable);
}
