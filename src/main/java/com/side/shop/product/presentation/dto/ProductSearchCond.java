package com.side.shop.product.presentation.dto;

import lombok.Data;

// %상품명% and 사이즈 and 색상 and 가격 range, 페이징 가능, 정렬 - 최신순, 가격순, 상품명순
@Data
public class ProductSearchCond {

    private String name;
    private int size;
    private String color;
    private int minPrice;
    private int maxPrice;
}
