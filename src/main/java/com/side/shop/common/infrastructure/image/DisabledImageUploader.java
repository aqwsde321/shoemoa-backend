package com.side.shop.common.infrastructure.image;

import com.side.shop.common.application.ImageUploader;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Profile({"prod", "local"})
@Service
public class DisabledImageUploader implements ImageUploader {

    @Override
    public List<String> uploadProductImages(Long productId, List<MultipartFile> images) {
        throw new UnsupportedOperationException("이미지 업로드는 아직 설정되지 않았습니다.");
    }
}
