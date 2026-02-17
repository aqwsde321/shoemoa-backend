# TESTING.md

## 테스트 전략

본 프로젝트의 테스트 전략은 다음 원칙을 따른다:

- **Mock 라이브러리 사용 금지**
- **테스트 목적에 따라 계층별로 분리**
- **통합 테스트와 성능 테스트는 같은 API 호출**
- **테스트 코드 중복 작성 금지**

---

## 1. 테스트 전략 개요

테스트는 아래 4가지로 구분한다:

| 테스트 종류 | 목적 | 기술 | Mock |
|---|---|---|---|
| **[Domain Test](GLOSSARY.md#domain-test-도메인-테스트)** | 비즈니스 규칙 검증 | 순수 Java | ❌ |
| **[UseCase Test](GLOSSARY.md#usecase-test-유즈케이스-테스트)** | 유즈케이스 흐름 검증 | [Fake Port](GLOSSARY.md#fake-페이크) | ❌ |
| **[Controller Test](GLOSSARY.md#controller-test-컨트롤러-테스트)** | API 통합 검증 | @SpringBootTest | ❌ |
| **성능 테스트** | 부하/동시성 검증 | k6 | ❌ |

---

## 2. [Domain Test (순수 자바)](GLOSSARY.md#domain-test-도메인-테스트)

### 2.1 목적

- **비즈니스 규칙이 올바른지 검증**
- JPA, Spring, DB와 **완전히 분리**
- 가장 빠르고 **가장 많이 작성**되는 테스트

---

### 2.2 작성 규칙

**절대 금지**:

- ❌ `@SpringBootTest`, `@DataJpaTest` 사용
- ❌ Repository, EntityManager 사용
- ❌ Mock 라이브러리

**허용**:

- ✅ `new` 생성자로 직접 객체 생성
- ✅ 순수 Java 테스트
- ✅ JUnit, AssertJ만 사용

---

### 2.3 예시

```java
package com.shop.domain.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    @Test
    void 주문_금액은_상품가격과_수량의_곱이다() {
        // given
        int price = 1000;
        int quantity = 3;

        // when
        Order order = Order.create(1L, 10L, quantity, price);

        // then
        assertThat(order.getTotalAmount()).isEqualTo(3000);
    }

    @Test
    void 수량이_0이하면_주문할_수_없다() {
        // when & then
        assertThatThrownBy(() ->
            Order.create(1L, 10L, 0, 1000)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수량은 1 이상");
    }

    @Test
    void 주문은_생성_상태에서만_취소할_수_있다() {
        // given
        Order order = Order.create(1L, 10L, 3, 1000);

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void 취소된_주문은_다시_취소할_수_없다() {
        // given
        Order order = Order.create(1L, 10L, 3, 1000);
        order.cancel();

        // when & then
        assertThatThrownBy(() -> order.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("취소 불가");
    }
}
```

---

### 2.4 Domain Test 체크리스트

```
[ ] @SpringBootTest 사용 안 함
[ ] Repository 사용 안 함
[ ] new 생성자로 객체 생성
[ ] 비즈니스 규칙만 검증
[ ] 실행 속도 1초 이내
```

---

## 3. [UseCase Test](GLOSSARY.md#usecase-test-유즈케이스-테스트) ([Fake Port](GLOSSARY.md#fake-페이크))

### 3.1 목적

- **Service 흐름 검증**
- **Port 협력 검증**
- **비즈니스 시나리오 검증**
- Spring/JPA 없이 빠른 테스트

---

### 3.2 작성 규칙

**사용 기술**:

- ✅ Fake Port 구현체
- ✅ InMemory Repository
- ✅ 순수 Java

**금지**:

- ❌ @SpringBootTest
- ❌ Mock 라이브러리 (Mockito 등)
- ❌ 실제 DB

---

### 3.3 Fake Port 구현 예시

```java
// Fake MemberValidator
class FakeMemberValidator implements MemberValidator {

    private final Map<Long, Boolean> activeStatus = new HashMap<>();

    public void setActive(Long memberId, boolean active) {
        activeStatus.put(memberId, active);
    }

    @Override
    public void validateActive(Long memberId) {
        if (!activeStatus.getOrDefault(memberId, true)) {
            throw new IllegalStateException("비활성 회원입니다.");
        }
    }
}

// Fake StockManager
class FakeStockManager implements StockManager {

    private final Map<Long, Integer> stocks = new HashMap<>();

    public void setStock(Long productId, int stock) {
        stocks.put(productId, stock);
    }

    public int getStock(Long productId) {
        return stocks.getOrDefault(productId, 0);
    }

    @Override
    public void decrease(Long productId, int quantity) {
        int current = getStock(productId);
        if (current < quantity) {
            throw new IllegalStateException("재고 부족");
        }
        stocks.put(productId, current - quantity);
    }

    @Override
    public void increase(Long productId, int quantity) {
        int current = getStock(productId);
        stocks.put(productId, current + quantity);
    }
}

// InMemory OrderRepository
class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> store = new HashMap<>();
    private Long sequence = 1L;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            // Reflection으로 ID 주입 (테스트용)
            setId(order, sequence++);
        }
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    private void setId(Order order, Long id) {
        try {
            Field field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 나머지 메서드는 UnsupportedOperationException
    @Override
    public List<Order> findAll() {
        throw new UnsupportedOperationException();
    }
}
```

---

### 3.4 UseCase Test 예시

```java
package com.shop.application.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
            orderRepository,
            memberValidator,
            stockManager
        );
    }

    @Test
    void 주문_생성_성공() {
        // given
        Long memberId = 1L;
        Long productId = 10L;
        int quantity = 3;

        memberValidator.setActive(memberId, true);
        stockManager.setStock(productId, 10);

        // when
        Long orderId = orderService.createOrder(memberId, productId, quantity);

        // then
        Order order = orderRepository.findById(orderId).get();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(stockManager.getStock(productId)).isEqualTo(7);
    }

    @Test
    void 비활성_회원은_주문할_수_없다() {
        // given
        Long memberId = 1L;
        memberValidator.setActive(memberId, false);

        // when & then
        assertThatThrownBy(() ->
            orderService.createOrder(memberId, 10L, 3)
        ).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("비활성 회원");
    }

    @Test
    void 재고_부족_시_주문_실패() {
        // given
        Long productId = 10L;
        stockManager.setStock(productId, 2);

        // when & then
        assertThatThrownBy(() ->
            orderService.createOrder(1L, productId, 3)
        ).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("재고 부족");
    }

    @Test
    void 주문_취소_성공() {
        // given
        Long orderId = orderService.createOrder(1L, 10L, 3);

        // when
        orderService.cancelOrder(orderId);

        // then
        Order order = orderRepository.findById(orderId).get();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void 주문_취소_시_재고_복구() {
        // given
        Long productId = 10L;
        stockManager.setStock(productId, 10);
        Long orderId = orderService.createOrder(1L, productId, 3);

        // when
        orderService.cancelOrder(orderId);

        // then
        assertThat(stockManager.getStock(productId)).isEqualTo(10);
    }
}
```

---

### 3.5 UseCase Test 체크리스트

```
[ ] Fake Port 구현체 사용
[ ] InMemory Repository 사용
[ ] @SpringBootTest 없음
[ ] Mock 라이브러리 없음
[ ] 유즈케이스 시나리오 검증
[ ] Port 협력 동작 확인
```

---

### 3.6 [Fake vs Mock 차이](GLOSSARY.md#fake-vs-mock)

| 구분   | Fake                      | Mock (Mockito)         |
| ------ | ------------------------- | ---------------------- |
| 구현   | 실제 동작하는 간단한 구현 | 라이브러리로 행위 정의 |
| 상태   | 내부 상태 유지            | 상태 없음              |
| 검증   | 실제 동작 검증            | 호출 여부만 검증       |
| 재사용 | 여러 테스트에서 재사용    | 테스트마다 재정의      |

**우리 프로젝트는 Fake 사용** (Mock 금지)

---

## 4. [Controller 통합 테스트](GLOSSARY.md#controller-test-컨트롤러-테스트)

### 4.1 핵심 원칙

**UseCase 단독 통합 테스트는 만들지 않는다.**

**이유**:

1. 실제 사용자는 항상 HTTP API 호출
2. k6 성능 테스트도 Controller 호출
3. UseCase Test는 Controller Test와 중복

👉 **유즈케이스 흐름 검증은 Controller 통합 테스트가 담당**

---

### 4.2 목적

- **요청 → 응답 전체 흐름 검증**
- **트랜잭션 경계 검증**
- **Domain + Repository + Service 조합 검증**
- **실사용 시나리오 검증**

---

### 4.3 사용 기술

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
```

---

### 4.4 예시

```java
package com.shop.presentation.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    private Long memberId;
    private Long productId;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        Member member = memberRepository.save(
            Member.create("user1", "user1@test.com")
        );
        memberId = member.getId();

        Product product = productRepository.save(
            Product.create("상품1", 1000, 100)
        );
        productId = product.getId();
    }

    @Test
    void 주문_생성_성공() throws Exception {
        // given
        String requestBody = """
            {
              "memberId": %d,
              "productId": %d,
              "quantity": 3
            }
        """.formatted(memberId, productId);

        // when & then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void 비활성_회원은_주문_불가() throws Exception {
        // given
        Member inactiveMember = memberRepository.save(
            Member.create("inactive", "inactive@test.com")
        );
        inactiveMember.deactivate();
        memberRepository.save(inactiveMember);

        String requestBody = """
            {
              "memberId": %d,
              "productId": %d,
              "quantity": 3
            }
        """.formatted(inactiveMember.getId(), productId);

        // when & then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("비활성 회원입니다."));
    }

    @Test
    void 재고_부족_시_주문_실패() throws Exception {
        // given
        Product lowStockProduct = productRepository.save(
            Product.create("품절임박상품", 1000, 2)
        );

        String requestBody = """
            {
              "memberId": %d,
              "productId": %d,
              "quantity": 5
            }
        """.formatted(memberId, lowStockProduct.getId());

        // when & then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("재고가 부족합니다."));
    }

    @Test
    void 주문_취소_성공() throws Exception {
        // given - 주문 생성
        String createRequest = """
            {
              "memberId": %d,
              "productId": %d,
              "quantity": 3
            }
        """.formatted(memberId, productId);

        String response = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long orderId = extractOrderId(response);

        // when & then - 주문 취소
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
            .andExpect(status().isOk());

        // 취소 확인
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void 주문_목록_조회() throws Exception {
        // given - 여러 주문 생성
        for (int i = 0; i < 3; i++) {
            String request = """
                {
                  "memberId": %d,
                  "productId": %d,
                  "quantity": 1
                }
            """.formatted(memberId, productId);

            mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
        }

        // when & then
        mockMvc.perform(get("/api/orders")
                .param("memberId", memberId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orders").isArray())
            .andExpect(jsonPath("$.orders.length()").value(3));
    }

    private Long extractOrderId(String json) {
        // JSON 파싱 로직
        return 1L;
    }
}
```

---

### 4.5 Controller Test 체크리스트

```
[ ] @SpringBootTest 사용
[ ] MockMvc 사용
[ ] 실제 DB 사용 (Testcontainers)
[ ] 전체 흐름 검증
[ ] HTTP 요청/응답 검증
[ ] 비즈니스 시나리오 검증
```

---

## 5. 성능 테스트 (k6)

### 5.1 원칙

**Controller 통합 테스트와 동일한 API 호출**.

| 구분                   | 실행 환경 | 코드       |
| ---------------------- | --------- | ---------- |
| Controller 통합 테스트 | JUnit     | Java       |
| 성능 테스트            | k6        | JavaScript |

**중요**: 같은 API, 다른 실행기

---

### 5.2 코드 중복이 아닌 이유

- Java 테스트 코드 재작성 ❌
- HTTP 요청 시나리오만 정의
- 비즈니스 로직 재작성 없음

---

### 5.3 예시 (k6)

```javascript
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 20 }, // 30초 동안 20 VU까지 증가
    { duration: "1m", target: 20 }, // 1분 동안 20 VU 유지
    { duration: "10s", target: 0 }, // 10초 동안 0으로 감소
  ],
};

export default function () {
  // 주문 생성 API 호출
  const payload = JSON.stringify({
    memberId: 1,
    productId: 10,
    quantity: 2,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  const res = http.post("http://localhost:8080/api/orders", payload, params);

  check(res, {
    "status is 200": (r) => r.status === 200,
    "response time < 500ms": (r) => r.timings.duration < 500,
  });

  sleep(1);
}
```

---

### 5.4 k6 실행

```bash
k6 run performance-test.js
```

---

## 6. 테스트 작성 순서 (권장)

### 1단계: Domain Test 작성

```
비즈니스 규칙 먼저 검증
- 상태 전이
- 유효성 검증
- 도메인 로직
```

---

### 2단계: UseCase Test 작성

```
흐름 검증
- Port 협력
- 비즈니스 시나리오
- 예외 상황
```

---

### 3단계: Controller 통합 테스트

```
전체 통합 검증
- API 엔드포인트
- 요청/응답
- 트랜잭션
```

---

### 4단계: k6 성능 테스트

```
부하 테스트
- 동시 사용자
- 응답 시간
- 처리량
```

---

## 7. 테스트 커버리지 목표

| 계층               | 목표 커버리지 | 우선순위 |
| ------------------ | ------------- | -------- |
| **Domain**         | 100%          | 최우선   |
| **UseCase**        | 80% 이상      | 필수     |
| **Controller**     | 주요 시나리오 | 필수     |
| **Infrastructure** | 50%           | 선택     |

---

## 8. Fake 구현체 관리

### 8.1 위치

```
src/test/java
└─ com/shop/support
   └─ fake
      ├─ FakeMemberValidator.java
      ├─ FakeStockManager.java
      └─ InMemoryOrderRepository.java
```

---

### 8.2 재사용

**Fake는 여러 테스트에서 재사용한다.**

```java
// 테스트마다 새로 생성
@BeforeEach
void setUp() {
    FakeMemberValidator validator = new FakeMemberValidator();
    // ...
}
```

---

### 8.3 Fake 작성 가이드

1. **인터페이스 완전 구현**

   - 사용하지 않는 메서드는 `UnsupportedOperationException`

2. **상태 유지**

   - Map, List 등으로 내부 상태 관리

3. **테스트 편의 메서드 제공**

```java
   public void setActive(Long memberId, boolean active) {
       // 테스트에서 상태 설정용
   }
```

---

## 9. 테스트 전략 한 줄 요약

> **"비즈니스는 Domain에서 검증하고,  
> 흐름은 UseCase에서 검증하고,  
> 통합은 Controller에서 검증하며,  
> 성능은 같은 API를 k6로 검증한다."**

---

## 10. 테스트 Anti-Pattern

### ❌ 하지 말아야 할 것

1. **Mock 라이브러리 사용**

```java
   // ❌ 금지
   @Mock
   MemberValidator memberValidator;
```

2. **Domain Test에 Spring 사용**

```java
   // ❌ 금지
   @SpringBootTest
   class OrderTest { }
```

3. **UseCase와 Controller Test 중복**

```java
   // ❌ 불필요
   @SpringBootTest
   class OrderServiceTest { }  // UseCase는 Fake로 충분
```

4. **테스트에서 비즈니스 로직 재작성**

```java
   // ❌ 금지
   @Test
   void test() {
       int expected = quantity * price;  // 비즈니스 로직 재작성
       assertThat(order.getTotal()).isEqualTo(expected);
   }

   // ✅ 허용
   @Test
   void test() {
       assertThat(order.getTotal()).isEqualTo(3000);
   }
```

---

## 부록: InMemory Repository 템플릿

```java
class InMemoryRepository<T, ID> {

    protected final Map<ID, T> store = new HashMap<>();
    protected Long sequence = 1L;

    public T save(T entity) {
        if (getId(entity) == null) {
            setId(entity, (ID) sequence++);
        }
        store.put(getId(entity), entity);
        return entity;
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    public void deleteById(ID id) {
        store.remove(id);
    }

    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    protected abstract ID getId(T entity);
    protected abstract void setId(T entity, ID id);
}
```

---
