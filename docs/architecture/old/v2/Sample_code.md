# Sample Code

## 전체 구조 코드 예시

이 문서는 **Order Aggregate를 중심으로 한 완전한 코드 예시**를 제공한다.  
이 예시 하나만 봐도 프로젝트 전체 구조를 이해할 수 있다.

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
│  └─ order
│     ├─ OrderService.java
│     └─ port
│        ├─ MemberValidator.java
│        ├─ ProductValidator.java
│        └─ StockManager.java
│
├─ infrastructure
│  ├─ member
│  │  └─ JpaMemberValidator.java
│  ├─ product
│  │  ├─ JpaProductValidator.java
│  │  └─ JpaStockManager.java
│  └─ order
│     ├─ OrderDslRepository.java
│     └─ OrderDslRepositoryImpl.java
│
└─ presentation
   └─ order
      ├─ OrderController.java
      ├─ request
      │  ├─ OrderCreateRequest.java
      │  └─ OrderCancelRequest.java
      └─ response
         ├─ OrderCreateResponse.java
         └─ OrderResponse.java
```

---

## 1️⃣ Domain Layer

### 1.1 Order Entity (Aggregate Root)

```java
package com.shop.domain.order;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private Long productId;
    private int quantity;
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;

    // JPA용 기본 생성자
    protected Order() {}

    // 정적 팩토리 메서드
    public static Order create(Long memberId, Long productId, int quantity, int price) {
        return new Order(memberId, productId, quantity, price);
    }

    // private 생성자 (검증 포함)
    private Order(Long memberId, Long productId, int quantity, int price) {
        validateQuantity(quantity);
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = calculateTotalAmount(quantity, price);
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 주문 취소
    public void cancel() {
        if (!this.status.isCancelable()) {
            throw new IllegalStateException("취소할 수 없는 상태입니다: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    // 비즈니스 로직: 주문 확정
    public void confirm() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("확정할 수 없는 상태입니다: " + this.status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    // 도메인 규칙
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        if (quantity > 100) {
            throw new IllegalArgumentException("한 번에 100개 이상 주문할 수 없습니다.");
        }
    }

    private int calculateTotalAmount(int quantity, int price) {
        return quantity * price;
    }

    // Getter만 노출 (Setter 없음)
    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
```

---

### 1.2 OrderStatus Enum

```java
package com.shop.domain.order;

public enum OrderStatus {
    CREATED("생성"),
    CONFIRMED("확정"),
    CANCELLED("취소"),
    COMPLETED("완료");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public boolean isCancelable() {
        return this == CREATED;
    }

    public String getDescription() {
        return description;
    }
}
```

---

### 1.3 OrderRepository

```java
package com.shop.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 기본 CRUD는 JpaRepository가 제공
}
```

---

### 1.4 Member Entity

```java
package com.shop.domain.member;

import jakarta.persistence.*;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private boolean active;

    protected Member() {}

    public static Member create(String username, String email) {
        return new Member(username, email);
    }

    private Member(String username, String email) {
        validateUsername(username);
        validateEmail(email);
        this.username = username;
        this.email = email;
        this.active = true;
    }

    // 비즈니스 로직
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수입니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }
}
```

---

### 1.5 Product Entity

```java
package com.shop.domain.product;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int price;
    private int stock;

    protected Product() {}

    public static Product create(String name, int price, int stock) {
        return new Product(name, price, stock);
    }

    private Product(String name, int price, int stock) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // 비즈니스 로직: 재고 차감
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + this.stock);
        }
        this.stock -= quantity;
    }

    // 비즈니스 로직: 재고 증가
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
    }

    private void validatePrice(int price) {
        if (price < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }
    }

    private void validateStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }
}
```

---

## 2️⃣ Application Layer

### 2.1 Port Interfaces

#### MemberValidator

```java
package com.shop.application.order.port;

/**
 * 회원 검증 Port
 * 다른 Aggregate(Member)의 상태를 읽기 전용으로 검증
 */
public interface MemberValidator {

    /**
     * 회원이 활성화 상태인지 검증
     * @throws IllegalStateException 비활성 회원인 경우
     * @throws EntityNotFoundException 회원이 존재하지 않는 경우
     */
    void validateActive(Long memberId);
}
```

#### ProductValidator

```java
package com.shop.application.order.port;

