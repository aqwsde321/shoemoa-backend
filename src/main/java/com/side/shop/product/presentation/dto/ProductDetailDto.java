package com.side.shop.product.presentation.dto;

import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductImage;
import java.util.List;
import lombok.Data;

@Data
public class ProductDetailDto {
    private String name;
    private String brand;
    private String color;
    private int price;

    private List<ProductOptionDetailDto> options;
    private List<ProductImageDto> images;

    //    public ProductDetailDto(Product product) {
    //        this.name = product.getName();
    //        this.brand = product.getBrand();
    //        this.color = product.getColor();
    //        this.price = product.getPrice();
    //
    //        this.images = product.getImages().stream().map(ProductImageDto::new).toList();
    //        this.options =
    //                product.getOptions().stream().map(ProductOptionDetailDto::new).toList();
    //    }

    public static ProductDetailDto of(Product product, List<ProductImage> images) {
        ProductDetailDto dto = new ProductDetailDto();

        dto.name = product.getName();
        dto.brand = product.getBrand();
        dto.price = product.getPrice();

        dto.options =
                product.getOptions().stream().map(ProductOptionDetailDto::new).toList();

        dto.images = images.stream().map(ProductImageDto::new).toList();

        return dto;
    }
}
