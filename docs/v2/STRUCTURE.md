# STRUCTURE.md

## 코드 작성 가이드

**실제 코드 작성 시 참고하는 실무 가이드**

> 본 문서를 읽기 전, [CORE.md](CORE.md)에서 설계 원칙을, [GLOSSARY.md](GLOSSARY.md)에서 주요 용어를 먼저 확인하는 것을 권장합니다.

---

## 1. 패키지 구조

```
com.shop
├─ domain
│  ├─ order (Order, OrderRepository)
│  ├─ member (Member, MemberRepository)
│  └─ product (Product, ProductRepository)
│
├─ application
│  └─ order
│     ├─ OrderService.java
│     └─ port (MemberValidator, StockManager 등)
│
├─ infrastructure
│  ├─ member (JpaMemberValidator)
│  ├─ product (JpaStockManager)
│  └─ order (QueryDSL 구현체)
│
└─ presentation
   └─ order (OrderController, DTO)
```

---

## 2. 레이어별 책임

| 레이어 | 책임 | 허용 | 금지 |
|---|---|---|---|
| **[Domain](GLOSSARY.md#domain-도메인)** | 비즈니스 규칙 | 상태 변경, 검증 | 다른 Aggregate 참조, Repository 호출 |
| **[Application](GLOSSARY.md#application-service-애플리케이션-서비스)** | 유즈케이스 흐름 | Port 호출, 트랜잭션 | if/else 분기, Service 주입 |
| **[Infrastructure](GLOSSARY.md#adapter-어댑터)** | 기술 상세 | Port 구현, DB 접근 | 비즈니스 로직 |
| **[Presentation](GLOSSARY.md#layered-architecture-계층형-아키텍처)** | HTTP 처리 | DTO 변환 | Entity 반환, 비즈니스 로직 |

---

## 3. [Entity](GLOSSARY.md#entity-엔티티) 작성 규칙

> 전체 [Entity 예시 코드는 Sample_code.md](Sample_code.md#11-order-entity-aggregate-root)에서 확인할 수 있습니다.

### 3.1 기본 템플릿

```java
@Entity
public class Order {
    
    @Id @GeneratedValue
    private Long id;
    
    private Long memberId;  // ID 참조 (Entity 참조 ❌)
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    // JPA용 기본 생성자
    protected Order() {}
    
    // 정적 팩토리
    public static Order create(Long memberId, Long productId, int quantity) {
        return new Order(memberId, productId, quantity);
    }
    
    // private 생성자 + 검증
    private Order(Long memberId, Long productId, int quantity) {
        validateQuantity(quantity);
        this.memberId = memberId;
        this.status = OrderStatus.CREATED;
    }
    
    // 비즈니스 메서드
    public void cancel() {
        if (!this.status.isCancelable()) {
            throw new IllegalStateException("취소 불가");
        }
        this.status = OrderStatus.CANCELLED;
    }
    
    // Getter만 (Setter 없음)
    public OrderStatus getStatus() { return status; }
}
```

### 3.2 핵심 규칙

**✅ 해야 할 것**
- Setter 대신 의미 있는 메서드 (`cancel()`, `confirm()`)
- 생성자에서 검증
- 비즈니스 로직 포함

**❌ 하지 말아야 할 것**
- 다른 Aggregate Entity 필드 참조
- public Setter
- Repository 의존

### 3.3 Aggregate 간 참조

```java
// ❌ 금지
@Entity
public class Order {
    @ManyToOne
    private Member member;  // 다른 Aggregate 참조 금지
}

// ✅ 허용
@Entity
public class Order {
    private Long memberId;  // ID 기반 참조
}
```

---

## 4. [Service](GLOSSARY.md#application-service-애플리케이션-서비스) 작성 규칙

> 전체 [Service 예시 코드는 Sample_code.md](Sample_code.md#22-orderservice-application-service)에서 확인할 수 있습니다.

### 4.1 기본 템플릿

```java
@Service
public class OrderService {
    
    // 자기 Aggregate Repository
    private final OrderRepository orderRepository;
    
    // Port 인터페이스
    private final MemberValidator memberValidator;
    private final StockManager stockManager;
    
    public OrderService(
        OrderRepository orderRepository,
        MemberValidator memberValidator,
        StockManager stockManager
    ) {
        this.orderRepository = orderRepository;
        this.memberValidator = memberValidator;
        this.stockManager = stockManager;
    }
    
    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        // 1. Port로 검증
        memberValidator.validateActive(memberId);
        
        // 2. Port로 상태 변경
        stockManager.decrease(productId, quantity);
        
        // 3. Domain 생성
        Order order = Order.create(memberId, productId, quantity);
        
        // 4. 저장
        return orderRepository.save(order).getId();
    }
}
```

### 4.2 핵심 규칙

**Service의 역할**
1. 유즈케이스 흐름 조합
2. 트랜잭션 경계 정의
3. Port/Repository 호출

**절대 금지**
```java
// ❌ 비즈니스 분기
if (order.getPrice() > 100_000) {
    order.setDiscount(10);
}

// ❌ 상태 직접 변경
order.setStatus(CANCELLED);

// ❌ 다른 Service 주입
private final ProductService productService;

// ❌ 다른 Aggregate Repository 주입
private final MemberRepository memberRepository;
```

---

## 5. [Port](GLOSSARY.md#port-포트) 설계

> 전체 [Port 예시 코드는 Sample_code.md](Sample_code.md#21-port-interfaces)에서 확인할 수 있습니다.

### 5.1 Port가 필요한 경우

다음 **3가지 경우에만** Port를 만든다:

1. **다른 Aggregate 검증**
2. **다른 Aggregate 상태 변경**
3. **외부 시스템 연동**

### 5.2 Port 종류와 네이밍

| 역할 | 네이밍 | 예시 |
|------|--------|------|
| 검증 (읽기 전용) | ~Validator | MemberValidator |
| 상태 변경 | ~Manager | StockManager |
| 조회 | ~Reader | ProductReader |
| 외부 시스템 | ~Gateway | PaymentGateway |

### 5.3 Port 설계 원칙

**✅ 좋은 Port**
- 도메인 의도 표현
- DTO/원시값 반환
- 단일 책임

```java
public interface MemberValidator {
    void validateActive(Long memberId);
}

public interface StockManager {
    void decrease(Long productId, int quantity);
}
```

**❌ 나쁜 Port**
- Entity 반환
- Repository Wrapper

```java
// ❌ 금지
public interface MemberPort {
    Member findById(Long id);  // Entity 반환 금지
}
```

### 5.4 Port 구현 예시

```java
// Application Layer (port 패키지)
public interface MemberValidator {
    void validateActive(Long memberId);
}

// Infrastructure Layer
@Component
public class JpaMemberValidator implements MemberValidator {
    
    private final MemberRepository memberRepository;
    
    @Override
    public void validateActive(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException());
        
        if (!member.isActive()) {
            throw new IllegalStateException("비활성 회원");
        }
    }
}
```

### 5.5 트랜잭션 처리

**Service에만 @Transactional**

```java
// Service: @Transactional O
@Service
public class OrderService {
    @Transactional  // ← 여기
    public Long createOrder(...) {
        stockManager.decrease(...);  // ← 전파됨
    }
}

// Port 구현: @Transactional X
@Component
public class JpaStockManager implements StockManager {
    // @Transactional 없음
    public void decrease(...) { }
}
```

---

## 6. [Repository](GLOSSARY.md#repository-리포지토리) 설계

> 전체 [Repository 예시 코드는 Sample_code.md](Sample_code.md#13-orderrepository)에서 확인할 수 있습니다.

### 6.1 기본 Repository

```java
// Domain Layer
package com.shop.domain.order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

### 6.2 QueryDSL Repository

```java
// Infrastructure Layer
public interface OrderDslRepository {
    List<Order> findPaidOrders(LocalDate from, LocalDate to);
}

public class OrderDslRepositoryImpl implements OrderDslRepository {
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<Order> findPaidOrders(LocalDate from, LocalDate to) {
        return queryFactory
            .selectFrom(order)
            .where(...)
            .fetch();
    }
}

// Domain Layer - 통합
public interface OrderRepository 
    extends JpaRepository<Order, Long>, OrderDslRepository {
}
```

---

## 7. DTO 작성

### 7.1 Request DTO

```java
public record OrderCreateRequest(
    Long memberId,
    Long productId,
    int quantity
) {
    public OrderCreateRequest {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId 필수");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity는 1 이상");
        }
    }
}
```

### 7.2 Response DTO

```java
public record OrderResponse(
    Long orderId,
    Long memberId,
    int quantity,
    OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getMemberId(),
            order.getQuantity(),
            order.getStatus()
        );
    }
}
```

---

## 8. Controller 작성

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
        @RequestBody OrderCreateRequest request
    ) {
        Long orderId = orderService.createOrder(
            request.memberId(),
            request.productId(),
            request.quantity()
        );
        
        return ResponseEntity.ok(new OrderCreateResponse(orderId));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
        @PathVariable Long orderId
    ) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

**핵심**: Entity를 절대 직접 반환하지 않음

---

## 9. [Value Object](GLOSSARY.md#value-object-값-객체) 사용 기준

다음 **2개 이상** 해당되면 Value Object 분리:

1. 의미 있는 개념 이름
2. 유효성 검증 필요
3. 여러 Entity에서 재사용

```java
@Embeddable
public class Money {
    
    private BigDecimal amount;
    
    protected Money() {}
    
    public Money(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상");
        }
        this.amount = amount;
    }
    
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
    
    // equals, hashCode 필수
}
```

---

## 10. 빠른 참조

### 의존성 규칙

| 레이어 | 주입 가능 | 주입 불가 |
|--------|----------|----------|
| **Controller** | Service, Mapper | Repository, Port |
| **Service** | 자기 Repository, Port | 다른 Repository, 다른 Service |
| **Port 구현** | 다른 Repository | Service |
| **Domain** | 없음 | 모두 |

### 작성 순서 (권장)

```
1. Domain (Entity, Enum)
   ↓
2. Repository Interface (Domain)
   ↓
3. Port Interface (Application)
   ↓
4. Service (Application)
   ↓
5. Port 구현 (Infrastructure)
   ↓
6. DTO (Presentation)
   ↓
7. Controller (Presentation)
```

### 자주 찾는 패턴

| 작성할 것 | 참고 문서 |
|---|---|
| Entity 템플릿 | [이 문서 § 3.1](STRUCTURE.md#31-기본-템플릿) |
| Service 템플릿 | [이 문서 § 4.1](STRUCTURE.md#41-기본-템플릿) |
| Port 설계 | [이 문서 § 5](STRUCTURE.md#5-port-설계) |
| 전체 코드 예시 | [Sample_code.md](Sample_code.md) |
| 왜 이렇게? | [CORE.md](CORE.md) |
| 용어 모름 | [GLOSSARY.md](GLOSSARY.md) |

---

## 한 줄 요약

> **"Entity는 § 3,  
> Service는 § 4,  
> Port는 § 5 보기."**

---