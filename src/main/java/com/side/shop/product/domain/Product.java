package com.side.shop.product.domain;

import static jakarta.persistence.CascadeType.*;
import static lombok.AccessLevel.*;

import com.side.shop.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String brand;
    private String description;
    private String color;
    private int price;

    @OneToMany(mappedBy = "product", cascade = ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = ALL, orphanRemoval = true)
    @OrderBy("productSize ASC")
    private List<ProductOption> options = new ArrayList<>();

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

    // 이미지 추가
    // 연관관계 메서드
    public void addImages(List<String> imageUrls) {
        int start = images.size();
        for (int i = 0; i < imageUrls.size(); i++) {
            // 첫번째 이미지를 썸네일로
            boolean thumbnail = i == 0;
            ProductImage image = ProductImage.create(imageUrls.get(i), start + i, thumbnail);
            image.assignProduct(this);
            images.add(image);
        }
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