/**
 * 상품 검증 Port
 */
public interface ProductValidator {

    /**
     * 상품이 주문 가능한 상태인지 검증
     */
    void validateAvailable(Long productId);

    /**
     * 상품 재고가 충분한지 검증
     */
    void validateStock(Long productId, int quantity);
}
```

#### StockManager

```java
package com.shop.application.order.port;

/**
 * 재고 관리 Port
 * 다른 Aggregate(Product)의 상태를 변경
 */
public interface StockManager {

    /**
     * 재고 차감
     * @throws IllegalStateException 재고 부족 시
     */
    void decrease(Long productId, int quantity);

    /**
     * 재고 복구
     */
    void increase(Long productId, int quantity);
}
```

---

### 2.2 OrderService (Application Service)

```java
package com.shop.application.order;

import com.shop.application.order.port.MemberValidator;
import com.shop.application.order.port.ProductValidator;
import com.shop.application.order.port.StockManager;
import com.shop.domain.order.Order;
import com.shop.domain.order.OrderRepository;
import com.shop.domain.product.Product;
import com.shop.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // Port 의존성
    private final MemberValidator memberValidator;
    private final ProductValidator productValidator;
    private final StockManager stockManager;

    public OrderService(
        OrderRepository orderRepository,
        ProductRepository productRepository,
        MemberValidator memberValidator,
        ProductValidator productValidator,
        StockManager stockManager
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.memberValidator = memberValidator;
        this.productValidator = productValidator;
        this.stockManager = stockManager;
    }

    /**
     * 주문 생성
     * 1. 회원 검증 (Port)
     * 2. 상품 검증 (Port)
     * 3. 재고 차감 (Port)
     * 4. 주문 생성 (Domain)
     * 5. 저장 (Repository)
     */
    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity) {
        // 1. Port를 통한 회원 검증
        memberValidator.validateActive(memberId);

        // 2. Port를 통한 상품 검증
        productValidator.validateAvailable(productId);
        productValidator.validateStock(productId, quantity);

        // 3. 상품 가격 조회 (자기 Aggregate가 아니지만 읽기만 함)
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        // 4. Port를 통한 재고 차감
        stockManager.decrease(productId, quantity);

        // 5. Domain 생성
        Order order = Order.create(memberId, productId, quantity, product.getPrice());

        // 6. 저장
        Order savedOrder = orderRepository.save(order);

        return savedOrder.getId();
    }

    /**
     * 주문 취소
     * 1. 주문 조회
     * 2. 취소 처리 (Domain)
     * 3. 재고 복구 (Port)
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        // 2. Domain 메서드로 취소
        order.cancel();

        // 3. Port를 통한 재고 복구
        stockManager.increase(order.getProductId(), order.getQuantity());

        // 4. 저장 (Dirty Checking으로 자동 저장되지만 명시적으로 호출)
        orderRepository.save(order);
    }

    /**
     * 주문 확정
     */
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        // Domain 메서드 호출
        order.confirm();

        orderRepository.save(order);
    }

    /**
     * 주문 조회 (읽기 전용)
     */
    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));
    }
}
```

---

## 3️⃣ Infrastructure Layer

### 3.1 Port 구현체

#### JpaMemberValidator

```java
package com.shop.infrastructure.member;

import com.shop.application.order.port.MemberValidator;
import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class JpaMemberValidator implements MemberValidator {

    private final MemberRepository memberRepository;

    public JpaMemberValidator(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void validateActive(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. ID: " + memberId));

        if (!member.isActive()) {
            throw new IllegalStateException("비활성 회원입니다. ID: " + memberId);
        }
    }
}
```

#### JpaProductValidator

```java
package com.shop.infrastructure.product;

import com.shop.application.order.port.ProductValidator;
import com.shop.domain.product.Product;
import com.shop.domain.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class JpaProductValidator implements ProductValidator {

    private final ProductRepository productRepository;

    public JpaProductValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void validateAvailable(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. ID: " + productId));

        // 추가 검증 로직 (예: 판매 중지 상품 등)
        // if (!product.isAvailable()) { ... }
    }

    @Override
    public void validateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        if (product.getStock() < quantity) {
            throw new IllegalStateException(
                String.format("재고가 부족합니다. 요청: %d, 현재: %d", quantity, product.getStock())
            );
        }
    }
}
```

#### JpaStockManager

```java
package com.shop.infrastructure.product;

