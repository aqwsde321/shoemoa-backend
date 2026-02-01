package com.side.shop.common.infrastructure.s3;

import com.side.shop.common.application.ImageUploader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Profile({"prod", "local"})
@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService implements ImageUploader {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloudfront.domain}")
    private String cloudFrontDomain;

    @Override
    public List<String> uploadProductImages(Long productId, List<MultipartFile> files) {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        return files.stream().map(file -> uploadProductImage(productId, file)).toList();
    }

    private String uploadProductImage(Long productId, MultipartFile file) {
        validateImageFile(file);

        String s3Key = generateProductImageKey(productId, extractExtension(file.getOriginalFilename()));

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    // 브라우저와 CloudFront 캐시 모두 장기 활용
                    // public: 모든 사용자 및 캐시 서버에서 캐시 가능
                    // max-age=31536000: TTL 1년 (초 단위)
                    // immutable: URL이 바뀌지 않는 한 재검증 불필요 → 불필요한 요청 방지
                    .cacheControl("public, max-age=31536000, immutable")
                    // 파일 타입 지정: 브라우저가 이미지로 올바르게 처리
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return generateCloudFrontUrl(s3Key);

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    private String generateProductImageKey(Long productId, String extension) {
        return "products/%d/images/%s%s".formatted(productId, UUID.randomUUID(), extension);
    }

    private String generateCloudFrontUrl(String s3Key) {
        return "https://%s/%s".formatted(cloudFrontDomain, s3Key);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("10MB 초과");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
