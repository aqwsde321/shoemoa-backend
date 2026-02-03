# S3 이미지 업로드 실패

## 문제 상황

상품 등록/수정 API 호출 시, 이미지를 첨부하면 HTTP 500 Internal Server Error가 발생합니다.

서버 애플리케이션 로그를 확인하면 아래와 같은 `Access Denied` 에러 메시지가 출력됩니다.

```log
com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied (Service: Amazon S3; Status Code: 403; Error Code: AccessDenied; Request ID: [...]), S3 Extended Request ID: [...]
```

## 원인

이 문제는 S3 버킷에 파일을 업로드할 권한이 없는 경우에 발생합니다. 주요 원인은 다음과 같습니다.

1.  **AWS 자격 증명(Credentials) 오류**: 애플리케이션이 사용하는 AWS Access Key ID 또는 Secret Access Key가 잘못되었습니다.
2.  **S3 버킷 이름 오류**: `application-prod.yml`에 설정된 버킷 이름이 실제 AWS S3의 버킷 이름과 다릅니다.
3.  **IAM 권한 부족**: 애플리케이션에 연결된 IAM 사용자가 대상 S3 버킷에 파일을 업로드할 `s3:PutObject` 권한을 가지고 있지 않습니다.

## 해결 방법

1.  **AWS 자격 증명 확인**:
    - 로컬 개발 환경의 경우, `~/.aws/credentials` 파일에 설정된 `aws_access_key_id`와 `aws_secret_access_key`가 올바른지 확인합니다.
    - EC2/ECS 등 배포 환경의 경우, 해당 환경에 할당된 IAM Role의 정책(Policy)이 올바른지 확인합니다.

2.  **S3 버킷 이름 확인**:
    - `src/main/resources/application-prod.yml` 파일을 열어 `cloud.aws.s3.bucket` 속성 값이 AWS에 생성된 실제 S3 버킷의 이름과 정확히 일치하는지 확인합니다.

    ```yaml
    cloud:
      aws:
        s3:
          bucket: "shoemoa-bucket" # <-- 이 부분을 확인
    ```

3.  **IAM 정책 확인**:
    - AWS Management Console에 로그인하여 IAM 서비스로 이동합니다.
    - 애플리케이션이 사용하는 IAM 사용자 또는 Role을 선택합니다.
    - '권한(Permissions)' 탭에서 연결된 정책에 `s3:PutObject` 작업이 허용되어 있는지 확인합니다. 아래는 정책 예시입니다.

    ```json
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "AllowS3PutObject",
                "Effect": "Allow",
                "Action": "s3:PutObject",
                "Resource": "arn:aws:s3:::shoemoa-bucket/*"
            }
        ]
    }
    ```
    - 위 정책에서 `shoemoa-bucket` 부분은 실제 사용하는 버킷 이름으로 변경해야 합니다.
