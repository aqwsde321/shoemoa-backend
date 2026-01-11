# CORE.md

## 프로젝트 핵심 설계 원칙

**이 문서는 팀 합의된 절대 규칙이며, 변경 시 전체 동의가 필요합니다.**

---

## 🎯 프로젝트 한 문장 정의

> **"JPA Entity를 Domain으로 사용하되,  
> Service는 오케스트레이션만 하고,  
> Aggregate 간 협력은 Port로 한다."**

---

## 📐 핵심 규칙 3가지

### 1. 비즈니스 로직은 Entity 메서드에

```java
// ❌ 금지: Service에 비즈니스 로직
@Service
public class OrderService {
    public void cancel(Order order) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.CANCELLED);
    }
}

// ✅ 허용: Entity에 비즈니스 로직
@Entity
public class Order {
    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("취소 불가 상태");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

---

### 2. Service는 if/else 금지

```java
// ❌ 금지: Service에서 비즈니스 판단
@Service
public class OrderService {
    public void process(Order order) {
        if (order.getTotalPrice() > 100_000) {
            order.setDiscount(10);
        } else {
            order.setDiscount(0);
        }
    }
}

// ✅ 허용: Domain 메서드 호출만
@Service
public class OrderService {
    @Transactional
    public void process(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow();
        order.applyDiscount();  // 로직은 Entity에
        orderRepository.save(order);
    }
}
```

---

### 3. Aggregate 간 직접 참조 금지

```java
// ❌ 금지: 다른 Aggregate Repository 직접 주입
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;  // 다른 Aggregate
}

// ✅ 허용: Port 사용
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberValidator memberValidator;  // Port
}
```

---

## 🏗️ 레이어 책임

| 레이어         | 책임                 | 허용                     | 금지                       |
| -------------- | -------------------- | ------------------------ | -------------------------- |
| **Controller** | HTTP 요청/응답       | DTO 변환                 | Entity 반환, 비즈니스 로직 |
| **Service**    | 유즈케이스 흐름 제어 | Port 호출, 트랜잭션 경계 | if/else 비즈니스 분기      |
| **Domain**     | 비즈니스 규칙        | 상태 변경, 검증          | 다른 Aggregate 참조        |
| **Repository** | 영속성               | 저장/조회                | 비즈니스 로직              |

---

## 🔗 Aggregate 간 협력 원칙

### 기본 방침

**OrderService는 Order 이외의 Aggregate와 Port로만 협력한다.**

```
OrderService
  |
  |-- OrderRepository (직접 주입 ✅)
  |
  |-- MemberValidator (Port ✅)
  |     └─ JpaMemberValidator
  |           └─ MemberRepository
  |
  |-- StockManager (Port ✅)
        └─ JpaStockManager
              └─ ProductRepository
```

---

### Port를 사용하는 이유

1. **Aggregate 경계 유지**

   - OrderService는 Member, Product 내부 구조를 모름

2. **테스트 가능성**

   - Fake 구현체로 UseCase 단위 테스트 가능

3. **변경 영향 최소화**
   - Member 테이블 구조 변경 시 OrderService 무영향

---

### Port 사용 예시

```java
// Application Layer
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
            .orElseThrow();
        if (!member.isActive()) {
            throw new IllegalStateException("비활성 회원");
        }
    }
}

// Service
@Service
public class OrderService {
    private final MemberValidator memberValidator;

    @Transactional
    public Long createOrder(Long memberId, ...) {
        memberValidator.validateActive(memberId);  // Port 사용
        // ...
    }
}
```

---

## 🚫 절대 금지 사항

### 1. Entity Setter 사용

```java
// ❌ 금지
order.setStatus(OrderStatus.CANCELLED);

// ✅ 허용
order.cancel();
```

---

### 2. Service → Service 주입

```java
// ❌ 금지
@Service
public class OrderService {
    private final ProductService productService;
}

// ✅ 허용
@Service
public class OrderService {
    private final StockManager stockManager;  // Port
}
```

**이유**: 순환 참조, 트랜잭션 경계 모호, Service 비대화

---

### 3. 다른 Aggregate Repository 직접 주입

```java
// ❌ 금지
@Service
public class OrderService {
    private final MemberRepository memberRepository;
}

// ✅ 허용
@Service
public class OrderService {
    private final MemberValidator memberValidator;  // Port
}
```

---

### 4. Controller에서 Entity 반환

```java
// ❌ 금지
@GetMapping("/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderService.getOrder(id);
}

