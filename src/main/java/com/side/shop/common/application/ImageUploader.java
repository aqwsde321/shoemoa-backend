package com.side.shop.common.application;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ImageUploader {
    List<String> uploadProductImages(Long productId, List<MultipartFile> images);
}
