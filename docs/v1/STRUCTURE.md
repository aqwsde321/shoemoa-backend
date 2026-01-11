# STRUCTURE.md

## 프로젝트 구조 및 작성 가이드

본 문서는 **실제 코드 작성 시 참고하는 실무 가이드**이다.  
패키지 구조, Domain/Service 작성 규칙, Port 설계 방법을 정의한다.

---

## 1. 패키지 구조

본 프로젝트는 **도메인 중심 + 레이어 분리** 구조를 따른다.

```
com.shop
├─ domain
│  ├─ order
│  │  ├─ Order.java              // Aggregate Root (JPA Entity)
│  │  ├─ OrderItem.java          // Entity
│  │  ├─ OrderStatus.java        // Enum
│  │  └─ OrderRepository.java    // Repository Interface
│  ├─ member
│  │  ├─ Member.java
│  │  └─ MemberRepository.java
│  └─ product
│     ├─ Product.java
│     └─ ProductRepository.java
│
├─ application
│  └─ order
│     ├─ OrderService.java       // UseCase (Application Service)
│     └─ port
│        ├─ MemberValidator.java     // Port Interface (검증)
│        ├─ ProductValidator.java
│        ├─ StockManager.java        // Port Interface (상태 변경)
│        └─ PaymentGateway.java      // Port Interface (외부 시스템)
│
├─ infrastructure
│  ├─ member
│  │  └─ JpaMemberValidator.java     // Port 구현체
│  ├─ product
│  │  ├─ JpaProductValidator.java
│  │  └─ JpaStockManager.java
│  ├─ payment
│  │  └─ TossPaymentGateway.java
│  └─ order
│     ├─ OrderDslRepository.java     // QueryDSL Interface
│     └─ OrderDslRepositoryImpl.java // QueryDSL 구현체
│
├─ presentation
│  └─ order
│     ├─ OrderController.java
│     ├─ request
│     │  └─ OrderCreateRequest.java
│     └─ response
│        └─ OrderCreateResponse.java
│
└─ global
   ├─ config
   └─ exception
```

---

## 2. 레이어별 책임

### 2.1 Domain Layer

**책임**:

- 비즈니스 규칙의 중심
- 상태 + 행위 응집
- Aggregate Root 기준 설계

**허용**:

- ✅ 상태 변경 로직
- ✅ 도메인 규칙
- ✅ 유효성 검증

**금지**:

- ❌ 다른 Aggregate Entity 직접 참조
- ❌ Repository 호출
- ❌ 외부 시스템 의존

---

### 2.2 Application Layer (Service)

**책임**:

- 유즈케이스 단위 흐름 제어
- 여러 도메인/포트 조합
- 트랜잭션 경계 설정

**허용**:

- ✅ 자기 Aggregate Repository 호출
- ✅ Port 인터페이스 호출
- ✅ 도메인 객체 생성/조합

**금지**:

- ❌ 비즈니스 규칙 구현 (if/else 분기)
- ❌ 엔티티 상태 직접 조작
- ❌ 다른 Service 주입
- ❌ 다른 Aggregate Repository 직접 주입

---

### 2.3 Infrastructure Layer

**책임**:

- Port 구현체 제공
- 기술 상세 구현
- 외부 시스템 연동

**포함**:

- Port 구현 (JpaMemberValidator 등)
- QueryDSL 구현
- 외부 API 연동

---

### 2.4 Presentation Layer (Controller)

**책임**:

- HTTP 요청/응답 처리
- DTO 변환

**금지**:

- ❌ 비즈니스 로직
- ❌ Entity 직접 반환

---

## 3. Domain(Entity) 작성 규칙

### 3.1 기본 원칙

JPA Entity = Domain Entity  
단, DDD 규율을 강제한다.

---

### 3.2 Setter 금지

모든 Entity는 **무분별한 Setter 사용을 금지**한다.  
상태 변경은 의미 있는 메서드로만 허용한다.

```java
// ❌ 금지
order.setStatus(OrderStatus.CANCELLED);

// ✅ 허용
order.cancel();
```

---

### 3.3 생성자 규칙

**기본 생성자는 JPA 용도로만 사용** (`protected`).  
외부에서는 정적 팩토리 또는 명시적 생성자만 사용.

```java
@Entity
public class Order {

    @Id @GeneratedValue
    private Long id;

    private Long memberId;
    private Long productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // JPA용 기본 생성자
    protected Order() {}

    // 정적 팩토리 메서드
    public static Order create(Long memberId, Long productId, int quantity) {
        return new Order(memberId, productId, quantity);
    }

    // private 생성자 (검증 포함)
    private Order(Long memberId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.CREATED;
    }

    // 비즈니스 메서드
    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("취소할 수 없는 상태입니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // getter만 노출
    public Long getId() { return id; }
    public OrderStatus getStatus() { return status; }
}
```

---

