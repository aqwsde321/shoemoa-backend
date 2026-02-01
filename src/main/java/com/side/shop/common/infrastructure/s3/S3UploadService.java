package com.side.shop.common.infrastructure.s3;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * 상품 이미지 다중 업로드
     */
    public List<String> uploadProductImages(Long productId, List<MultipartFile> files) {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        return files.stream()
                .map(file -> uploadProductImage(productId, file))
                .toList();
    }

    /**
     * 상품 이미지 단일 업로드
     */
    private String uploadProductImage(Long productId, MultipartFile file) {
        validateImageFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String s3Key = generateProductImageKey(productId, extension);

        try {
            s3Template.upload(bucket, s3Key, file.getInputStream());

            String fileUrl = generateFileUrl(s3Key);
            log.info("S3 상품 이미지 업로드 성공: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("S3 상품 이미지 업로드 실패", e);
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3 상품 이미지 key 생성
     * products/{productId}/images/{uuid}.{ext}
     */
    private String generateProductImageKey(Long productId, String extension) {
        return "products/%d/images/%s%s"
                .formatted(productId, UUID.randomUUID(), extension);
    }

    /**
     * S3 파일 URL 생성
     */
    private String generateFileUrl(String s3Key) {
        return "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(bucket, region, s3Key);
    }

    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
