package com.side.shop.product.presentation.dto;

import lombok.Data;

// %상품명% and 사이즈 and 색상 and 가격 range
// 정렬 - 최신순, 가격순, 상품명순
@Data
public class ProductSearchCond {

    private String name;
    private Integer productSize;
    private String color;
    private Integer minPrice;
    private Integer maxPrice;
    private SortType sortType; // 정렬 타입

    public enum SortType {
        LATEST, // 최신순
        PRICE_ASC, // 가격 낮은순
        PRICE_DESC, // 가격 높은순
        NAME_ASC, // 상품명순 (가나다)
    }
}
