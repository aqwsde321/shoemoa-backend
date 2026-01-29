package com.side.shop.product.domain;

import com.side.shop.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String brand;
    private String description;
    private String color;
    private int price;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    // 상품 생성
    public static Product create(String name, String brand, String description, String color, int price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("브랜드명은 필수입니다.");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("색상은 필수입니다.");
        }
        return new Product(name, brand, description, color, price);
    }

    private Product(String name, String brand, String description, String color, int price) {
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.color = color;
        this.price = price;
    }

    // 옵션 추가
    // 연관관계 메서드
    public void addOption(ProductOption option) {
        options.add(option);
        option.assignProduct(this);
    }

    public void removeOption(ProductOption option) {
        options.remove(option);
        option.assignProduct(null);
    }

    public ProductOption getOption(Long optionId) {
        return options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다."));
    }

    public void updateInfo(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        this.name = name;
        this.description = description;
    }
}