import com.shop.application.order.port.StockManager;
import com.shop.domain.product.Product;
import com.shop.domain.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

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

        // Domain 메서드 호출 (비즈니스 규칙은 Domain에)
        product.decreaseStock(quantity);

        productRepository.save(product);
    }

    @Override
    public void increase(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        // Domain 메서드 호출
        product.increaseStock(quantity);

        productRepository.save(product);
    }
}
```

---

## 4️⃣ Presentation Layer

### 4.1 Request DTO

```java
package com.shop.presentation.order.request;

public record OrderCreateRequest(
    Long memberId,
    Long productId,
    int quantity
) {
    public OrderCreateRequest {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId는 필수입니다.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
    }
}
```

---

### 4.2 Response DTO

```java
package com.shop.presentation.order.response;

public record OrderCreateResponse(
    Long orderId
) {}
```

```java
package com.shop.presentation.order.response;

import com.shop.domain.order.Order;
import com.shop.domain.order.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
    Long orderId,
    Long memberId,
    Long productId,
    int quantity,
    int totalAmount,
    OrderStatus status,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getMemberId(),
            order.getProductId(),
            order.getQuantity(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
```

---

### 4.3 OrderController

```java
package com.shop.presentation.order;

import com.shop.application.order.OrderService;
import com.shop.domain.order.Order;
import com.shop.presentation.order.request.OrderCreateRequest;
import com.shop.presentation.order.response.OrderCreateResponse;
import com.shop.presentation.order.response.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 생성
     * POST /api/orders
     */
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

    /**
     * 주문 조회
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /**
     * 주문 취소
     * POST /api/orders/{orderId}/cancel
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * 주문 확정
     * POST /api/orders/{orderId}/confirm
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(@PathVariable Long orderId) {
        orderService.confirmOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
```

---

## 5️⃣ Test Layer

### 5.1 Domain Test (순수 자바)

```java
package com.shop.domain.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    @Test
    void 주문_금액은_수량과_가격의_곱이다() {
        // when
        Order order = Order.create(1L, 10L, 3, 1000);

        // then
        assertThat(order.getTotalAmount()).isEqualTo(3000);
    }

    @Test
    void 수량이_0이하면_주문할_수_없다() {
        assertThatThrownBy(() ->
            Order.create(1L, 10L, 0, 1000)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수량은 1 이상");
    }

    @Test
    void 수량이_100개를_초과하면_주문할_수_없다() {
        assertThatThrownBy(() ->
            Order.create(1L, 10L, 101, 1000)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("100개 이상");
    }

    @Test
    void 생성된_주문은_취소할_수_있다() {
        // given
        Order order = Order.create(1L, 10L, 3, 1000);

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void 확정된_주문은_취소할_수_없다() {
        // given
        Order order = Order.create(1L, 10L, 3, 1000);
        order.confirm();

        // when & then
        assertThatThrownBy(() -> order.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("취소할 수 없는 상태");
    }

    @Test
    void 생성된_주문은_확정할_수_있다() {
        // given
        Order order = Order.create(1L, 10L, 3, 1000);

        // when
        order.confirm();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
```

---

### 5.2 Fake Port 구현체

```java
package com.shop.support.fake;

import com.shop.application.order.port.MemberValidator;

import java.util.HashMap;
import java.util.Map;

public class FakeMemberValidator implements MemberValidator {

    private final Map<Long, Boolean> activeStatus = new HashMap<>();

    public void setActive(Long memberId, boolean active) {
        activeStatus.put(memberId, active);
    }

    @Override
    public void validateActive(Long memberId) {
        if (!activeStatus.getOrDefault(memberId, true)) {
            throw new IllegalStateException("비활성 회원입니다. ID: " + memberId);
        }
    }
}
```

```java
package com.shop.support.fake;

import com.shop.application.order.port.StockManager;

import java.util.HashMap;
import java.util.Map;

public class FakeStockManager implements StockManager {

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
```

---

### 5.3 UseCase Test

```java
package com.shop.application.order;

import com.shop.domain.order.Order;
import com.shop.domain.order.OrderStatus;
import com.shop.support.fake.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderServiceTest {

    private FakeMemberValidator memberValidator;
    private FakeProductValidator productValidator;
    private FakeStockManager stockManager;
    private InMemoryOrderRepository orderRepository;
    private InMemoryProductRepository productRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        memberValidator = new FakeMemberValidator();
        productValidator = new FakeProductValidator();
        stockManager = new FakeStockManager();
        orderRepository = new InMemoryOrderRepository();
        productRepository = new InMemoryProductRepository();

        orderService = new OrderService(
            orderRepository,
            productRepository,
            memberValidator,
            productValidator,
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
        productValidator.setAvailable(productId, true);
        stockManager.setStock(productId, 10);
        productRepository.save(Product.create("상품", 1000, 10));

        // when
        Long orderId = orderService.createOrder(memberId, productId, quantity);

        // then
        Order order = orderRepository.findById(orderId).get();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(3000);
        assertThat(stockManager.getStock(productId)).isEqualTo(7);
    }

    @Test
    void 비활성_회원은_주문_불가() {
        // given
        memberValidator.setActive(1L, false);

        // when & then
        assertThatThrownBy(() ->
            orderService.createOrder(1L, 10L, 3)
        ).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("비활성 회원");
    }

    @Test
    void 재고_부족_시_주문_실패() {
        // given
        Long productId = 10L;
        stockManager.setStock(productId, 2);
        productRepository.save(Product.create("상품", 1000, 2));

        // when & then
        assertThatThrownBy(() ->
            orderService.createOrder(1L, productId, 5)
        ).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("재고");
    }

    @Test
    void 주문_취소_시_재고_복구() {
        // given
        Long productId = 10L;
        stockManager.setStock(productId, 10);
        productRepository.save(Product.create("상품", 1000, 10));

        Long orderId = orderService.createOrder(1L, productId, 3);

        // when
        orderService.cancelOrder(orderId);

        // then
        assertThat(stockManager.getStock(productId)).isEqualTo(10);
        Order order = orderRepository.findById(orderId).get();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
```

---

### 5.4 Controller 통합 테스트

```java
package com.shop.presentation.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ProductRepository productRepository;

    private Long memberId;
    private Long productId;

    @BeforeEach
    void setUp() {
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
    void 주문_생성_API() throws Exception {
        // given
        String request = """
            {
            "memberId": %d,
            "productId": %d,
            "quantity": 3
            }
        """.formatted(memberId, productId);

        // when & then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void 주문_조회_API() throws Exception {
        // given - 주문 생성
        String createRequest = """
            {
            "memberId": %d,
            "productId": %d,
            "quantity": 2
            }
        """.formatted(memberId, productId);

        String response = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // orderId 추출 (간단히 하드코딩)
        Long orderId = 1L;

        // when & then
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void 주문_취소_API() throws Exception {
        // given
        String createRequest = """
            {
            "memberId": %d,
            "productId": %d,
            "quantity": 1
            }
        """.formatted(memberId, productId);

        mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createRequest));

        Long orderId = 1L;

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
            .andExpect(status().isOk());

        // 취소 확인
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
```

---

## 6️⃣ 이 예시가 "정답 구조"인 이유ㅁ

| 항목                | 충족 여부                      |
| ------------------- | ------------------------------ |
| DDD 사고            | ✅ Aggregate 경계 명확         |
| JPA 실무 친화       | ✅ Entity = Domain             |
| Domain 응집도       | ✅ 비즈니스 로직이 Entity에    |
| Service 비대화 방지 | ✅ Service는 오케스트레이션만  |
| Mock 없는 테스트    | ✅ Fake Port 사용              |
| Port를 통한 협력    | ✅ Aggregate 간 직접 참조 없음 |
| 팀 교육 난이도      | ✅ 1년차도 이해 가능           |

---

## 한 줄 요약

> **"Order 하나만 봐도 이 프로젝트 구조가 설명되면 성공이다."**

---
