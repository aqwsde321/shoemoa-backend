# 프로젝트 컨벤션

코드 작성 시 따라야 할 명명 규칙과 컨벤션

---

## 📦 패키지 구조

```
com.shop
├─ domain
│  ├─ order
│  │  ├─ Order.java
│  │  ├─ OrderStatus.java
│  │  └─ OrderRepository.java
│  ├─ member
│  │  ├─ Member.java
│  │  └─ MemberRepository.java
│  └─ product
│     ├─ Product.java
│     └─ ProductRepository.java
│
├─ application
│  ├─ order
│  │  └─ OrderService.java
│  ├─ member
│  │  └─ MemberService.java
│  └─ product
│     └─ ProductService.java
│
├─ infrastructure
│  └─ order
│     ├─ OrderDslRepository.java
│     └─ OrderDslRepositoryImpl.java
│
└─ presentation
   ├─ order
   │  ├─ OrderController.java
   │  ├─ request
   │  │  ├─ OrderCreateRequest.java
   │  │  └─ OrderCancelRequest.java
   │  └─ response
   │     └─ OrderResponse.java
   ├─ member
   │  ├─ MemberController.java
   │  ├─ request
   │  │  └─ MemberJoinRequest.java
   │  └─ response
   │     └─ MemberResponse.java
   └─ common
      └─ ErrorResponse.java
```

---

## 🏷️ 네이밍 규칙

### Entity

```java
// 단수형, PascalCase
Order
Member
Product
Payment

// ❌ 금지
Orders          // 복수형
order           // 소문자
OrderEntity     // Entity 접미사
```

### Service

```java
// Entity명 + Service
OrderService
MemberService
ProductService

// ❌ 금지
OrderServiceImpl        // Impl 접미사
OrderApplicationService // 중복
```

### Repository

```java
// Entity명 + Repository
OrderRepository
MemberRepository

// QueryDSL용
OrderDslRepository       // 인터페이스
OrderDslRepositoryImpl   // 구현체 (Impl 허용)
```

### Controller

```java
// Entity명 + Controller
OrderController
MemberController
ProductController
```

### DTO

```java
// Request: Entity명 + 행위 + Request
OrderCreateRequest
OrderUpdateRequest
MemberJoinRequest

// Response: Entity명 + Response
OrderResponse
MemberResponse
OrderListResponse      // 리스트용

// ❌ 금지
CreateOrderRequest     // 행위가 앞에
OrderDTO              // DTO 접미사
OrderReq              // 축약
```

---

## 🌐 API 엔드포인트

### RESTful 규칙

```
- 리소스는 복수형
- 행위는 HTTP Method로 표현
- 추가 행위는 동사 사용
```

### 기본 CRUD

```
POST   /api/orders          # 주문 생성
GET    /api/orders/{id}     # 주문 조회
PUT    /api/orders/{id}     # 주문 수정
DELETE /api/orders/{id}     # 주문 삭제
GET    /api/orders          # 주문 목록
```

### 추가 행위

```
POST /api/orders/{id}/cancel    # 취소
POST /api/orders/{id}/confirm   # 확정
POST /api/orders/{id}/complete  # 완료

POST /api/members/{id}/deactivate  # 비활성화
POST /api/members/{id}/activate    # 활성화
```

### 검색/필터

```
GET /api/orders?memberId=1           # 회원별 조회
GET /api/orders?status=CREATED       # 상태별 조회
GET /api/orders?memberId=1&status=CREATED  # 복합 조건

GET /api/products?name=노트북         # 검색
GET /api/products?minPrice=1000      # 범위 조건
```

### 페이징

```
GET /api/orders?page=0&size=20       # 첫 페이지, 20개
GET /api/orders?page=1&size=10       # 두 번째 페이지, 10개
```

---

## 🗄️ DB 테이블명

### 규칙

```
- 복수형
- snake_case
- 소문자
```

### 예시

```sql
-- Entity → Table
Order    → orders
Member   → members
Product  → products
OrderItem → order_items

-- ❌ 금지
Order    → order          -- 단수형
Order    → Orders         -- 대문자
OrderItem → orderItem     -- camelCase
```

---

## 📋 컬럼명

### 규칙

```
- snake_case
- 소문자
- ID는 _id 접미사
```

### 예시

```sql
-- Java → DB
memberId     → member_id
productId    → product_id
totalAmount  → total_amount
createdAt    → created_at

-- ❌ 금지
memberId     → memberId      -- camelCase
member_id    → MEMBER_ID     -- 대문자
```

---

## 🎯 도메인 목록

### 핵심 도메인

| Entity | 설명 | 테이블명 |
|--------|------|----------|
| Member | 회원 | members |
| Product | 상품 | products |
| Order | 주문 | orders |
| OrderItem | 주문 상품 | order_items |
| Payment | 결제 | payments |

### 추가 도메인 (확장 시)

| Entity | 설명 | 테이블명 |
|--------|------|----------|
| Cart | 장바구니 | carts |
| CartItem | 장바구니 상품 | cart_items |
| Review | 리뷰 | reviews |
| Coupon | 쿠폰 | coupons |
| Category | 카테고리 | categories |

