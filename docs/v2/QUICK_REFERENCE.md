# QUICK_REFERENCE.md

## 빠른 참조 가이드

**코드 작성 중 빠르게 확인하는 체크리스트와 필수 FAQ**

---

## ✅ PR 전 필수 체크리스트

### Entity
```
[ ] public Setter 없음
[ ] 기본 생성자가 protected
[ ] 비즈니스 로직이 메서드로 구현됨
[ ] 다른 Aggregate Entity를 필드로 참조 안 함
[ ] 정적 팩토리 메서드 사용
```

### Service
```
[ ] if/else 비즈니스 분기 없음
[ ] 다른 Service 주입 안 함
[ ] 다른 Aggregate Repository 직접 주입 안 함
[ ] Port 인터페이스 사용
[ ] @Transactional이 메서드에 있음
```

### Port
```
[ ] 네이밍이 역할 표현 (~Validator, ~Manager, ~Reader, ~Gateway)
[ ] Entity 반환 안 함 (DTO 또는 원시값만)
[ ] Infrastructure 패키지에 구현체 있음
[ ] application/port 패키지에 인터페이스 있음
```

### Controller
```
[ ] Entity 직접 반환 안 함
[ ] Request/Response DTO 사용
[ ] 비즈니스 로직 없음
```

### Test
```
[ ] Domain Test는 순수 자바 (Spring/JPA 없음)
[ ] UseCase Test는 Fake Port 사용
[ ] Controller Test는 @SpringBootTest
[ ] 핵심 비즈니스 규칙에 Domain Test 있음
```

---

## ❓ FAQ Top 10

### Q1. Service에서 다른 도메인 Repository 주입?
**A. ❌ 안 됩니다. Port 사용**

```java
// ❌ 금지
@Service
public class OrderService {
    private final MemberRepository memberRepository;  // 다른 Aggregate
}

// ✅ 허용
@Service
public class OrderService {
    private final MemberValidator memberValidator;  // Port
}
```

---

### Q2. Service가 다른 Service 주입?
**A. ❌ 절대 안 됩니다. Port 사용**

```java
// ❌ 금지
private final ProductService productService;

// ✅ 허용
private final StockManager stockManager;  // Port
```

**이유**: 순환 참조, 트랜잭션 경계 모호, Service 비대화

---

### Q3. Port는 언제 만드나요?
**A. 이 3가지 경우만**

1. **다른 Aggregate 검증**: `MemberValidator`
2. **다른 Aggregate 상태 변경**: `StockManager`
3. **외부 시스템 연동**: `PaymentGateway`

**만들지 않는 경우**: 단순 존재 확인, 자기 Aggregate 로직

---

### Q4. Entity Setter 금지?
**A. ✅ 네, 절대 안 됩니다**

```java
// ❌ 금지
order.setStatus(OrderStatus.CANCELLED);

// ✅ 허용
order.cancel();
```

---

### Q5. Controller에서 Entity 반환?
**A. ❌ 안 됩니다. DTO 사용**

```java
// ❌ 금지
public Order getOrder(@PathVariable Long id) {
    return orderService.getOrder(id);
}

// ✅ 허용
public OrderResponse getOrder(@PathVariable Long id) {
    Order order = orderService.getOrder(id);
    return OrderResponse.from(order);
}
```

---

### Q6. Port가 Entity 반환?
**A. ❌ 안 됩니다. DTO/원시값만**

```java
// ❌ 금지
Product findById(Long id);

// ✅ 허용
ProductInfo getInfo(Long id);
boolean hasStock(Long productId, int quantity);
```

---

### Q7. Port 구현체에 @Transactional?
**A. ❌ 안 됩니다. Service에만**

Service의 `@Transactional`이 Port까지 전파되므로 불필요

---

### Q8. Domain Test에 Spring?
**A. ❌ 순수 자바만**

```java
// ✅ 허용
@Test
void 주문_취소() {
    Order order = Order.create(1L, 10L, 3, 1000);
    order.cancel();
    assertThat(order.getStatus()).isEqualTo(CANCELLED);
}
```

---

### Q9. UseCase Test 필수?
**A. ✅ Port 사용한다면 필수**

Fake Port로 유즈케이스 시나리오 검증

---

### Q10. 다른 Aggregate 수정 방법?
**A. Port의 Manager 사용**

```java
// Port
public interface StockManager {
    void decrease(Long productId, int quantity);
}

// Service
@Transactional
public Long createOrder(...) {
    stockManager.decrease(productId, quantity);
    // ...
}
```

---

## 🎯 Port 네이밍 규칙

| 역할 | 네이밍 | 예시 | 메서드 |
|------|--------|------|--------|
| 검증 | ~Validator | MemberValidator | validateActive(Long) |
| 상태 변경 | ~Manager | StockManager | decrease(Long, int) |
| 조회 | ~Reader | ProductReader | getInfo(Long) |
| 외부 연동 | ~Gateway | PaymentGateway | pay(Request) |

---

## 🚨 흔한 실수 Top 3

### 1. Service에 비즈니스 로직
```java
// ❌
if (quantity > 10) throw new Exception();

// ✅
Order order = Order.create(...);  // Domain에서 검증
```

### 2. Port가 Entity 반환
```java
// ❌
Product findById(Long id);

// ✅
boolean hasStock(Long productId, int quantity);
```

### 3. Controller가 Entity 반환
```java
// ❌
return orderService.get(id);

// ✅
return OrderResponse.from(orderService.get(id));
```

---

## 📌 용어 빠른 찾기

모르는 용어는 **GLOSSARY.md** 참조

| 용어 | 한 줄 정의 |
|------|-----------|
| Aggregate | 일관성 경계를 가진 Entity 묶음 |
| Port | Aggregate 간 협력 추상화 인터페이스 |
| UseCase | 사용자 관점의 기능 단위 |
| Fake | 테스트용 실제 동작 구현체 |
| Domain Test | 순수 자바 비즈니스 로직 테스트 |

---

## 🆘 막혔을 때

| 상황 | 문서 |
|------|------|
| 용어 모름 | GLOSSARY.md |
| Entity 작성법 | STRUCTURE.md § 3 |
| Service 작성법 | STRUCTURE.md § 4 |
| Port 설계 | STRUCTURE.md § 5 |
| 테스트 작성 | TESTING.md |
| 전체 예시 | Sample_code.md |
| 왜 이렇게? | CORE.md |

---

## 한 줄 요약

> **"PR 전엔 체크리스트,  
> 막히면 FAQ,  
> 용어는 GLOSSARY."**

---