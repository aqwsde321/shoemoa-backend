# 프로젝트 핵심 원칙

## 🎯 한 문장 정의

"비즈니스 로직은 Entity에 두고, Service는 Repository를 자유롭게 사용한다."

---

## 📐 핵심 규칙 3가지

### 1. 비즈니스 로직은 Entity 메서드에

```java
// ❌ Service에 로직
@Service
public class OrderService {
    public void cancel(Order order) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.CANCELLED);
    }
}

// ✅ Entity에 로직
@Entity
public class Order {
    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("취소 불가");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

### 2. Service는 흐름 조합만

```java
@Service
public class OrderService {
    
    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        // 1. 조회
        Member member = memberRepository.findById(memberId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();
        
        // 2. 검증 (Entity 메서드)
        member.validateActive();
        
        // 3. 상태 변경 (Entity 메서드)
        product.decreaseStock(quantity);
        
        // 4. 생성
        Order order = Order.create(memberId, productId, quantity, product.getPrice());
        
        // 5. 저장
        return orderRepository.save(order).getId();
    }
}
```

**Service의 역할**
- Repository 조회
- Entity 메서드 호출
- 트랜잭션 관리

**Service가 하면 안 되는 것**
- if/else 비즈니스 분기
- 계산 로직
- 상태 직접 변경

### 3. Setter 금지

```java
// ❌ 금지
order.setStatus(OrderStatus.CANCELLED);
product.setStock(product.getStock() - 3);

// ✅ 허용
order.cancel();
product.decreaseStock(3);
```

---

## 🏗️ 레이어 구조

```
Controller
  - HTTP 요청/응답
  - DTO 변환
    ↓
Service
  - Repository 호출
  - Entity 메서드 호출
  - 트랜잭션 관리
    ↓
Entity
  - 비즈니스 규칙
  - 상태 변경
  - 검증
    ↓
Repository
  - DB 저장/조회
```

---

## 🚫 절대 금지 사항

### 1. Entity에서 Setter 사용

```java
// ❌ 금지
public void setStatus(OrderStatus status) {
    this.status = status;
}

// ✅ 허용
public void cancel() {
    if (!this.status.isCancelable()) {
        throw new IllegalStateException("취소 불가");
    }
    this.status = OrderStatus.CANCELLED;
}
```

### 2. Service에서 비즈니스 로직

```java
// ❌ 금지
if (order.getTotalPrice() > 100_000) {
    order.setDiscount(10);
}

// ✅ 허용
order.applyDiscount();  // Entity 메서드
```

### 3. Controller에서 Entity 반환

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

## 📝 Repository 사용 규칙

**Service에서 필요한 모든 Repository 사용 가능**

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;    // ✅ 가능
    private final ProductRepository productRepository;  // ✅ 가능
    
    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        // 모든 Repository 자유롭게 사용
        Member member = memberRepository.findById(memberId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();
        
        // 단, 비즈니스 로직은 Entity 메서드로
        member.validateActive();
        product.decreaseStock(quantity);
        
        Order order = Order.create(memberId, productId, quantity, product.getPrice());
        return orderRepository.save(order).getId();
    }
}
```

---

## 🧪 테스트 전략

**2가지 테스트**

```java
// 1. Domain Test - 순수 자바
@Test
void 주문_취소_성공() {
    Order order = Order.create(1L, 10L, 3, 1000);
    
    order.cancel();
    
    assertThat(order.getStatus()).isEqualTo(CANCELLED);
}

// 2. Integration Test - Spring + 실제 DB
@SpringBootTest
@Transactional
class OrderServiceTest {
    @Test
    void 주문_생성_성공() {
        Member member = memberRepository.save(Member.create(...));
        Product product = productRepository.save(Product.create(...));
        
        Long orderId = orderService.createOrder(member.getId(), product.getId(), 3);
        
        assertThat(orderId).isNotNull();
    }
}
```

---

## 한 줄 요약

**"로직은 Entity에, 흐름은 Service에, 안전함은 Test로."**