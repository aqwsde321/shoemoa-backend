# Domain 작성 규칙

이 문서는 본 프로젝트에서 **Domain(Entity) 작성 시 반드시 지켜야 할 규칙**을 정의한다.

본 프로젝트는 다음 전제를 기반으로 한다.

- JPA Entity = Domain Model
- DDD의 모든 패턴을 적용하지 않되, **핵심 규율은 강제**한다
- Service 비대화를 막고, 비즈니스 규칙을 Domain에 응집시킨다

---

## 1. Domain의 책임 범위

### 1.1 Domain이 반드시 가져야 할 것

- 비즈니스 규칙
- 상태 전이 로직
- 불변식(invariant) 검증
- 자기 자신의 유효성 보장

```java
order.cancel();
order.complete();
order.validateCancelable();
```

### 1.2 Domain이 가져서는 안 되는 것

- Repository 접근
- 다른 Aggregate Entity 직접 참조
- 외부 시스템 접근 (API, 메시지, 파일 등)
- Application / Infrastructure 객체 의존

---

## 2. Entity 설계 원칙

### 2.1 Setter 금지

- 모든 Entity는 **무분별한 Setter 사용을 금지**한다
- 상태 변경은 의미 있는 메서드로만 허용한다

```java
// ❌ 금지
order.setStatus(OrderStatus.CANCELLED);

// ✅ 허용
order.cancel();
```

---

### 2.2 생성 규칙

- 기본 생성자는 JPA 용도로만 사용 (`protected`)
- 외부에서는 정적 팩토리 또는 명시적 생성자만 사용

```java
protected Order() {}

public static Order create(Long memberId, Long productId, int quantity) {
    return new Order(memberId, productId, quantity);
}
```

---

## 3. Aggregate 간 관계 규칙

### 3.1 Aggregate 직접 참조 금지

- 다른 Aggregate의 Entity를 필드로 참조하지 않는다

```java
// ❌ 금지
@ManyToOne
private Member member;
```

### 3.2 허용되는 협력 방식

- ID 기반 참조
- Application Port를 통한 검증/조회

```java
private Long memberId;
private Long productId;
```

---

## 4. Domain Method 설계 규칙

### 4.1 Domain 메서드 인자 규칙

Domain 메서드는 **자기 Aggregate의 정보만 받아야 한다**.

#### 허용

- Primitive / Wrapper
- Value Object
- ID 값

```java
order.changeQuantity(3);
order.assignProduct(productId);
```

#### 금지

- 다른 Aggregate Entity 전달
- DTO 전달
- Repository 전달

```java
// ❌ 금지
order.place(member, product);
```

---

### 4.2 검증 책임

- **상태 기반 검증은 Domain이 담당**
- 외부 존재 여부 검증은 Application(Service)가 담당

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

### 4.3 반환값 규칙

- Domain 메서드는 가능하면 `void`
- 성공/실패는 예외로 표현

```java
order.cancel(); // 성공
```

---

## 5. Value Object 사용 기준

다음 조건 중 2개 이상 해당되면 Value Object로 분리한다.

- 의미 있는 개념 이름이 존재함
- 유효성 검증 로직이 필요함
- 여러 Entity에서 재사용됨

```java
@Embeddable
public class Money {
    private BigDecimal amount;
}
```

---

## 6. Domain 테스트 규칙

- Domain 테스트는 **Spring 없이 순수 자바 테스트**로 작성
- Repository / Mock 사용 금지
- 상태 변화와 예외만 검증

```java
@Test
void 주문은_취소될_수_있다() {
    Order order = Order.create(1L, 2L, 3);
    order.cancel();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}
```

---

## 7. Domain과 Service의 경계 요약

| 구분          | Domain | Service |
| ------------- | ------ | ------- |
| 비즈니스 규칙 | ✅     | ❌      |
| 상태 변경     | ✅     | ❌      |
| 외부 검증     | ❌     | ✅      |
| 트랜잭션      | ❌     | ✅      |

---

## 8. 한 줄 요약

> **Domain은 스스로의 규칙과 상태를 책임지는 핵심 모델이며, 외부 의존 없이 독립적으로 완결되어야 한다.**
