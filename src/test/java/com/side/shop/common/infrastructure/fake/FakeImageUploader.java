package com.side.shop.common.infrastructure.fake;

import com.side.shop.common.application.ImageUploader;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("test")
public class FakeImageUploader implements ImageUploader {

    @Override
    public List<String> uploadProductImages(Long productId, List<MultipartFile> images) {
        return images.stream()
                .map(file -> "https://fake/" + file.getOriginalFilename())
                .toList();
    }
}
