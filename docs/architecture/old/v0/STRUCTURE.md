# STRUCTURE.md

프로젝트 구조 및 레이어 책임 정의

---

## 1. 패키지 구조 개요

본 프로젝트는 **도메인 중심 + 레이어 분리 구조**를 따른다.  
JPA Entity를 Domain으로 사용하되, DDD 규율을 강제한다.

com.shop
├─ domain
│ └─ order
│ ├─ Order.java // Aggregate Root (JPA Entity)
│ ├─ OrderItem.java
│ ├─ OrderStatus.java
│ └─ OrderRepository.java // JpaRepository + Domain Repository
│
├─ application
│ └─ order
│ ├─ OrderService.java // UseCase / Orchestration
│ └─ port
│ ├─ MemberValidator.java
│ └─ StockManager.java
│
├─ infrastructure
│ ├─ member
│ │ └─ JpaMemberValidator.java
│ ├─ stock
│ │ └─ JpaStockManager.java
│ └─ order
│ ├─ OrderDslRepository.java
│ └─ OrderDslRepositoryImpl.java
│
├─ presentation
│ └─ order
│ ├─ OrderController.java
│ ├─ request
│ │ └─ OrderCreateRequest.java
│ └─ response
│ └─ OrderCreateResponse.java
│
└─ global
├─ config
└─ exception

yaml
코드 복사

---

## 2. 레이어별 책임 정의

### 2.1 Presentation (Controller)

- HTTP 요청/응답 처리
- Request DTO → Application Service 호출
- Response DTO 반환
- 비즈니스 로직 금지

---

### 2.2 Application Service (UseCase)

- 유즈케이스 단위 흐름 제어
- 여러 도메인/포트 조합
- 트랜잭션 경계 설정
- 외부 정책/상태 검증 수행

**금지 사항**

- 비즈니스 규칙 구현 ❌
- 엔티티 상태 직접 조작 ❌
- JPA 쿼리 작성 ❌

---

### 2.3 Domain (JPA Entity = Domain Model)

- 비즈니스 로직의 중심
- 상태 + 행위 응집
- Aggregate Root 기준 설계

**허용**

- 상태 변경 로직
- 도메인 규칙
- 유효성 검증

**금지**

- 다른 Aggregate Entity 직접 참조 ❌
- Repository 호출 ❌
- 외부 시스템 의존 ❌

---

### 2.4 Repository (Spring Data JPA)

- 저장 / 조회 책임만 수행
- 비즈니스 로직 포함 금지

```java
public interface OrderRepository
        extends JpaRepository<Order, Long>, OrderDslRepository {
}
3. JPA Entity 사용 방침
JPA Entity를 Domain Entity로 사용한다

단, 다음 규칙을 반드시 지킨다

3.1 필수 규칙
비즈니스 로직 포함

Setter 사용 금지 (또는 최소화)

생성자 / 정적 팩토리로만 생성

무효 상태 생성 불가

4. 도메인 간 의존성 규칙
4.1 도메인 직접 참조 금지
다른 Aggregate의 Entity를 직접 참조하지 않는다.

Order → Member ❌

Order → Product ❌

4.2 허용되는 협력 방식
ID 기반 참조

Application Port(interface) 사용

java
코드 복사
Order order = Order.create(memberId, productId, quantity);
5. 도메인 메서드 인자 규칙
다른 Aggregate Entity 전달 ❌

ID / Value Object / enum만 허용

java
코드 복사
Order.create(Long memberId, Long productId, int quantity);
6. Port(interface) 사용 규칙
6.1 Port의 역할
다른 도메인 상태 검증

외부 정책 / 시스템 추상화

java
코드 복사
public interface MemberValidator {
    boolean isActive(Long memberId);
}
6.2 Port 구현체 위치
Port 인터페이스 → application

구현체 → infrastructure

Service는 Port 인터페이스에만 의존

java
코드 복사
@Component
public class JpaMemberValidator implements MemberValidator {
}
7. DSL(QueryDSL) Repository 구조
7.1 DSL 인터페이스 위치
infrastructure 레이어

JPA/QueryDSL에 종속되므로 Domain에 두지 않는다

java
코드 복사
public interface OrderDslRepository {
    List<Order> findPaidOrders(LocalDate from, LocalDate to);
}
7.2 DSL 구현체 위치
DSL 인터페이스와 동일한 infrastructure 패키지

Spring Data 규칙에 따라 Impl 접미사 사용

java
코드 복사
public class OrderDslRepositoryImpl implements OrderDslRepository {
}
7.3 Repository 연결 방식
java
코드 복사
public interface OrderRepository
        extends JpaRepository<Order, Long>, OrderDslRepository {
}
Application / Domain은 DSL 존재를 모른다

조회 최적화는 전부 Infrastructure 책임

8. 테스트 관점 구조 정합성
8.1 Domain Test
순수 자바 테스트

Spring / JPA / Mock ❌

8.2 Application Test
UseCase 단위 테스트

Fake Port 구현체 사용

Spring 없이 가능

9. 구조 한 줄 요약
이 프로젝트는
JPA 엔티티를 도메인으로 사용하되,
비즈니스 로직은 엔티티에,
유즈케이스 흐름은 Application Service에,
인프라 기술(QueryDSL, 외부 시스템)은 infrastructure에 격리하는 구조를 따른다.

markdown
코드 복사

---

## 정리

- ✅ 도메인 = JPA 엔티티 (하지만 규율 강제)
- ✅ Service는 오케스트레이션만
- ✅ Port는 application, 구현체는 infrastructure
- ✅ QueryDSL은 전부 infrastructure
- ❌ 도메인 간 엔티티 직접 참조 금지
```
