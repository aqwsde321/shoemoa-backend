package com.side.shop.product.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    //상품 생성
    public static Product create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        return new Product(name, description);
    }

    private Product(String name, String description) {
        this.name = name;
        this.description = description;
    }

    //옵션 추가
    //연관관계 메서드
    public void addOption(ProductOption option) {
        options.add(option);
        option.assignProduct(this);
    }

    public ProductOption getOption(Long optionId) {
        return options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));
    }
}
