# Service(Application) 작성 규칙

본 문서는 Application Service(이하 Service)의 책임, 허용 범위, 금지 사항을 명확히 정의한다.
Service는 **유즈케이스 실행기**이며, 비즈니스 규칙의 소유자가 아니다.

---

## 1. Service의 역할 정의

Service는 다음 역할만 수행한다.

- 하나의 유즈케이스를 실행한다
- 여러 도메인 객체의 흐름을 **조합(Orchestration)** 한다
- 트랜잭션 경계를 정의한다
- 외부 시스템과의 협력을 **Port** 를 통해 위임한다

> Service는 "무엇을 할지"를 결정하지 않고, **"이미 정해진 규칙을 언제 호출할지"만 결정한다.**

---

## 2. Service가 해서는 안 되는 일 (금지 사항)

Service에 다음 로직이 들어가면 설계 위반이다.

- 도메인 규칙(if/else 기반 비즈니스 판단)
- 상태 계산, 정책 판단
- 엔티티 필드 직접 변경
- 여러 상태를 조합한 규칙 처리

### ❌ 잘못된 예시

```java
if (order.getTotalPrice() > 100_000) {
    order.setDiscount(10);
}
```

---

## 3. Service가 해야 하는 일 (허용 사항)

Service는 아래 작업만 수행한다.

- 입력값 검증 (형식, null 등)
- 도메인 객체 생성/조회
- 도메인 메서드 호출
- Repository 저장

### ✅ 권장 예시

```java
Order order = Order.create(memberId, productId, quantity);
order.place();
orderRepository.save(order);
```

---

## 4. Aggregate Repository 사용 규칙

### 4.1 자기 Aggregate Repository 직접 사용 허용

Service는 **자기 Aggregate의 Repository** 는 직접 사용한다.

```java
Order order = orderRepository.findById(orderId);
```

이것이 허용되는 이유:

- Repository는 Aggregate의 생명주기 관리 책임
- Service는 해당 Aggregate 유즈케이스의 소유자
- 트랜잭션 일관성 유지 목적

---

### 4.2 다른 Aggregate Repository 직접 호출 금지

다른 Aggregate의 Repository를 직접 호출하면 결합도가 급격히 증가한다.

```java
// ❌ 금지
Member member = memberRepository.findById(memberId);
```

---

## 5. 다른 Aggregate와 협력하는 방법

다른 Aggregate와의 협력은 반드시 **Port(interface)** 를 통해서만 한다.

```java
public interface MemberValidator {
    void validateActive(Long memberId);
}
```

Service에서는 구현체를 모른 채 인터페이스만 사용한다.

```java
memberValidator.validateActive(memberId);
```

---

## 6. Service는 엔티티를 직접 참조하지 않는다

Service는 다음 형태의 의존성만 가진다.

- 자기 Aggregate 엔티티
- Port 인터페이스
- 자기 Aggregate Repository

다른 Aggregate 엔티티를 파라미터로 받지 않는다.

```java
// ❌ 금지
Order.create(Member member, Product product);

// ✅ 허용
Order.create(Long memberId, Long productId);
```

---

## 7. Service 테스트 전략

Service는 다음 이유로 **순수 자바 단위 테스트 대상이 아니다**.

- Port 구현체가 필요
- 트랜잭션 경계 포함

따라서 Service는:

- 통합 테스트 대상으로만 검증한다
- 유즈케이스 기준 테스트를 작성한다

> 순수 단위 테스트 대상은 **Domain(Entity)** 이다.

---

## 8. Service 코드 길이 가이드

Service 메서드는 다음 기준을 넘지 않는 것을 목표로 한다.

- 한 메서드 30~50줄 이내
- private 메서드 남용 금지
- 읽었을 때 시나리오가 보일 것

---

## 9. 한 줄 요약

> Service는 **유즈케이스 흐름을 조합하는 계층**이며,
> **비즈니스 규칙은 절대 소유하지 않는다.**
