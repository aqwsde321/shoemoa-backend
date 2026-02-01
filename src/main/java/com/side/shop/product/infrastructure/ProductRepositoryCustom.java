package com.side.shop.product.infrastructure;

import com.side.shop.product.presentation.dto.ProductSearchCond;
import com.side.shop.product.presentation.dto.ProductSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<ProductSearchResult> searchProducts(ProductSearchCond condition, Pageable pageable);
}
