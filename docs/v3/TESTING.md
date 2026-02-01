# 테스트 작성 가이드

## 🎯 테스트 전략

### 2가지 테스트만 작성

| 테스트 | 대상 | 기술 | 목적 |
|--------|------|------|------|
| Domain Test | Entity 비즈니스 로직 | 순수 자바 | 규칙 검증 |
| Integration Test | Service 전체 흐름 | Spring + DB | 통합 검증 |

---

## 1️⃣ Domain Test

### 목적

- 비즈니스 규칙 검증
- Spring, JPA, DB 없이 테스트
- 가장 빠르고 많이 작성

### 작성 규칙

```java
// ✅ 허용
- new 키워드로 객체 생성
- 순수 Java 테스트
- JUnit + AssertJ만 사용

// ❌ 금지
- @SpringBootTest
- Repository 사용
- DB 연결
```

### 예시 1: 주문 취소

```java
@Test
void 주문_취소_성공() {
    // given
    Order order = Order.create(1L, 10L, 3, 1000);
    
    // when
    order.cancel();
    
    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}

@Test
void 확정된_주문은_취소_불가() {
    // given
    Order order = Order.create(1L, 10L, 3, 1000);
    order.confirm();
    
    // when & then
    assertThatThrownBy(() -> order.cancel())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("취소할 수 없는 상태");
}
```

### 예시 2: 재고 차감

```java
@Test
void 재고_차감_성공() {
    // given
    Product product = Product.create("노트북", 1000, 10);
    
    // when
    product.decreaseStock(3);
    
    // then
    assertThat(product.getStock()).isEqualTo(7);
}

@Test
void 재고_부족_시_예외() {
    // given
    Product product = Product.create("노트북", 1000, 2);
    
    // when & then
    assertThatThrownBy(() -> product.decreaseStock(5))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("재고 부족");
}
```

### 예시 3: 회원 검증

```java
@Test
void 활성_회원_검증_성공() {
    // given
    Member member = Member.create("user", "user@test.com");
    
    // when & then
    assertThatCode(() -> member.validateActive())
        .doesNotThrowAnyException();
}

@Test
void 비활성_회원_검증_실패() {
    // given
    Member member = Member.create("user", "user@test.com");
    member.deactivate();
    
    // when & then
    assertThatThrownBy(() -> member.validateActive())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("비활성 회원");
}
```

---

## 2️⃣ Integration Test

### 목적

- Service 전체 흐름 검증
- Spring + 실제 DB 사용
- 트랜잭션, Repository 포함 검증

### 작성 규칙

```java
@SpringBootTest
@Transactional  // 각 테스트 후 자동 롤백
class OrderServiceTest {
    
    @Autowired OrderService orderService;
    @Autowired MemberRepository memberRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    
    // 테스트 작성
}
```

### 예시 1: 주문 생성

```java
@Test
void 주문_생성_성공() {
    // given
    Member member = memberRepository.save(
        Member.create("user", "user@test.com")
    );
    Product product = productRepository.save(
        Product.create("노트북", 1000, 10)
    );
    
    // when
    Long orderId = orderService.createOrder(
        member.getId(),
        product.getId(),
        3
    );
    
    // then
    assertThat(orderId).isNotNull();
    
    Order order = orderRepository.findById(orderId).get();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.getTotalAmount()).isEqualTo(3000);
    
    Product updatedProduct = productRepository.findById(product.getId()).get();
    assertThat(updatedProduct.getStock()).isEqualTo(7);
}
```

### 예시 2: 주문 취소 + 재고 복구

```java
@Test
void 주문_취소_시_재고_복구() {
    // given
    Member member = memberRepository.save(Member.create("user", "user@test.com"));
    Product product = productRepository.save(Product.create("노트북", 1000, 10));
    
    Long orderId = orderService.createOrder(member.getId(), product.getId(), 3);
    
    // when
    orderService.cancelOrder(orderId);
    
    // then
    Order order = orderRepository.findById(orderId).get();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    
    Product updatedProduct = productRepository.findById(product.getId()).get();
    assertThat(updatedProduct.getStock()).isEqualTo(10);  // 원복됨
}
```

### 예시 3: 예외 케이스