### 3.4 Aggregate 간 관계 규칙

**다른 Aggregate의 Entity를 필드로 참조하지 않는다.**

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

### 3.5 Domain 메서드 인자 규칙

Domain 메서드는 **자기 Aggregate의 정보만 받는다**.

**허용**:

- Primitive / Wrapper
- Value Object
- ID 값

```java
order.changeQuantity(3);
order.assignProduct(productId);
```

**금지**:

- 다른 Aggregate Entity 전달
- DTO 전달
- Repository 전달

```java
// ❌ 금지
order.place(member, product);
```

---

### 3.6 검증 책임

**상태 기반 검증은 Domain이 담당**.  
**외부 존재 여부 검증은 Application(Service)가 담당**.

```java
// Domain
public void cancel() {
    if (!this.status.isCancelable()) {
        throw new IllegalStateException("취소 불가 상태");
    }
    this.status = OrderStatus.CANCELLED;
}
```

---

## 4. Service(Application) 작성 규칙

### 4.1 Service의 역할

Service는 다음 역할만 수행한다:

1. 하나의 유즈케이스를 실행
2. 여러 도메인 객체의 흐름을 조합
3. 트랜잭션 경계를 정의
4. Port를 통해 외부 협력

> Service는 "무엇을 할지"를 결정하지 않고,  
> **"이미 정해진 규칙을 언제 호출할지"만 결정한다.**

---

### 4.2 Service 금지 사항

Service에 다음 로직이 들어가면 설계 위반이다.

```java
// ❌ 금지: 비즈니스 규칙
if (order.getTotalPrice() > 100_000) {
    order.setDiscount(10);
}

// ❌ 금지: 상태 직접 변경
order.setStatus(OrderStatus.CANCELLED);

// ❌ 금지: 다른 Service 주입
@Service
public class OrderService {
    private final ProductService productService;  // ❌
}

// ❌ 금지: 다른 Aggregate Repository 직접 주입
@Service
public class OrderService {
    private final MemberRepository memberRepository;  // ❌
}
```

---

### 4.3 Service 허용 사항

```java
@Service
public class OrderService {

    // ✅ 허용: 자기 Aggregate Repository
    private final OrderRepository orderRepository;

    // ✅ 허용: Port 인터페이스
    private final MemberValidator memberValidator;
    private final StockManager stockManager;

    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        // 1. Port를 통한 검증
        memberValidator.validateActive(memberId);

        // 2. Port를 통한 상태 변경
        stockManager.decrease(productId, quantity);

        // 3. 도메인 객체 생성
        Order order = Order.create(memberId, productId, quantity);

        // 4. 자기 Repository 저장
        return orderRepository.save(order).getId();
    }
}
```

---

### 4.4 Repository 사용 규칙

**자기 Aggregate Repository만 직접 주입 가능**.

```java
// ✅ 허용
@Service
public class OrderService {
    private final OrderRepository orderRepository;  // 자기 Aggregate
}

// ❌ 금지
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;  // 다른 Aggregate
    private final ProductRepository productRepository;  // 다른 Aggregate
}
```

**다른 Aggregate와 협력이 필요하면 Port를 사용한다.**

---

## 5. Port 설계 가이드 (핵심)

### 5.1 Port란?

**Application Layer에서 외부 의존성을 추상화한 인터페이스**.

목적:

1. Aggregate 간 직접 결합 방지
2. UseCase 단위 테스트 가능 (Fake 사용)
3. 도메인 의도 명확화

---

### 5.2 Port가 필요한 경우

다음 경우에만 Port를 만든다:

1. **다른 Aggregate의 상태 검증**

   - 예: 회원 활성화 여부, 상품 재고 확인

2. **다른 Aggregate의 정책 판단**

   - 예: 회원 등급별 할인율

3. **다른 Aggregate의 상태 변경**

   - 예: 재고 차감, 포인트 적립

4. **외부 시스템 연동**
   - 예: PG 결제, 알림톡 발송

---

### 5.3 Port 분류 및 네이밍

| 역할           | 네이밍       | 예시            | 설명                     |
| -------------- | ------------ | --------------- | ------------------------ |
| 읽기 전용 검증 | `~Validator` | MemberValidator | 상태 검증만 수행         |
| 상태 변경      | `~Manager`   | StockManager    | 다른 Aggregate 상태 변경 |
| 조회           | `~Reader`    | ProductReader   | DTO 조회                 |
| 외부 시스템    | `~Gateway`   | PaymentGateway  | 외부 API 연동            |

---

### 5.4 Port 설계 원칙

**Port는 "도메인 의도"를 표현한다.**

```java
// ✅ 좋은 Port: 도메인 의도 표현
public interface MemberValidator {
    void validateActive(Long memberId);
    void validateCanPurchase(Long memberId, int amount);
}

public interface StockManager {
    void decrease(Long productId, int quantity);
    void increase(Long productId, int quantity);
    void reserve(Long productId, int quantity);
}

// ❌ 나쁜 Port: Repository Wrapper
public interface MemberPort {
    Member findById(Long id);
    List<Member> findAll();
    void save(Member member);
}
```

