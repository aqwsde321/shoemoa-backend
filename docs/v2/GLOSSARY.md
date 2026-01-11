# GLOSSARY.md

## 용어 사전

**이 문서는 프로젝트에서 사용하는 핵심 용어를 정의합니다.**  
**읽는 순서**: 모르는 용어가 나올 때마다 찾아보세요.

---

## 📚 용어 분류

### 🎯 DDD 핵심 개념

#### [Aggregate (애그리거트)](STRUCTURE.md#34-aggregate-간-관계-규칙)
**한 문장**: 일관성을 유지해야 하는 객체들의 묶음

**설명**:
- 함께 변경되어야 하는 객체들을 하나의 단위로 묶은 것
- 예: `Order` + `OrderItem` → Order Aggregate
- 외부에서는 Root를 통해서만 접근 가능

**예시**:
```java
// Order Aggregate
Order (Root)
  └─ OrderItem (내부 Entity)
```

**관련 용어**: Aggregate Root, Entity

---

#### Aggregate Root (애그리거트 루트)
**한 문장**: Aggregate의 진입점이 되는 대표 Entity

**설명**:
- Aggregate 외부에서 접근할 수 있는 유일한 Entity
- 모든 비즈니스 규칙은 Root를 통해 실행
- Repository는 Root에만 존재

**예시**:
```java
// Order가 Root
Order order = orderRepository.findById(1L);
order.addItem(...);  // OrderItem은 Order를 통해서만 추가

// ❌ 금지: OrderItem을 직접 생성/저장
OrderItem item = new OrderItem(...);
orderItemRepository.save(item);  // 이런 Repository 없음
```

**관련 용어**: Aggregate, Repository

---

#### [Entity (엔티티)](STRUCTURE.md#3-entity-작성-규칙)
**한 문장**: 고유한 식별자(ID)를 가진 객체

**설명**:
- 생명주기 동안 식별자가 변하지 않음
- 속성이 같아도 ID가 다르면 다른 객체
- JPA `@Entity`와 동일 개념 (본 프로젝트에서)

**예시**:
```java
Order order1 = new Order(1L, ...);
Order order2 = new Order(1L, ...);
// order1 == order2 (같은 Entity, ID가 같으므로)
```

**비교**: Value Object는 ID가 없고, 속성이 같으면 같은 객체

**관련 용어**: Aggregate, Value Object

---

#### [Value Object (값 객체)](STRUCTURE.md#9-value-object-사용-기준)
**한 문장**: 식별자 없이 속성으로만 구별되는 불변 객체

**설명**:
- ID 없음
- 불변 (변경 시 새 객체 생성)
- 속성이 같으면 같은 객체

**예시**:
```java
@Embeddable
public class Money {
    private BigDecimal amount;
    
    // Setter 없음, 불변
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
}
```

**비교**: Entity는 ID로 구별, Value Object는 속성으로 구별

**관련 용어**: Entity

---

#### [Domain (도메인)](CORE.md#--도메인-정의)
**한 문장**: 비즈니스 규칙과 지식이 담긴 영역

**설명**:
- 소프트웨어가 해결하려는 문제 영역
- "주문", "회원", "상품" 같은 비즈니스 개념
- **본 프로젝트에서는 Domain = JPA Entity**

**예시**:
```java
// Domain Layer
@Entity
public class Order {
    public void cancel() {  // 비즈니스 규칙
        if (!this.status.isCancelable()) {
            throw new IllegalStateException("취소 불가");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

**관련 용어**: Domain Model, Domain Service

---

#### [Repository (리포지토리)](STRUCTURE.md#6-repository-설계)
**한 문장**: Aggregate를 저장/조회하는 인터페이스

**설명**:
- Aggregate Root당 하나만 존재
- 영속성 기술(JPA 등)을 감춤
- Domain 패키지에 인터페이스, Infrastructure에 구현

**예시**:
```java
// Domain Layer
public interface OrderRepository extends JpaRepository<Order, Long> {
}

// ✅ Aggregate Root당 하나
OrderRepository
MemberRepository
ProductRepository

