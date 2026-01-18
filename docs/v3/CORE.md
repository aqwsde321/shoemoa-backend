# CORE.md

프로젝트 핵심 설계 원칙  
이 문서는 팀 합의된 절대 규칙이며, 변경 시 전체 동의가 필요합니다.

---

## 🎯 프로젝트 한 문장 정의

"비즈니스 로직은 Entity에 두고,  
Service는 Repository를 자유롭게 사용하되,  
TDD로 안전하게 개발한다."

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

**왜 중요한가?**

- 비즈니스 규칙이 한 곳에 모임
- 테스트가 쉬움 (Spring 없이 가능)
- 중복 코드 방지
- 변경이 안전함

---

### 2. Service는 흐름 조합만

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

// ✅ 허용: Entity 메서드 호출만
@Service
public class OrderService {
    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        // 1. 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원 없음"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품 없음"));

        // 2. 검증 (Entity 메서드)
        member.validateActive();

        // 3. 상태 변경 (Entity 메서드)
        product.decreaseStock(quantity);

        // 4. 생성 (Entity 메서드)
        Order order = Order.create(memberId, productId, quantity, product.getPrice());

        // 5. 저장
        return orderRepository.save(order).getId();
    }
}
```

**Service의 역할**

- Repository 조회
- Entity 메서드 호출
- 트랜잭션 경계 설정
- 예외 처리

**Service가 하면 안 되는 것**

- if/else 비즈니스 분기
- 상태 직접 변경 (setter)
- 계산 로직

---

### 3. Setter 금지, 의미 있는 메서드 사용

```java
// ❌ 금지: Setter 사용
order.setStatus(OrderStatus.CANCELLED);
product.setStock(product.getStock() - quantity);

// ✅ 허용: 의미 있는 메서드
order.cancel();
product.decreaseStock(quantity);
```

**왜 Setter를 금지하나?**

- 비즈니스 의도가 불명확
- 검증 로직 누락 위험
- 어디서든 변경 가능 (캡슐화 깨짐)

---

## 🏗️ 레이어 구조

```
Presentation (Controller) - HTTP 요청/응답 처리 - DTO 변환
↓
Application (Service) - 유즈케이스 흐름 조합 - 트랜잭션 경계 - Repository 호출
↓
Domain (Entity) - 비즈니스 규칙 - 상태 변경 로직 - 검증 로직
↓
Infrastructure (Repository) - DB 저장/조회 - JPA 구현
```

### 📋 레이어 책임

| 레이어     | 책임                                       | 허용                                         | 금지                                  |
| ---------- | ------------------------------------------ | -------------------------------------------- | ------------------------------------- |
| Controller | HTTP 요청/응답, DTO 변환                   | Entity 반환, 비즈니스 로직                   | Service 흐름 처리                     |
| Service    | 유즈케이스 흐름, Repository 호출, 트랜잭션 | Repository 자유롭게 사용, Entity 메서드 호출 | if/else 비즈니스 분기, 상태 직접 변경 |
| Entity     | 비즈니스 규칙, 상태 변경, 검증             | 비즈니스 로직 메서드                         | Repository 호출                       |
| Repository | 영속성, DB 저장/조회                       | 조회, 저장                                   | 비즈니스 로직                         |

---

### 🔄 Repository 사용 규칙

- Service에서 다른 Entity의 Repository 자유롭게 사용 가능
- 조회는 자유롭게
- 상태 변경은 반드시 Entity 메서드 통해서
- Setter 직접 변경 금지

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        member.validateActive();
        product.decreaseStock(quantity);

        Order order = Order.create(memberId, productId, quantity, product.getPrice());
        return orderRepository.save(order).getId();
    }
}
```

---

### 🚫 절대 금지 사항

1. Entity Setter 사용
2. Service에 비즈니스 로직 작성
3. Controller에서 Entity 직접 반환
4. Service → Service 주입 (예외적으로 읽기 전용 조회만 허용)

---

### 🧪 테스트 전략

#### Domain Test (순수 자바)

```java
@Test
void 주문_취소_테스트() {
    Order order = Order.create(1L, 10L, 3, 1000);
    order.cancel();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}

@Test
void 재고_차감_성공() {
    Product product = Product.create("노트북", 1000, 10);
    product.decreaseStock(3);
    assertThat(product.getStock()).isEqualTo(7);
}
```

#### Integration Test (@SpringBootTest)

```java
@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired MemberRepository memberRepository;
    @Autowired ProductRepository productRepository;

    @Test
    void 주문_생성_성공() {
        Member member = memberRepository.save(Member.create("user", "email"));
        Product product = productRepository.save(Product.create("상품", 1000, 10));

        Long orderId = orderService.createOrder(member.getId(), product.getId(), 3);
        assertThat(orderId).isNotNull();

        Product updated = productRepository.findById(product.getId()).get();
        assertThat(updated.getStock()).isEqualTo(7);
    }
}
```

---

## 💡 핵심 가치

1. 비즈니스 로직이 Entity에 잘 모여 있는가?
2. TDD로 안전하게 개발하는가?
3. JPA를 제대로 이해하고 사용하는가?

---

## 📖 학습 로드맵

### Week 1-2: 기본

- Entity 작성 규칙 익히기
- Domain Test 작성
- JPA 기본 개념

### Week 3-4: 실습

- Service 작성
- Integration Test 작성
- 트랜잭션 이해

### Week 5-6: 심화

- QueryDSL 사용
- 동시성 제어 (Lock)
- 성능 최적화

**한 줄 요약:**
"비즈니스 로직은 Entity에, 흐름 조합은 Service에, 안전함은 TDD로."