```java
@Test
void 비활성_회원은_주문_불가() {
    // given
    Member member = memberRepository.save(Member.create("user", "user@test.com"));
    member.deactivate();
    memberRepository.save(member);
    
    Product product = productRepository.save(Product.create("노트북", 1000, 10));
    
    // when & then
    assertThatThrownBy(() -> 
        orderService.createOrder(member.getId(), product.getId(), 3)
    )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("비활성 회원");
}

@Test
void 재고_부족_시_주문_실패() {
    // given
    Member member = memberRepository.save(Member.create("user", "user@test.com"));
    Product product = productRepository.save(Product.create("노트북", 1000, 2));
    
    // when & then
    assertThatThrownBy(() -> 
        orderService.createOrder(member.getId(), product.getId(), 5)
    )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("재고 부족");
}
```

---

## 3️⃣ TDD 작성 순서

### Red → Green → Refactor

```
1. Domain Test 작성 (Red)
   ↓
2. Entity 구현 (Green)
   ↓
3. Integration Test 작성 (Red)
   ↓
4. Service 구현 (Green)
   ↓
5. 리팩토링 (Refactor)
```

### 실전 예시: 주문 생성 기능

#### Step 1: Domain Test (Red)

```java
@Test
void 주문_생성_시_총액_계산() {
    // when
    Order order = Order.create(1L, 10L, 3, 1000);
    
    // then
    assertThat(order.getTotalAmount()).isEqualTo(3000);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
}
```

#### Step 2: Entity 구현 (Green)

```java
@Entity
public class Order {
    public static Order create(Long memberId, Long productId, int quantity, int price) {
        return new Order(memberId, productId, quantity, price);
    }
    
    private Order(Long memberId, Long productId, int quantity, int price) {
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = quantity * price;  // 계산 로직
        this.status = OrderStatus.CREATED;
    }
}
```

#### Step 3: Integration Test (Red)

```java
@Test
void 주문_생성_성공() {
    Member member = memberRepository.save(Member.create("user", "email"));
    Product product = productRepository.save(Product.create("상품", 1000, 10));
    
    Long orderId = orderService.createOrder(member.getId(), product.getId(), 3);
    
    assertThat(orderId).isNotNull();
}
```

#### Step 4: Service 구현 (Green)

```java
@Service
public class OrderService {
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

## 4️⃣ 테스트 팁

### AssertJ 주요 메서드

```java
// 같은지
assertThat(actual).isEqualTo(expected);

// Null 체크
assertThat(actual).isNotNull();
assertThat(actual).isNull();

// 예외 검증
assertThatThrownBy(() -> method())
    .isInstanceOf(IllegalStateException.class)
    .hasMessageContaining("메시지");

// 예외 없음
assertThatCode(() -> method())
    .doesNotThrowAnyException();

// 컬렉션
assertThat(list).hasSize(3);
assertThat(list).contains(item);
assertThat(list).isEmpty();
```

### 테스트 데이터 준비

```java
// given 절에서 테스트 데이터 생성
@Test
void test() {
    // given
    Member member = memberRepository.save(
        Member.create("user", "user@test.com")
    );
    
    Product product = productRepository.save(
        Product.create("상품", 1000, 10)
    );
    
    // when
    Long orderId = orderService.createOrder(
        member.getId(),
        product.getId(),
        3
    );
    
    // then
    assertThat(orderId).isNotNull();
}
```

### @BeforeEach 활용

```java
@SpringBootTest
@Transactional
class OrderServiceTest {
    
    @Autowired OrderService orderService;
    @Autowired MemberRepository memberRepository;
    @Autowired ProductRepository productRepository;
    
    private Member member;
    private Product product;
    
    @BeforeEach
    void setUp() {
        member = memberRepository.save(
            Member.create("user", "user@test.com")
        );
        
        product = productRepository.save(
            Product.create("노트북", 1000, 10)
        );
    }
    
    @Test
    void 주문_생성_성공() {
        // given - setUp에서 준비됨
        
        // when
        Long orderId = orderService.createOrder(
            member.getId(),
            product.getId(),
            3
        );
        
        // then
        assertThat(orderId).isNotNull();
    }
}
```

---

## 📋 체크리스트

### Domain Test

```
[ ] Spring 없이 작성
[ ] new 키워드로 객체 생성
[ ] 비즈니스 규칙만 검증
[ ] 실행 시간 1초 이내
```

### Integration Test

```
[ ] @SpringBootTest 사용
[ ] @Transactional 사용 (자동 롤백)
[ ] 실제 Repository 사용
[ ] 전체 흐름 검증
[ ] 예외 케이스 포함
```

---

## 한 줄 요약

**"비즈니스 규칙은 Domain Test로, 전체 흐름은 Integration Test로."**