**Port는 Entity를 반환하지 않는다.**

```java
// ❌ 금지: Entity 반환
public interface ProductReader {
    Product findById(Long id);
}

// ✅ 허용: DTO 또는 원시값 반환
public interface ProductReader {
    ProductInfo getInfo(Long id);
    boolean hasStock(Long productId, int quantity);
}
```

---

### 5.5 Port 구현 예시

#### 예시 1: 검증용 Port

```java
// Application Layer (port 패키지)
package com.shop.application.order.port;

public interface MemberValidator {
    void validateActive(Long memberId);
}

// Infrastructure Layer
package com.shop.infrastructure.member;

@Component
public class JpaMemberValidator implements MemberValidator {

    private final MemberRepository memberRepository;

    public JpaMemberValidator(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void validateActive(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));

        if (!member.isActive()) {
            throw new IllegalStateException("비활성 회원입니다.");
        }
    }
}
```

#### 예시 2: 상태 변경 Port

```java
// Application Layer
package com.shop.application.order.port;

public interface StockManager {
    void decrease(Long productId, int quantity);
    void increase(Long productId, int quantity);
}

// Infrastructure Layer
package com.shop.infrastructure.product;

@Component
public class JpaStockManager implements StockManager {

    private final ProductRepository productRepository;

    public JpaStockManager(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void decrease(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        // Domain 로직 호출
        product.decreaseStock(quantity);

        productRepository.save(product);
    }

    @Override
    public void increase(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow();

        product.increaseStock(quantity);
        productRepository.save(product);
    }
}
```

---

### 5.6 Service에서 Port 사용

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberValidator memberValidator;  // Port
    private final StockManager stockManager;        // Port

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
        // 1. 검증 (Port 사용)
        memberValidator.validateActive(memberId);

        // 2. 재고 차감 (Port 사용)
        stockManager.decrease(productId, quantity);

        // 3. 주문 생성 (Domain)
        Order order = Order.create(memberId, productId, quantity);

        // 4. 저장 (자기 Repository)
        return orderRepository.save(order).getId();
    }
}
```

---

### 5.7 트랜잭션 경계

**Application Service가 트랜잭션 경계**.  
Port 구현체는 `@Transactional` 없이 구현한다.

```java
// Service: @Transactional 있음
@Service
public class OrderService {

    @Transactional  // ← 여기서 트랜잭션 시작
    public Long createOrder(...) {
        stockManager.decrease(...);   // ← 같은 트랜잭션
        orderRepository.save(...);    // ← 같은 트랜잭션
    }
}

// Port 구현체: @Transactional 없음
@Component
public class JpaStockManager implements StockManager {

    // @Transactional 없음 ← 중요
    @Override
    public void decrease(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow();
        product.decreaseStock(quantity);
        productRepository.save(product);
    }
}
```

**이유**: Service의 `@Transactional`이 Port 구현체까지 전파됨.

---

## 6. Repository 설계 규칙

### 6.1 Repository 위치

Repository 인터페이스는 **Domain 패키지**에 위치한다.

```java
package com.shop.domain.order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

---

### 6.2 QueryDSL Repository

**QueryDSL 인터페이스와 구현체는 Infrastructure에 위치**.

```java
// Infrastructure Layer
package com.shop.infrastructure.order;

public interface OrderDslRepository {
    List<Order> findPaidOrders(LocalDate from, LocalDate to);
}

// Infrastructure Layer
package com.shop.infrastructure.order;

public class OrderDslRepositoryImpl implements OrderDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Order> findPaidOrders(LocalDate from, LocalDate to) {
        return queryFactory
            .selectFrom(order)
            .where(
                order.status.eq(OrderStatus.PAID),
                order.createdAt.between(from.atStartOfDay(), to.atTime(23, 59, 59))
            )
            .fetch();
    }
}
```

**Repository 연결**:

```java
package com.shop.domain.order;

public interface OrderRepository
        extends JpaRepository<Order, Long>, OrderDslRepository {
}
```

---

## 7. Value Object 사용 기준

다음 조건 중 2개 이상 해당되면 Value Object로 분리한다:

1. 의미 있는 개념 이름이 존재함
2. 유효성 검증 로직이 필요함
3. 여러 Entity에서 재사용됨

```java
@Embeddable
public class Money {

    private BigDecimal amount;

    protected Money() {}

    public Money(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
        this.amount = amount;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    // equals, hashCode 구현 필수
}
```

---

## 8. 한 줄 요약

> **비즈니스 로직은 Domain(Entity)에,  
> 흐름 제어는 Service에,  
> Aggregate 간 협력은 Port로.**

---
