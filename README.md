# Shoemoa Backend

신발 쇼핑몰 사이드 프로젝트 **Shoemoa**의 백엔드 API 레포지토리입니다.  
Spring Boot를 기반으로 RESTful API를 제공하며, 프론트엔드와 연동됩니다.

**💡 이 프로젝트는 JPA 학습용으로도 활용됩니다.**

---

## 📖 API Docs

**Swagger API**  
👉 (서버 실행 후) http://localhost:8080/swagger-ui/index.html

---

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.5.9, Spring Data JPA
- **Language**: Java 17
- **Database**: PostgreSQL, H2 (for testing)
- **Query**: QueryDSL
- **Storage**: AWS S3 (for Images)
- **CDN**: AWS CloudFront
- **Email**: JavaMailSender (Google SMTP)
- **Build**: Gradle
- **Code Style**: Spotless (Palantir Java Format)
- **API Docs**: SpringDoc (Swagger)

---

## 📦 Project Overview

Shoemoa 백엔드 시스템은 도메인 주도 설계(DDD) 원칙에 따라 개발되었습니다. 비즈니스 로직은 **Entity**에 집중시키고, **Service**는 트랜잭션과 데이터 흐름을 관리하는 역할만 수행하여 응집도 높고 유지보수하기 쉬운 구조를 지향합니다.

### 아키텍처 원칙
- **로직은 Entity에, 흐름은 Service에, 안전함은 Test로.**
- 모든 비즈니스 규칙과 상태 변경은 Entity 내부 메서드를 통해 이루어집니다.
- Service는 여러 Repository를 자유롭게 사용하여 필요한 데이터를 조회하고, Entity의 메서드를 호출하여 비즈니스 흐름을 조합합니다.
- Controller는 HTTP 요청을 받아 DTO를 변환하고 Service를 호출하는 역할만 담당합니다.

### 도메인 구조
- **`product`**: 상품, 이미지, 옵션 등 상품 관련 도메인
- **`order`**: 주문, 주문 상품 등 주문 관련 도메인
- **`member`**: 회원 관련 도메인
- **`common`**: 공통 모듈 (이미지 업로더, 메일 발송 등)

---

## 🗺️ Development Roadmap

이 프로젝트는 v3 아키텍처를 기반으로 다음 단계에 따라 기능을 확장해 나가는 것을 목표로 합니다.

### Phase 1: 핵심 도메인 구현 (Product & Member)
- **[v] 상품(Product) 도메인**: 상품 등록, 조회, 수정, 삭제 API 구현
- **[v] 회원(Member) 도메인**:
    -   `[v]` 회원가입(이메일 인증 포함), 로그인 API 구현
    -   `[v]` Spring Security와 JWT를 활용한 인증/인가 구현
    -   `[v]` 비동기 이메일 발송 처리 (Event Driven)

### Phase 2: 주문 및 결제 기능 (Order & Payment)
- **[ ] 장바구니(Cart) 기능**:
    -   `[ ]` 장바구니에 상품 추가, 조회, 삭제 기능
- **[ ] 주문(Order) 도메인**:
    -   `[ ]` 상품 주문 API 구현
    -   `[ ]` 주문 내역 조회, 주문 취소 API 구현
- **[ ] 결제(Payment) 연동**:
    -   `[ ]` PG사(예: 토스페이먼츠) 연동을 통한 결제 요청 및 처리

### Phase 3: 고급 기능 및 최적화
- **[ ] 검색 기능 고도화**:
    -   `[ ]` QueryDSL을 활용한 동적 검색 조건 및 정렬 기능 확장
- **[ ] 성능 최적화**:
    -   `[ ]` Redis를 이용한 쿼리 결과 캐싱 (예: 상품 상세 정보)
- **[ ] 동시성 제어**:
    -   `[ ]` 재고 차감 로직에 대한 동시성 테스트 및 Lock(Pessimistic/Optimistic) 적용 검토

---

## ✨ Core Features

### 사용자 기능
- 상품 목록 조회 (검색, 필터링, 페이징)
- 상품 상세 정보 조회
- 회원가입 (이메일 인증) 및 로그인

### 관리자 기능
- 상품 등록 (이미지 포함)
- 상품 정보 수정
- 상품 옵션 추가/수정/삭제
- 상품 삭제

> 프론트엔드 UI에 맞춰 회원, 주문, 장바구니 등 핵심 도메인 기능이 단계적으로 연동될 예정입니다.

---

## 🚀 Getting Started

### Prerequisites
- Java 17
- (Optional) Docker

### Build
```bash
./gradlew build
```

### Run Application
실행 전, 프로젝트 루트에 `.env` 파일을 생성하여 필요한 환경 변수(DB, AWS, Email 등)를 설정해야 합니다.

**1. Using Gradle (local profile)**  
H2 인메모리 DB를 사용하여 로컬에서 빠르게 실행합니다.
```bash
./gradlew bootRun
```

**2. Using Docker**  
`docker-compose.yml` (생성 필요) 또는 `docker-run.sh` 스크립트를 통해 PostgreSQL과 함께 실행할 수 있습니다.
```bash
# 1. Docker 이미지 빌드
./docker/docker-build.sh

# 2. Docker 컨테이너 실행
./docker/docker-run.sh
```

---

## 🧪 Testing

프로젝트는 두 가지 유형의 테스트를 중심으로 안정성을 확보합니다.

1.  **Domain Test**: 순수 Java 코드로 Entity의 핵심 비즈니스 로직을 검증합니다. (Spring, DB 의존성 없음)
2.  **Integration Test**: `@SpringBootTest`를 사용하여 Service부터 Repository, 실제 DB(H2)까지 이어지는 전체 흐름을 통합 검증합니다. 모든 테스트는 `@Transactional`을 통해 격리됩니다.

### Run Tests
```bash
./gradlew test
```

---

## 💡 Additional Information

### Configuration Profiles
이 프로젝트는 Spring Profile을 통해 환경별 설정을 분리합니다.
- **`local`**: 개발 환경용 프로필. H2 인메모리 DB를 사용하고, S3 대신 로컬에 파일을 업로드하는 `FakeImageUploader`가 활성화될 수 있습니다. (`./gradlew bootRun`의 기본값)
- **`prod`**: 운영 환경용 프로필. PostgreSQL DB와 실제 AWS S3 연동을 사용합니다. Docker로 실행 시 이 프로필을 사용하는 것이 권장됩니다.

### Error Handling
API에서 오류 발생 시, 다음과 같은 일관된 형식의 JSON 응답을 반환합니다.
```json
{
  "code": "ERROR_CODE",
  "message": "에러에 대한 상세 메시지"
}
```
- **`code`**: 에러 유형을 식별하기 위한 고유 코드 (예: `BAD_REQUEST`, `ENTITY_NOT_FOUND`)
- **`message`**: 개발자 또는 사용자가 문제를 파악하는 데 도움이 되는 설명 메시지
