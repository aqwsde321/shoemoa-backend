# FEATURES.md

## 목적

이 문서는 **실제 개발 단위(티켓, PR, 테스트 케이스)로 바로 분해 가능한 기능 목록**을 정의한다.

- 기획 문서 ❌
- API 명세서 ❌
- **개발 실행 문서 ✅**

---

## 기준 도메인: Order

본 FEATURES는 `Order` Aggregate를 기준으로 작성한다.
다른 도메인은 동일한 패턴으로 확장한다.

---

## Feature Group 1. 주문 생성 (Order Creation)

### F-ORD-001 주문 생성

- 설명

  - 사용자는 상품을 선택하여 주문을 생성할 수 있다.
  - 주문 생성 시 결제는 발생하지 않는다.

- 입력

  - 상품 ID 목록
  - 수량
  - 주문자 ID

- 처리 규칙

  - 상품은 반드시 판매 가능 상태여야 한다
  - 수량은 1 이상
  - 주문 생성 시 상태는 `CREATED`

- 도메인

  - Aggregate: `Order`
  - Entity: `OrderItem`
  - Domain Service: 없음

- Application

  - UseCase: `CreateOrderUseCase`

- API

  - POST /api/orders

- 테스트

  - Domain Test: 주문 생성 규칙 검증
  - UseCase Test: 정상 생성
  - Controller Test: 201 응답

---

## Feature Group 2. 주문 조회 (Order Query)

### F-ORD-010 주문 단건 조회

- 설명

  - 주문 ID로 주문 상세를 조회한다

- 입력

  - 주문 ID

- 처리 규칙

  - 존재하지 않는 주문이면 404

- Application

  - Query Service: `OrderQueryService`

- API

  - GET /api/orders/{orderId}

- 테스트

  - Controller 통합 테스트

---

### F-ORD-011 주문 목록 조회

- 설명

  - 사용자 기준 주문 목록 조회

- 입력

  - 사용자 ID
  - 페이지 정보

- API

  - GET /api/orders?memberId={id}

- 테스트

  - Controller 통합 테스트

---

## Feature Group 3. 주문 상태 변경

### F-ORD-020 주문 취소

- 설명

  - 생성된 주문을 취소한다

- 처리 규칙

  - 상태가 `CREATED` 인 주문만 취소 가능
  - 취소 후 상태는 `CANCELLED`

- Domain

  - Order.cancel()

- Application

  - UseCase: `CancelOrderUseCase`

- API

  - POST /api/orders/{orderId}/cancel

- 테스트

  - Domain Test: 상태 전이 규칙
  - UseCase Test
  - Controller Test

---

## Feature Group 4. 결제 연동 (후속)

### F-ORD-030 결제 요청

- 설명

  - 주문에 대해 결제를 요청한다

- 처리 규칙

  - 주문 상태가 `CREATED` 여야 함

- 비고

  - 외부 PG 연동
  - 현재 스프린트 제외

---

## 공통 규칙

- Feature ID는 고정 식별자이며 변경하지 않는다
- Feature 단위 = Jira Ticket 1개
- 하나의 Feature는 반드시 테스트를 포함한다

---

## Feature → 구현 매핑 기준

| Feature   | Domain | Application        | Controller      | Test                |
| --------- | ------ | ------------------ | --------------- | ------------------- |
| F-ORD-001 | Order  | CreateOrderUseCase | OrderController | Domain + Controller |
| F-ORD-020 | Order  | CancelOrderUseCase | OrderController | Full                |

---

## 미작성 영역

- 배송
- 쿠폰
- 결제 실패 처리

(확장 시 Feature Group 추가)
