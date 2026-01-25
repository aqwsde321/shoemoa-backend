package com.side.shop.product.infrastructure;

import static com.side.shop.product.domain.QProduct.product;
import static com.side.shop.product.domain.QProductOption.*;
import static com.side.shop.product.presentation.dto.ProductSearchCond.SortType.LATEST;

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
    // OneToMany Collection 조회, default_batch_fetch_size 적용
    @Override
    public Page<Product> searchProducts(ProductSearchCond condition, Pageable pageable) {
        //Fetch Join X
        //Product 기준으로 페이징
        List<Product> content = jpaQueryFactory
                .select(product).distinct()
                .from(product)
                .join(product.options, productOption)
                .where(
                        productNameContains(condition.getName()),
                        productSizeEq(condition.getSize()),
                        productColorEq(condition.getColor()),
                        productPriceBetween(condition.getMinPrice(), condition.getMaxPrice()))
                .orderBy(getOrderSpecifier(condition.getSortType()))
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
            return product.price.between(minPrice, maxPrice);
        } else if (minPrice != null) {
            return product.price.goe(minPrice);
        } else if (maxPrice != null) {
            return product.price.loe(maxPrice);
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

    private OrderSpecifier<?> getOrderSpecifier(ProductSearchCond.SortType sortType) {
        if (sortType == null) {
            return product.createdAt.desc(); // 기본값: 최신순
        }

        switch (sortType) {
            case LATEST:
                return product.createdAt.desc();
            case PRICE_ASC:
                return product.price.asc();
            case PRICE_DESC:
                return product.price.desc();
            case NAME_ASC:
                return product.name.asc();
            default:
                return product.createdAt.desc();
        }
    }

    private OrderSpecifier<?> createOrder(Sort sort) {
        try {
            Sort.Order order = sort.iterator().next();

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            return switch (order.getProperty()) {
                case "price" -> new OrderSpecifier<>(direction, product.price);
                case "name" -> new OrderSpecifier<>(direction, product.name);
                case "createdAt" -> new OrderSpecifier<>(direction, product.createdAt); // 최신순용
                default -> new OrderSpecifier<>(Order.DESC, product.createdAt);
            };
        } catch (Exception e) {
            return new OrderSpecifier<>(Order.DESC, product.createdAt);
        }
    }
}
