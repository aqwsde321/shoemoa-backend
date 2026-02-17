# AWS S3 + CloudFront CDN 세팅 가이드

이 문서는 AWS S3와 CloudFront를 사용하여 이미지 파일 업로드 및 CDN 배포를 설정하는 방법을 단계별로 정리한 문서입니다.

---

## 1. S3 버킷 생성 및 권한 설정

1. AWS 콘솔에서 S3 버킷 생성

    * 이름: `s3-yourbucketname`
    * 리전: 서울(ap-northeast-2)
    * 퍼블릭 액세스 차단: **모두 활성화** (버킷과 객체를 안전하게 보호)

2. 버킷 정책 작성 (CloudFront만 접근 가능)

```json
{
    "Version": "2008-10-17",
    "Id": "PolicyForCloudFrontPrivateContent",
    "Statement": [
        {
            "Sid": "AllowCloudFrontServicePrincipal",
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudfront.amazonaws.com"
            },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::s3-yourbucketname/*",
            "Condition": {
                "StringEquals": {
                    "AWS:SourceArn": "arn:aws:cloudfront::<ACCOUNT_ID>:distribution/<DISTRIBUTION_ID>"
                }
            }
        }
    ]
}
```

* `<ACCOUNT_ID>`와 `<DISTRIBUTION_ID>`는 본인 CloudFront 배포에 맞게 수정
* 이렇게 하면 S3는 CloudFront에서만 접근 가능, 일반 퍼블릭 접근 차단

3. CORS 설정 (웹 브라우저에서 직접 접근 시 필요)

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedOrigins": ["*"]
  }
]
```

* CDN을 통해서만 접근할 경우 필수 아님

---

## 2. CloudFront 배포 생성

1. CloudFront 콘솔 → `Create Distribution`

2. Origin 설정

    * Origin Domain: `s3-yourbucketname.s3.ap-northeast-2.amazonaws.com`
    * Origin Path: 비워둠
    * Origin Access: CloudFront가 S3 버킷 접근 가능하도록 허용

3. 배포 설정

    * Distribution Name: `shoemoa-image-cdn`
    * Security: HTTPS only 권장
    * Cache Policy: `CachingOptimized` (S3 최적화)
    * Viewer Protocol Policy: `Redirect HTTP to HTTPS` 추천

4. 생성 후 CloudFront OAI가 S3 버킷 정책과 연동되었는지 확인

---

## 3. S3 업로드 코드 예시 (Spring Boot)

```java
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

    s3Client.putObject(
        request,
        RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
```

* 파일명은 UUID 기반으로 생성하고 `products/{productId}/images/{uuid}.{ext}` 구조 추천
* 이렇게 하면 CloudFront + 브라우저에서 TTL 동기화, 캐시 Hit 최적화

---

## 4. CloudFront 캐시 확인

1. curl 명령어로 확인

```bash
curl -I https://<CLOUDFRONT_DOMAIN>/products/1/images/xxxx.png
```

2. 확인할 헤더

    * `x-cache: Hit from cloudfront` → 캐시 Hit
    * `age` → edge node에서 캐시된 시간
    * `cache-control` → TTL 확인

---

## 5. 캐시 정책/메타데이터 관리

* S3 객체 메타데이터

    * Content-Type: 필수
    * Cache-Control: CloudFront + 브라우저 TTL 직접 제어 시 사용
* CloudFront Cache Policy: CachingOptimized 사용 권장
* 이미지 교체 시 UUID 기반 파일명 → 캐시 무효화 필요 없음

---

## 6. 주의 사항

1. CloudFront 배포 후 edge 전파 시간 최소 5~10분 필요
2. Free tier 기준

    * S3: 5GB 스토리지, 20,000 GET, 2,000 PUT 요청 무료
    * CloudFront: 50GB 데이터 전송/월 무료
3. 퍼블릭 접근 차단, OAI 정책 적용 필수 (보안)
4. TTL 장기 설정 시, 이미지 변경 시 반드시 URL 변경 필요

---

문서 끝.
