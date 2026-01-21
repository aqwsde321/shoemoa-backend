# 코드 작성 템플릿

## 1️⃣ Entity 작성

### 기본 템플릿

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long memberId;  // ID 참조
    private Long productId;
    private int quantity;
    private int totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private LocalDateTime createdAt;
    
    // 정적 팩토리 메서드
    public static Order create(Long memberId, Long productId, int quantity, int price) {
        return new Order(memberId, productId, quantity, price);
    }
    
    // private 생성자
    private Order(Long memberId, Long productId, int quantity, int price) {
        validateQuantity(quantity);
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = quantity * price;
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }
    
    // 비즈니스 메서드
    public void cancel() {
        if (!this.status.isCancelable()) {
            throw new IllegalStateException("취소할 수 없는 상태입니다");
        }
        this.status = OrderStatus.CANCELLED;
    }
    
    // 검증 로직
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        }
    }
}
```

### 핵심 규칙

- **Setter 금지** - 의미 있는 메서드 사용
- **생성자는 private** - 정적 팩토리 메서드 사용
- **비즈니스 로직 포함** - validate, calculate 등

---

## 2️⃣ Service 작성

### 기본 템플릿

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    
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
        
        // 4. 생성
        Order order = Order.create(memberId, productId, quantity, product.getPrice());
        
        // 5. 저장
        return orderRepository.save(order).getId();
    }
    
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문 없음"));
        
        order.cancel();  // Entity 메서드
        
        Product product = productRepository.findById(order.getProductId())
            .orElseThrow();
        product.increaseStock(order.getQuantity());  // Entity 메서드
    }
    
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문 없음"));
    }
}
```

### 핵심 규칙

- **Repository 자유롭게 사용** - 필요한 모든 Repository 주입
- **Entity 메서드만 호출** - 비즈니스 로직 작성 금지
- **@Transactional** - 쓰기는 필수, 읽기는 readOnly = true

---

## 3️⃣ Repository 작성

### 기본 Repository

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByMemberId(Long memberId);
    
    List<Order> findByStatus(OrderStatus status);
}
```

### QueryDSL (복잡한 조회)

```java
public interface OrderDslRepository {
    List<Order> searchOrders(OrderSearchCondition condition);
}

@Repository
@RequiredArgsConstructor
public class OrderDslRepositoryImpl implements OrderDslRepository {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<Order> searchOrders(OrderSearchCondition condition) {
        return queryFactory
            .selectFrom(order)
            .where(
                memberIdEq(condition.getMemberId()),
                statusEq(condition.getStatus())
            )
            .orderBy(order.createdAt.desc())
            .fetch();
    }
    
    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? order.memberId.eq(memberId) : null;
    }
    
    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }
}
```

---

## 4️⃣ Controller 작성

### 기본 템플릿

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
        @RequestBody @Valid OrderCreateRequest request
    ) {
        Long orderId = orderService.createOrder(
            request.memberId(),
            request.productId(),
            request.quantity()
        );
        
        return ResponseEntity.ok(new OrderCreateResponse(orderId));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
    
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
```

### Request DTO

```java
public record OrderCreateRequest(
    @NotNull(message = "회원 ID는 필수입니다")
    Long memberId,
    
    @NotNull(message = "상품 ID는 필수입니다")
    Long productId,
    
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    Integer quantity
) {}
```

### Response DTO

```java
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

## 5️⃣ 예외 처리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalStateException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }
}

public record ErrorResponse(String code, String message) {}
```

---

## 6️⃣ 연관관계 규칙

### ID 참조 방식 (권장)

```java
// ✅ 권장
@Entity
public class Order {
    private Long memberId;
    private Long productId;
}
```

### Entity 참조 방식 (부모-자식만)

```java
// ✅ 같은 Aggregate 내에서만 허용
@Entity
public class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}

@Entity
public class OrderItem {
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;
    
    private Long productId;  // 다른 Aggregate는 ID 참조
}
```

---

## 📋 빠른 참조

### 자주 쓰는 어노테이션

```java
// Entity
@Entity
@Table(name = "orders")
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "member_id")
@Enumerated(EnumType.STRING)  // ORDINAL 금지

// Lombok
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor

// Service
@Service
@Transactional
@Transactional(readOnly = true)

// Controller
@RestController
@RequestMapping
@GetMapping
@PostMapping
@PathVariable
@RequestBody
@Valid
```

---

## 한 줄 요약

**"Entity → Service → Controller 순서로 작성, 각 템플릿 복사해서 사용."**