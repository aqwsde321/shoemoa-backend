# GLOSSARY.md

본 문서는 본 프로젝트에서 사용하는 **핵심 용어의 의미를 단일 기준으로 고정**하기 위한 문서이다.

- 구현체, 프레임워크 용어보다 **설계 의도 기준**으로 정의한다.
- 유사 용어(Service, UseCase, Domain 등)의 혼용을 방지한다.
- 코드 리뷰, 문서, 커뮤니케이션에서 **본 문서의 정의를 기준으로 한다**.

---

## 1. Domain / 도메인

### 정의

비즈니스 규칙과 상태를 스스로 책임지는 모델 계층.

### 본 프로젝트 기준

- **JPA Entity = Domain Entity**
- 단, DDD 규율을 강제한다.

### 포함 요소

- 비즈니스 로직
- 상태 변경 규칙
- 불변 조건 검증

### 포함하지 않는 것

- Repository 호출
- 외부 시스템 접근
- 트랜잭션 제어

---

## 2. Domain Entity / 도메인 엔티티

### 정의

식별자(ID)를 가지며 생명주기를 관리하는 도메인 객체.

### 본 프로젝트 기준

- `@Entity`로 선언된 JPA 엔티티
- 도메인 로직을 포함함

### 규칙

- Setter 금지
- 의미 없는 기본 생성자 외 사용 금지
- 생성자 / 정적 팩토리로만 유효 상태 생성

---

## 3. Aggregate / 애그리거트

### 정의

하나의 일관성 경계를 가지는 도메인 묶음.

### 본 프로젝트 기준

- Aggregate Root는 JPA Entity
- Aggregate 간 직접 참조 금지

### 허용되는 협력 방식

- ID 기반 참조
- Application Port(interface) 사용

---

## 4. Aggregate Root / 애그리거트 루트

### 정의

Aggregate 외부에서 접근 가능한 유일한 진입점.

### 본 프로젝트 기준

- Repository는 Aggregate Root만 다룬다
- 외부에서 내부 엔티티 직접 접근 금지

---

## 5. Application Service / Application Layer

### 정의

유스케이스를 실행하는 오케스트레이션 계층.

### 본 프로젝트 기준 명칭

- `*Service`
- `UseCase`라는 이름은 사용하지 않음

### 책임

- 도메인 객체 조합
- 트랜잭션 경계 설정
- Port 호출

### 비책임

- 비즈니스 규칙 구현
- 상태 판단 로직

---

## 6. Service (금지 용어)

### 설명

모호한 의미로 사용되기 쉬운 용어.

### 본 프로젝트 방침

- 단독으로 "Service"라는 용어 사용 금지
- 반드시 아래 중 하나로 명확히 구분

| 용어                | 의미             |
| ------------------- | ---------------- |
| Application Service | 유스케이스 실행  |
| Domain Method       | 엔티티 내부 로직 |

---

## 7. UseCase / 유스케이스

### 정의

사용자의 의도를 시스템이 처리하는 단위 작업.

### 본 프로젝트 기준

- **개념적으로만 사용**
- 실제 구현은 Application Service 메서드로 표현

---

## 8. Repository / 레포지토리

### 정의

Aggregate Root의 영속성을 책임지는 객체.

### 본 프로젝트 기준

- Domain 패키지 하위에 위치
- Spring Data JPA Repository 사용 허용

### 책임

- 조회
- 저장

### 비책임

- 비즈니스 판단
- 도메인 규칙 처리

---

## 9. Port / 포트

### 정의

Application Layer에서 외부 의존성을 추상화한 인터페이스.

### 예시

- MemberValidator
- StockManager

### 목적

- Aggregate 간 직접 Repository 호출 차단
- Service 단위 테스트 용이성 확보

---

## 10. Adapter / 어댑터

### 정의

Port의 실제 구현체.

### 위치

- `infrastructure` 패키지

### 예시

- JpaMemberAdapter
- ExternalStockAdapter

---

## 11. Controller / 컨트롤러

### 정의

HTTP 요청을 Application Service로 전달하는 진입점.

### 책임

- Request → DTO 변환
- Validation
- Response 매핑

### 비책임

- 비즈니스 로직
- 트랜잭션 처리

---

## 12. DTO

### 정의

계층 간 데이터 전달 객체.

### 규칙

- Domain 객체 직접 노출 금지
- Controller ↔ Application 사이에서만 사용

---

## 13. Infrastructure / 인프라스트럭처

### 정의

기술 상세 구현 계층.

### 포함 요소

- JPA 구현체
- QueryDSL
- 외부 API 연동

---

## 14. Testing 용어

### Domain Test

- 순수 자바 테스트
- JPA, Spring 미사용

### Application Test

- Port Mock 또는 Fake 사용

### Controller Test

- HTTP 기준 통합 테스트

---

## 15. 용어 사용 강제 규칙

- 코드, 문서, 리뷰에서 본 문서 기준 사용
- 새로운 용어 도입 시 GLOSSARY에 먼저 추가

---

## 한 줄 요약

> 이 문서는 "우리가 같은 단어를 같은 의미로 쓰고 있는가"를 강제하기 위한 기준 문서다.