// ✅ 허용
@GetMapping("/{id}")
public OrderResponse getOrder(@PathVariable Long id) {
    Order order = orderService.getOrder(id);
    return OrderResponse.from(order);
}
```

---

## 🧪 테스트 전략

### 3단계 테스트

| 테스트              | 대상                 | 기술            | 목적            |
| ------------------- | -------------------- | --------------- | --------------- |
| **Domain Test**     | Entity 비즈니스 로직 | 순수 자바       | 규칙 검증       |
| **UseCase Test**    | Service 흐름         | Fake Port       | 유즈케이스 검증 |
| **Controller Test** | API 엔드포인트       | @SpringBootTest | 전체 통합 검증  |

---

### Domain Test (순수 자바)

```java
@Test
void 주문_취소_테스트() {
    // given
    Order order = Order.create(1L, 10L, 3);

    // when
    order.cancel();

    // then
    assertThat(order.getStatus()).isEqualTo(CANCELLED);
}
```

**특징**:

- Spring ❌
- JPA ❌
- Mock ❌

---

### UseCase Test (Fake Port)

```java
@Test
void 비활성_회원은_주문_불가() {
    // given
    FakeMemberValidator validator = new FakeMemberValidator();
    validator.setActive(1L, false);

    OrderService service = new OrderService(..., validator, ...);

    // when & then
    assertThatThrownBy(() ->
        service.createOrder(1L, 10L, 3)
    ).isInstanceOf(IllegalStateException.class);
}
```

**특징**:

- Spring ❌
- Fake 구현체 ✅

---

### Controller Test (통합)

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Test
    void 주문_생성_API() throws Exception {
        mockMvc.perform(post("/api/orders")
            .content(...))
            .andExpect(status().isOk());
    }
}
```

**특징**:

- Spring ✅
- 전체 흐름 검증 ✅

---

## 📦 도메인 정의

### Domain = JPA Entity

본 프로젝트에서:

> **Domain Entity = JPA Entity**

도메인 모델과 영속 모델을 **분리하지 않는다**.

---

### 하지만 DDD 규율은 강제

JPA Entity를 사용하되, 다음 규칙을 지킨다:

1. **Setter 금지**
2. **비즈니스 로직 포함**
3. **Aggregate 간 직접 참조 금지**
4. **생성자/팩토리로만 생성**

---

### 예시

```java
@Entity
public class Order {

    @Id @GeneratedValue
    private Long id;

    private Long memberId;  // ID 참조 (Entity 참조 아님)

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // JPA용
    protected Order() {}

    // 정적 팩토리
    public static Order create(Long memberId, Long productId, int quantity) {
        return new Order(memberId, productId, quantity);
    }

    // private 생성자 (검증 포함)
    private Order(Long memberId, Long productId, int quantity) {
        validate(quantity);
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
}
```

---

## 🎓 이 구조를 선택한 이유

### 1. 실무 친화성

- 대부분의 Spring/JPA 프로젝트는 Entity = Domain
- 학습 후 바로 실무 투입 가능

---

### 2. 복잡도 최소화

- Domain/Entity 분리는 학습 곡선 높음
- 핵심만 집중: "로직을 어디에 둘 것인가"

---

### 3. 점진적 개선 가능

현재: JPA Entity = Domain  
향후: Domain ↔ Entity 분리 가능 (필요 시)

**전제 조건**: 비즈니스 로직이 Entity에 잘 응집되어 있어야 함

---

## 🔄 변경 규칙

### 이 문서(CORE.md)는 팀 합의 없이 변경 불가

변경 시:

1. ADR(Architecture Decision Record) 작성
2. 팀 전체 동의
3. 영향 받는 코드 범위 분석
4. 기존 코드 마이그레이션 계획

---

## 📋 요약 테이블

### 레이어별 의존성

| 레이어          | 주입 가능             | 주입 불가                     |
| --------------- | --------------------- | ----------------------------- |
| **Controller**  | Service, Mapper       | Repository, Port              |
| **Service**     | 자기 Repository, Port | 다른 Repository, 다른 Service |
| **Port 구현체** | 다른 Repository       | Service                       |
| **Domain**      | 없음                  | Repository, Service, Port     |

---

### Port 종류

| 종류      | 네이밍     | 역할                | 예시            |
| --------- | ---------- | ------------------- | --------------- |
| 검증      | ~Validator | 읽기 전용 검증      | MemberValidator |
| 상태 변경 | ~Manager   | 다른 Aggregate 수정 | StockManager    |
| 조회      | ~Reader    | DTO 조회            | ProductReader   |
| 외부 연동 | ~Gateway   | 외부 API            | PaymentGateway  |

---

## 한 줄 요약

> **"비즈니스 로직은 Entity에,  
> 흐름 제어는 Service에,  
> Aggregate 협력은 Port로."**

---