// ❌ 내부 Entity는 Repository 없음
OrderItemRepository  // 없음
```

**관련 용어**: Aggregate Root, Port

---

### 🏗️ 레이어 개념

#### [Application Service (애플리케이션 서비스)](STRUCTURE.md#4-service-작성-규칙)
**한 문장**: UseCase를 실행하는 흐름 제어 계층

**설명**:
- 비즈니스 로직 없음, 흐름만 조합
- 트랜잭션 경계 정의
- Port와 Repository 사용

**예시**:
```java
@Service
public class OrderService {
    @Transactional
    public Long createOrder(...) {
        memberValidator.validateActive(memberId);  // Port
        stockManager.decrease(productId, qty);     // Port
        Order order = Order.create(...);           // Domain
        return orderRepository.save(order).getId();
    }
}
```

**다른 이름**: UseCase, Service Layer

**관련 용어**: UseCase, Port, Domain Service

---

#### UseCase (유즈케이스)
**한 문장**: 사용자 관점의 기능 단위

**설명**:
- "주문 생성", "주문 취소" 같은 기능
- Application Service 메서드 하나가 보통 하나의 UseCase
- **개념적 용어** (실제 클래스명은 Service)

**예시**:
```java
// "주문 생성" UseCase
@Service
public class OrderService {
    public Long createOrder(...) { }  // ← 이게 UseCase
}
```

**관련 용어**: Application Service

---

#### Domain Service (도메인 서비스)
**한 문장**: 여러 Entity에 걸친 도메인 로직을 담는 객체

**설명**:
- Entity 하나로 표현 안 되는 로직
- 상태 없음 (Stateless)
- **본 프로젝트에서는 사용하지 않음**

**왜 안 쓰나**:
- 대부분 로직은 Entity 메서드로 충분
- Domain Service 남용 시 빈약한 도메인 모델 위험

**대신 사용**: Port를 통한 협력

**관련 용어**: Application Service, Port

---

#### [Port (포트)](STRUCTURE.md#5-port-설계)
**한 문장**: Aggregate 간 협력을 추상화한 인터페이스

**설명**:
- Application Layer에 정의
- 다른 Aggregate나 외부 시스템과 협력 시 사용
- Infrastructure에서 구현

**종류**:
- `~Validator`: 검증 (읽기 전용)
- `~Manager`: 상태 변경
- `~Reader`: 조회
- `~Gateway`: 외부 시스템

**예시**:
```java
// Application Layer
public interface MemberValidator {
    void validateActive(Long memberId);
}

// Infrastructure Layer
@Component
public class JpaMemberValidator implements MemberValidator {
    private final MemberRepository memberRepository;
    
    @Override
    public void validateActive(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow();
        if (!member.isActive()) {
            throw new IllegalStateException("비활성 회원");
        }
    }
}
```

**왜 사용**:
- Aggregate 경계 유지
- [테스트 가능성 (Fake 사용)](TESTING.md#3-usecase-test-fake-port)
- 의존성 역전

**관련 용어**: Application Service, Aggregate

---

#### Adapter (어댑터)
**한 문장**: 외부 기술을 내부 인터페이스에 맞게 변환하는 구현체

**설명**:
- Port의 구현체 = Adapter
- Infrastructure 패키지에 위치
- JPA, HTTP Client 등 기술 상세 포함

**예시**:
```java
// Port
public interface PaymentGateway {
    PaymentResult pay(PaymentRequest req);
}

// Adapter (Infrastructure)
@Component
public class TossPaymentGateway implements PaymentGateway {
    private final RestTemplate restTemplate;
    
    @Override
    public PaymentResult pay(PaymentRequest req) {
        // Toss API 호출
    }
}
```

**관련 용어**: Port

---

### 🧪 테스트 개념

#### [Fake (페이크)](TESTING.md#33-fake-port-구현-예시)
**한 문장**: 실제 동작하는 간단한 구현체 (테스트용)

**설명**:
- Port의 테스트용 구현
- 내부 상태 유지 (Map, List 등)
- Mock과 달리 실제로 동작

**예시**:
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

**비교**: Mock은 행위 검증만, Fake는 실제 동작

**관련 용어**: Port, Mock

---

#### [Mock (목)](TESTING.md#36-fake-vs-mock-차이)
**한 문장**: 행위를 기록/검증하는 테스트 더블

**설명**:
- Mockito 같은 라이브러리 사용
- "이 메서드가 호출됐는지" 검증
- **본 프로젝트에서는 사용 금지**

**왜 안 쓰나**:
- 구현이 아닌 행위에 의존
- 테스트가 깨지기 쉬움
- Fake가 더 실제에 가까움

**관련 용어**: Fake

---

#### [Domain Test (도메인 테스트)](TESTING.md#2-domain-test-순수-자바)
**한 문장**: 비즈니스 규칙을 검증하는 순수 Java 테스트

**설명**:
- Spring, JPA 없음
- Entity 메서드만 테스트
- 가장 빠르고 많이 작성

**예시**:
```java
@Test
void 주문은_생성_상태에서만_취소_가능() {
    Order order = Order.create(1L, 10L, 3, 1000);
    
    order.cancel();
    
    assertThat(order.getStatus()).isEqualTo(CANCELLED);
}
```

**관련 용어**: UseCase Test, Controller Test

---

#### [UseCase Test (유즈케이스 테스트)](TESTING.md#3-usecase-test-fake-port)
**한 문장**: Service 흐름을 Fake Port로 검증하는 테스트

**설명**:
- Spring 없음
- Fake Port 사용
- 비즈니스 시나리오 검증

**예시**:
```java
@Test
void 비활성_회원은_주문_불가() {
    FakeMemberValidator validator = new FakeMemberValidator();
    validator.setActive(1L, false);
    
    OrderService service = new OrderService(..., validator, ...);
    
    assertThatThrownBy(() ->
        service.createOrder(1L, 10L, 3)
    ).isInstanceOf(IllegalStateException.class);
}
```

**관련 용어**: Domain Test, Fake

---

#### [Controller Test (컨트롤러 테스트)](TESTING.md#4-controller-통합-테스트)
**한 문장**: HTTP API 전체 흐름을 검증하는 통합 테스트

**설명**:
- `@SpringBootTest` 사용
- 실제 DB 사용
- 요청 → 응답 전체 검증

**예시**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    @Test
    void 주문_생성_API() throws Exception {
        mockMvc.perform(post("/api/orders")
            .content(...))
            .andExpect(status().isOk());
    }
}
```

