# QUICK_REFERENCE.md

## 빠른 참조 가이드

**이 문서는 코드 작성 중 빠르게 확인하는 체크리스트와 FAQ입니다.**

---

## 📋 PR 전 필수 체크리스트

### Entity 관련
```
[ ] [Entity에 public Setter가 없는가?](STRUCTURE.md#32-setter-금지)
[ ] [기본 생성자가 protected인가?](STRUCTURE.md#33-생성자-규칙)
[ ] [비즈니스 로직이 Entity 메서드로 구현되었는가?](CORE.md#1-비즈니스-로직은-entity-메서드에)
[ ] [다른 Aggregate Entity를 필드로 참조하지 않는가?](STRUCTURE.md#34-aggregate-간-관계-규칙)
[ ] [정적 팩토리 메서드 또는 명시적 생성자를 사용했는가?](STRUCTURE.md#33-생성자-규칙)
```

---

### Service 관련
```
[ ] [Service에 if/else 비즈니스 분기가 없는가?](CORE.md#2-service는-ifelse-금지)
[ ] [Service가 다른 Service를 주입받지 않는가?](STRUCTURE.md#42-service-금지-사항)
[ ] [다른 Aggregate Repository를 직접 주입받지 않는가?](STRUCTURE.md#44-repository-사용-규칙)
[ ] [Port 인터페이스를 사용했는가?](STRUCTURE.md#5-port-설계-가이드-핵심)
[ ] [@Transactional이 Service 메서드에 있는가?](STRUCTURE.md#57-트랜잭션-경계)
```

---

### Port 관련
```
[ ] [Port 이름이 역할을 명확히 표현하는가?](STRUCTURE.md#53-port-분류-및-네이밍)
[ ] [Port가 Entity를 반환하지 않는가?](STRUCTURE.md#54-port-설계-원칙)
[ ] [Port 구현체가 Infrastructure 패키지에 있는가?](STRUCTURE.md#1-패키지-구조)
[ ] [Port 인터페이스가 application/port 패키지에 있는가?](STRUCTURE.md#1-패키지-구조)
```

---

### Controller 관련
```
[ ] [Controller에서 Entity를 직접 반환하지 않는가?](CORE.md#4-controller에서-entity-반환)
[ ] [Request/Response DTO를 사용했는가?](Sample_code.md#41-request-dto)
[ ] [Controller에 비즈니스 로직이 없는가?](CORE.md#--레이어-책임)
```

---

### 테스트 관련
```
[ ] [Domain Test는 순수 자바로 작성했는가?](TESTING.md#2-domain-test-순수-자바)
[ ] [UseCase Test는 Fake Port를 사용했는가?](TESTING.md#3-usecase-test-fake-port)
[ ] [Controller Test는 @SpringBootTest로 작성했는가?](TESTING.md#4-controller-통합-테스트)
[ ] [핵심 비즈니스 규칙에 대한 Domain Test가 있는가?](TESTING.md#7-테스트-커버리지-목표)
```

---

## ❓ 자주 묻는 질문 (FAQ)

### Q1. Service에서 다른 도메인 Repository를 주입받아도 되나요?