---

## 🔤 변수명

### Java 변수

```java
// camelCase
String userName;
int totalAmount;
LocalDateTime createdAt;

// 상수는 UPPER_SNAKE_CASE
public static final int MAX_QUANTITY = 100;
public static final String DEFAULT_STATUS = "CREATED";
```

### Boolean

```java
// is 접두사
boolean isActive;
boolean isDeleted;
boolean isPaid;

// has 접두사
boolean hasStock;
boolean hasPermission;
```

---

## 📝 메서드명

### Entity 비즈니스 메서드

```java
// 동사 + 목적어
cancel()              // 취소
confirm()             // 확정
decreaseStock()       // 재고 차감
increaseStock()       // 재고 증가
validateActive()      // 활성화 검증
calculateTotalAmount() // 총액 계산

// ❌ 금지
doCancle()           // do 접두사
cancelOrder()        // 중복 (Order 안에 있으므로)
```

### Service 메서드

```java
// 동사 + Entity명
createOrder()
cancelOrder()
getOrder()
updateOrder()

// 조회는 get/find
getOrder(Long id)              // 단건
getOrders(Long memberId)       // 다건
findOrdersByStatus(OrderStatus status)
```

### Repository 메서드

```java
// Spring Data JPA 규칙
findById()
findByEmail()
findByMemberId()
existsByEmail()
deleteById()
```

---

## 🎨 Enum 네이밍

### 규칙

```java
// PascalCase, 값은 UPPER_SNAKE_CASE

public enum OrderStatus {
    CREATED,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}

public enum PaymentMethod {
    CREDIT_CARD,
    BANK_TRANSFER,
    KAKAO_PAY,
    TOSS_PAY
}

// ❌ 금지
public enum OrderStatus {
    Created,      // PascalCase 값
    confirmed,    // 소문자
    CANCEL_ED     // 불필요한 언더스코어
}
```

---

## 📦 DTO 구조

### Request

```java
// record 사용
public record OrderCreateRequest(
    @NotNull Long memberId,
    @NotNull Long productId,
    @Min(1) Integer quantity
) {}
```

### Response

```java
// record + static factory
public record OrderResponse(
    Long orderId,
    Long memberId,
    int totalAmount,
    OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getMemberId(),
            order.getTotalAmount(),
            order.getStatus()
        );
    }
}
```

---

## 🔢 매직 넘버 금지

```java
// ❌ 금지
if (quantity > 100) {
    throw new IllegalArgumentException();
}

// ✅ 허용
private static final int MAX_ORDER_QUANTITY = 100;

if (quantity > MAX_ORDER_QUANTITY) {
    throw new IllegalArgumentException(
        "최대 주문 수량은 " + MAX_ORDER_QUANTITY + "개입니다"
    );
}
```

---

## 📅 날짜/시간

### 규칙

```java
// LocalDateTime 사용 (java.time 패키지)
private LocalDateTime createdAt;
private LocalDateTime updatedAt;

// ❌ 금지
private Date createdAt;         // java.util.Date
private Timestamp createdAt;    // java.sql.Timestamp
```

### 컬럼명

```java
createdAt  → created_at
updatedAt  → updated_at
deletedAt  → deleted_at
```

---

## 🎯 예외 메시지

### 규칙

```java
// 명확하고 구체적으로
throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
throw new IllegalStateException("취소할 수 없는 상태입니다: " + status);
throw new EntityNotFoundException("회원을 찾을 수 없습니다. ID: " + memberId);

// ❌ 금지
throw new Exception("에러");
throw new RuntimeException("실패");
throw new IllegalArgumentException("invalid");
```

---

## 📏 코드 포맷

### 들여쓰기

```
- 스페이스 4칸
- 탭 사용 금지
```

### 중괄호

```java
// ✅ 허용
if (condition) {
    // ...
}

// ❌ 금지
if (condition)
{
    // ...
}

if (condition) { /* ... */ }  // 한 줄 금지
```

### import

```java
// 알파벳 순서
// java → javax → org → com 순서
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.domain.order.Order;
```

---

## 💬 주석

### 규칙

```java
// 코드로 설명 가능하면 주석 불필요
// 의도가 불명확할 때만 작성

// ✅ 좋은 주석
// TODO: 재고 부족 시 알림 기능 추가 필요
// FIXME: 동시성 이슈 있음 - Lock 적용 예정

// ❌ 나쁜 주석
// 주문을 생성한다
public void createOrder() { }
```

---

## 📋 체크리스트

### 코드 작성 전

```
[ ] Entity명 확인 (단수형, PascalCase)
[ ] 패키지 위치 확인 (domain/application/presentation)
[ ] API 엔드포인트 확인 (복수형, RESTful)
[ ] 테이블명 확인 (복수형, snake_case)
```

### 코드 작성 후

```
[ ] Setter 사용 안 함
[ ] 매직 넘버 없음
[ ] 예외 메시지 명확함
[ ] 네이밍 규칙 준수
[ ] import 정리됨
```

---

## 한 줄 요약

**"Entity는 단수형, 테이블은 복수형, API는 RESTful."**