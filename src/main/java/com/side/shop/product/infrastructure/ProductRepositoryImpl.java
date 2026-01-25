package com.side.shop.product.infrastructure;

import static com.side.shop.product.domain.QProduct.product;
import static com.side.shop.product.domain.QProductOption.*;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.side.shop.product.domain.Product;
import com.side.shop.product.presentation.dto.ProductSearchCond;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public ProductRepositoryImpl(EntityManager em) {
        this.jpaQueryFactory = new JPAQueryFactory(em);
    }
    // 검색조건 - 상품명 and 사이즈 and 색상 and 가격
    // 정렬 - 최신순, 가격순, 상품명순
    @Override
    public Page<Product> searchProducts(ProductSearchCond condition, Pageable pageable) {
        List<Product> content = jpaQueryFactory
                .select(product).distinct()
                .from(product)
                .join(product.options, productOption)
                .where(
                        productNameContains(condition.getName()),
                        productSizeEq(condition.getSize()),
                        productColorEq(condition.getColor()),
                        productPriceBetween(condition.getMinPrice(), condition.getMaxPrice()))
                .orderBy(createOrder(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(product.count())
                .from(product)
                .join(product.options, productOption)
                .where(
                        productNameContains(condition.getName()),
                        productSizeEq(condition.getSize()),
                        productColorEq(condition.getColor()),
                        productPriceBetween(condition.getMinPrice(), condition.getMaxPrice()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression productPriceBetween(Integer minPrice, Integer maxPrice) {
        if (minPrice != null && maxPrice != null) {
            return productOption.price.between(minPrice, maxPrice);
        } else if (minPrice != null) {
            return productOption.price.goe(minPrice);
        } else if (maxPrice != null) {
            return productOption.price.loe(maxPrice);
        }
        return null;
    }

    private BooleanExpression productColorEq(String color) {
        return StringUtils.hasText(color) ? productOption.color.eq(color) : null;
    }

    private BooleanExpression productSizeEq(Integer size) {
        return size != null ? productOption.size.eq(size) : null;
    }

    private BooleanExpression productNameContains(String productName) {
        return StringUtils.hasText(productName) ? product.name.containsIgnoreCase(productName) : null;
    }

    private OrderSpecifier<?> createOrder(Sort sort) {
        try {
            Sort.Order order = sort.iterator().next();

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            return switch (order.getProperty()) {
                case "price" -> new OrderSpecifier<>(direction, productOption.price);
                case "name" -> new OrderSpecifier<>(direction, product.name);
                case "createdAt" -> new OrderSpecifier<>(direction, product.createdAt); // 최신순용
                default -> new OrderSpecifier<>(Order.DESC, product.createdAt);
            };
        } catch (Exception e) {
            return new OrderSpecifier<>(Order.DESC, product.createdAt);
        }
    }
}