**A. ❌ 안 됩니다. [Port를 사용하세요.](STRUCTURE.md#5-port-설계-가이드-핵심)**
```java
// ❌ 금지
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;  // 다른 Aggregate
}

// ✅ 허용
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberValidator memberValidator;  // Port
}
```

**이유**:
- Aggregate 경계 파괴 ([CORE.md](CORE.md#3-aggregate-간-직접-참조-금지))
- 결합도 증가
- 테스트 어려움 ([TESTING.md](TESTING.md#3-usecase-test-fake-port))

---

### Q2. Service가 다른 Service를 주입받아도 되나요?

**A. ❌ 절대 안 됩니다. [Port를 사용하세요.](STRUCTURE.md#5-port-설계-가이드-핵심)**
```java
// ❌ 금지
@Service
public class OrderService {
    private final ProductService productService;  // Service 주입 금지
}

// ✅ 허용
@Service
public class OrderService {
    private final StockManager stockManager;  // Port 사용
}
```

**이유**:
- 순환 참조 위험
- 트랜잭션 경계 애매 ([STRUCTURE.md](STRUCTURE.md#57-트랜잭션-경계))
- Service 계층 비대화

---

### Q3. Port는 언제 만드나요?

**A. [다음 경우에만 만듭니다:](STRUCTURE.md#52-port가-필요한-경우)**

1. **다른 Aggregate 상태 검증**
```java
   public interface MemberValidator {
       void validateActive(Long memberId);
   }
```

2. **다른 Aggregate 상태 변경**
```java
   public interface StockManager {
       void decrease(Long productId, int quantity);
   }
```

3. **외부 시스템 연동**
```java
   public interface PaymentGateway {
       PaymentResult pay(PaymentRequest request);
   }
```

**만들지 않는 경우**:
- 단순 존재 여부 확인 (existsById)
- 자기 Aggregate 내부 로직

---

### Q4. Entity에 Setter를 꼭 안 써야 하나요?

**A. ✅ 네, [절대 안 됩니다.](STRUCTURE.md#32-setter-금지)**
```java
// ❌ 금지
public class Order {
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}

// ✅ 허용
public class Order {
    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("취소할 수 없는 상태");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

**이유**:
- 무분별한 상태 변경 방지
- 비즈니스 규칙 명확화
- 불변 조건 유지

---

### Q5. Controller에서 Entity를 반환하면 안 되나요?

**A. ❌ 안 됩니다. [DTO를 사용하세요.](STRUCTURE.md#24-presentation-layer-controller)**
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

**이유**:
- Entity 내부 구조 노출
- 지연 로딩 문제 (N+1)
- JSON 순환 참조

---

### Q6. Port가 Entity를 반환해도 되나요?

**A. ❌ 안 됩니다. [DTO 또는 원시값만 반환하세요.](STRUCTURE.md#54-port-설계-원칙)**
```java
// ❌ 금지
public interface ProductReader {
    Product findById(Long id);  // Entity 반환
}

// ✅ 허용
public interface ProductReader {
    ProductInfo getInfo(Long id);  // DTO 반환
    boolean hasStock(Long productId, int quantity);  // 원시값
}
```

**이유**:
- Aggregate 경계 유지
- Entity는 자기 Aggregate 내에서만 존재

---

### Q7. Port 구현체에 @Transactional을 붙여야 하나요?

**A. ❌ 안 됩니다. [Service에만 붙입니다.](STRUCTURE.md#57-트랜잭션-경계)**
```java
// Service: @Transactional 있음
@Service
public class OrderService {
    @Transactional  // ← 여기
    public Long createOrder(...) {
        stockManager.decrease(...);
        orderRepository.save(...);
    }
}

// Port 구현체: @Transactional 없음
@Component
public class JpaStockManager implements StockManager {
    @Override  // @Transactional 없음
    public void decrease(Long productId, int quantity) {
        // ...
    }
}
```

**이유**: Service의 @Transactional이 Port 구현체까지 전파됨

---

### Q8. Domain Test에서 Spring을 사용하면 안 되나요?

**A. ✅ 네, [순수 자바로만 작성합니다.](TESTING.md#2-domain-test-순수-자바)**
```java
// ✅ 허용: 순수 자바
@Test
void 주문_취소_테스트() {
    Order order = Order.create(1L, 10L, 3);
    
    order.cancel();
    
    assertThat(order.getStatus()).isEqualTo(CANCELLED);
}

// ❌ 금지: Spring 사용
@SpringBootTest
class OrderTest {
    @Autowired
    OrderRepository orderRepository;  // Domain Test에서 금지
}
```

**이유**:
- 비즈니스 로직은 기술과 무관
- 빠른 테스트 실행
- Mock 불필요

---

### Q9. UseCase 단위 테스트를 꼭 만들어야 하나요?

**A. ✅ 네, [Port를 사용한다면 필수입니다.](TESTING.md#3-usecase-test-fake-port)**
```java
class OrderServiceTest {
    
    private FakeMemberValidator memberValidator;
    private FakeStockManager stockManager;
    private InMemoryOrderRepository orderRepository;
    private OrderService orderService;
    
    @BeforeEach
    void setUp() {
        memberValidator = new FakeMemberValidator();
        stockManager = new FakeStockManager();
        orderRepository = new InMemoryOrderRepository();
        
        orderService = new OrderService(
            memberValidator,
            stockManager,
            orderRepository
        );
    }
    
    @Test
    void 재고_부족_시_주문_실패() {
        stockManager.setStock(1L, 2);
        
        assertThatThrownBy(() ->
            orderService.createOrder(1L, 1L, 3)
        ).isInstanceOf(IllegalStateException.class);
    }
}
```

**이유**: Port 사용의 핵심 가치는 Fake로 UseCase 테스트

---

### Q10. 다른 Aggregate를 수정해야 하는데 어떻게 하나요?

**A. [Port의 Manager를 사용하세요.](STRUCTURE.md#53-port-분류-및-네이밍)**
```java
// Port 정의
public interface StockManager {
    void decrease(Long productId, int quantity);
}

// Port 구현
@Component
public class JpaStockManager implements StockManager {
    private final ProductRepository productRepository;
    
    @Override
    public void decrease(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow();
        product.decreaseStock(quantity);  // Domain 메서드 호출
        productRepository.save(product);
    }
}

// Service에서 사용
@Service
public class OrderService {
    private final StockManager stockManager;
    
    @Transactional
    public Long createOrder(...) {
        stockManager.decrease(productId, quantity);
        // ...
    }
}
```

---

## 🎯 용어 빠른 참조

### Port vs Repository

| 구분 | Port | Repository |
|------|------|------------|
| 위치 | application/port | domain |
| 역할 | Aggregate 간 협력 추상화 | Aggregate 영속성 |
| 구현 위치 | infrastructure | infrastructure |
| 예시 | MemberValidator | OrderRepository |

---

### Service vs Domain Service

| 구분 | Application Service | Domain Service |
|------|---------------------|----------------|
| 위치 | application | domain |
| 역할 | 유즈케이스 흐름 제어 | 여러 Entity 간 로직 |
| 트랜잭션 | ✅ 있음 | ❌ 없음 |
| 우리 프로젝트 | ✅ 사용 | ❌ 사용 안 함 |

**참고**: 이 프로젝트는 Domain Service를 사용하지 않습니다.

---

### UseCase vs Application Service

**같은 의미입니다.**

- UseCase: 개념적 표현
- Application Service: 실제 구현 클래스명
```java
// "주문 생성" UseCase
@Service
public class OrderService {  // Application Service
    public Long createOrder(...) {  // UseCase 메서드
        // ...
    }
}
```

---

### Aggregate vs Entity

| 구분 | Aggregate | Entity |
|------|-----------|--------|
| 의미 | 일관성 경계를 가진 묶음 | 식별자를 가진 객체 |
| 예시 | Order (Root) + OrderItem | Order, OrderItem 각각 |
| 접근 | Root를 통해서만 | - |

**핵심**: 
- Aggregate Root = 외부 접근 진입점
- 내부 Entity는 직접 접근 금지

---

## 🔍 코드 패턴 빠른 참조

### [Entity 생성 패턴](Sample_code.md#11-order-entity-aggregate-root)
```java
@Entity
public class Order {
    
    @Id @GeneratedValue
    private Long id;
    
    private Long memberId;
    private int quantity;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    // JPA용 기본 생성자
    protected Order() {}
    
    // 정적 팩토리 메서드
    public static Order create(Long memberId, int quantity) {
        return new Order(memberId, quantity);
    }
    
    // private 생성자
    private Order(Long memberId, int quantity) {
        validate(quantity);
        this.memberId = memberId;
        this.quantity = quantity;
        this.status = OrderStatus.CREATED;
    }
    
    private void validate(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상");
        }
    }
    
    // 비즈니스 메서드
    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("취소 불가");
        }
        this.status = OrderStatus.CANCELLED;
    }
    
    // getter만 노출
    public Long getId() { return id; }
    public OrderStatus getStatus() { return status; }
}
```

---

### [Port 정의 패턴](Sample_code.md#21-port-interfaces)
```java
// Application Layer
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
            .orElseThrow(() -> new EntityNotFoundException());
        
        if (!member.isActive()) {
            throw new IllegalStateException("비활성 회원");
        }
    }
}
```

---

### [Service 작성 패턴](Sample_code.md#22-orderservice-application-service)
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
        // 1. Port를 통한 검증
        memberValidator.validateActive(memberId);
        
        // 2. Port를 통한 상태 변경
        stockManager.decrease(productId, quantity);
        
        // 3. Domain 생성
        Order order = Order.create(memberId, productId, quantity);
        
        // 4. 저장
        return orderRepository.save(order).getId();
    }
}
```

---

### [Fake Port 작성 패턴](Sample_code.md#52-fake-port-구현체)
```java
class FakeMemberValidator implements MemberValidator {
    
    private Map<Long, Boolean> activeStatus = new HashMap<>();
    
    public void setActive(Long memberId, boolean active) {
        activeStatus.put(memberId, active);
    }
    
    @Override
    public void validateActive(Long memberId) {
        if (!activeStatus.getOrDefault(memberId, true)) {
            throw new IllegalStateException("비활성 회원");
        }
    }
}
```

---

## 🚨 흔한 실수

### 실수 1: Service에 비즈니스 로직
```java
// ❌ 잘못된 예
@Service
public class OrderService {
    public void createOrder(...) {
        if (quantity > 10) {  // 비즈니스 규칙
            throw new IllegalArgumentException("10개 이하만 가능");
        }
    }
}

// ✅ 올바른 예
@Entity
public class Order {
    private Order(int quantity) {
        if (quantity > 10) {  // Domain이 규칙 소유
            throw new IllegalArgumentException("10개 이하만 가능");
        }
    }
}
```

---

### 실수 2: Port가 Entity 반환
```java
// ❌ 잘못된 예
public interface ProductPort {
    Product findById(Long id);
}

// ✅ 올바른 예
public interface ProductValidator {
    void validateAvailable(Long productId);
}
```

---

### 실수 3: Controller가 Entity 반환
```java
// ❌ 잘못된 예
@GetMapping("/{id}")
public Order get(@PathVariable Long id) {
    return orderService.get(id);
}

// ✅ 올바른 예
@GetMapping("/{id}")
public OrderResponse get(@PathVariable Long id) {
    Order order = orderService.get(id);
    return OrderResponse.from(order);
}
```

---

## 📌 Port 네이밍 체크

| 역할 | 네이밍 | 예시 | 메서드 예시 |
|------|--------|------|------------|
| 검증 | ~Validator | MemberValidator | validateActive(Long id) |
| 상태 변경 | ~Manager | StockManager | decrease(Long id, int qty) |
| 조회 | ~Reader | ProductReader | getInfo(Long id) |
| 외부 연동 | ~Gateway | PaymentGateway | pay(PaymentRequest req) |

---

## ✅ 체크리스트 요약 (암기용)
```
Entity:
- [ ] Setter 없음
- [ ] protected 기본 생성자
- [ ] 비즈니스 메서드 있음

Service:
- [ ] if/else 비즈니스 분기 없음
- [ ] Service 주입 없음
- [ ] 다른 Repo 주입 없음
- [ ] Port 사용

Port:
- [ ] 네이밍 규칙 준수
- [ ] Entity 반환 안 함
- [ ] Infrastructure에 구현체

Test:
- [ ] Domain은 순수 자바
- [ ] UseCase는 Fake Port
- [ ] Controller는 통합
```

---

## 한 줄 요약

> **"막히면 이 문서 찾기.  
> PR 전엔 체크리스트 확인.  
> FAQ 3번 이상 읽기."**

---