**관련 용어**: UseCase Test

---

### 📐 아키텍처 패턴

#### Layered Architecture (계층형 아키텍처)
**한 문장**: 관심사별로 수평 계층을 나누는 구조

**설명**:
- Presentation → Application → Domain → Infrastructure
- 상위 계층은 하위 계층에만 의존

**본 프로젝트 계층**:
```
Presentation (Controller, DTO)
    ↓
Application (Service, Port)
    ↓
Domain (Entity, Repository Interface)
    ↓
Infrastructure (Port 구현, Repository 구현)
```

**관련 용어**: Hexagonal Architecture

---

#### Hexagonal Architecture (육각형 아키텍처)
**한 문장**: 도메인을 중심에 두고 외부 기술을 Port/Adapter로 연결

**설명**:
- 도메인이 외부 기술에 의존하지 않음
- Port = 인터페이스
- Adapter = 구현체

**다른 이름**: Ports and Adapters

**관련 용어**: Port, Adapter

---

## 🔍 자주 헷갈리는 개념

### Service vs Domain Service
| 구분 | Application Service | Domain Service |
|------|---------------------|----------------|
| 위치 | Application Layer | Domain Layer |
| 역할 | 흐름 제어 | 도메인 로직 |
| 상태 | 없음 | 없음 |
| 트랜잭션 | ✅ 있음 | ❌ 없음 |
| 본 프로젝트 | ✅ [사용](STRUCTURE.md#4-service-작성-규칙) | ❌ 사용 안 함 |

---

### Port vs Repository
| 구분 | Port | Repository |
|------|------|------------|
| 목적 | Aggregate 간 협력 | 영속성 |
| 위치 | [Application/port](STRUCTURE.md#5-port-설계) | [Domain](STRUCTURE.md#6-repository-설계) |
| 구현 위치 | Infrastructure | Infrastructure |
| 예시 | MemberValidator | OrderRepository |

---

### UseCase vs Application Service
**같은 개념의 다른 표현**

- **UseCase**: 개념적 용어 ("주문 생성" 기능)
- **Application Service**: 구현 클래스 (OrderService)

```java
// "주문 생성" UseCase를 구현한 Application Service
@Service
public class OrderService {
    public Long createOrder(...) { }  // UseCase 메서드
}
```

---

### Aggregate vs Entity
| 구분 | Aggregate | Entity |
|------|-----------|--------|
| 의미 | 일관성 경계를 가진 묶음 | 식별자를 가진 객체 |
| 관계 | 여러 Entity를 포함 가능 | Aggregate의 구성원 |
| 접근 | Root를 통해서만 | - |
| 예시 | `Order` (Root) + `OrderItem` | `Order`, `OrderItem` 각각 |

---

### [Fake vs Mock](TESTING.md#36-fake-vs-mock-차이)
| 구분 | Fake | Mock |
|------|------|------|
| 구현 | 실제 동작하는 간단 버전 | 행위 기록/검증 |
| 상태 | 내부 상태 유지 | 상태 없음 |
| 검증 | 실제 동작 검증 | 호출 여부 검증 |
| 재사용 | 여러 테스트에서 재사용 | 테스트마다 재정의 |
| 본 프로젝트 | ✅ [사용](TESTING.md#33-fake-port-구현-예시) | ❌ 사용 안 함 |

---

## 📖 본 프로젝트만의 특징

### [Domain = JPA Entity](CORE.md#--도메인-정의)
- 도메인 모델과 영속 모델을 **분리하지 않음**
- `@Entity` = Domain Entity
- 대신 DDD 규율(Setter 금지 등)은 엄격히 적용

### Domain Service 사용 안 함
- 대부분 로직은 Entity 메서드로 충분
- Aggregate 간 협력은 Port 사용
- 복잡도 최소화 목적

### [Mock 라이브러리 금지](TESTING.md#10-테스트-anti-pattern)
- Mockito, MockK 등 사용 안 함
- 대신 Fake 구현체 사용
- 테스트가 실제 동작에 가까워짐

---

## 💡 용어 학습 팁

### 1단계: 핵심 6개만
```
Entity, Aggregate, Repository, Port, Service, UseCase
```

### 2단계: 테스트 3개
```
Domain Test, UseCase Test, Fake
```

### 3단계: 나머지
```
Value Object, Adapter, Domain Service 등
```

---

## 🆘 모르는 용어가 나왔을 때

1. **이 문서 검색** (Ctrl+F)
2. **관련 용어 따라가기** (문서 내 링크)
3. **Sample_code.md에서 실제 코드 보기**
4. **팀 채널에 질문**

---

## 한 줄 요약

> **"모르는 용어 나오면 여기부터.  
> 3줄로 이해 안 되면 Sample_code.md 보기."**

---