# CORE.md

## 프로젝트 핵심 설계 원칙 (Team Constitution)

이 문서는 본 프로젝트의 **변경되지 않는 설계 기준**을 정의한다.  
모든 코드 작성, 리뷰, 구조 논의는 본 문서를 최우선 기준으로 삼는다.

---

## 1. 프로젝트 성격

- 본 프로젝트는 **실무 친화적인 학습용 사이드 프로젝트**이다.
- 이론적으로 이상적인 아키텍처보다, **현업에서 가장 흔히 쓰이는 구조**를 우선한다.
- 모든 설계 선택은 “왜 이 선택을 했는가”를 설명할 수 있어야 한다.

---

## 2. 도메인 모델에 대한 정의

### 2.1 도메인의 의미

본 프로젝트에서:

> **도메인 = JPA Entity**

- 도메인 모델과 영속 모델을 분리하지 않는다.
- Aggregate, VO, Rich Domain Model을 과도하게 추구하지 않는다.
- DDD는 **개념·사고방식·경계 설정** 수준에서만 적용한다.

---

### 2.2 JPA Entity 사용 방침

JPA Entity를 Domain Entity로 사용한다.  
단, 다음 규칙을 **반드시** 지킨다.

- Entity는 **비즈니스 로직을 포함한다**
- Setter를 통한 **무분별한 상태 변경을 금지**한다
- 생성자 또는 정적 팩토리 메서드를 통해서만 **유효한 상태를 생성**한다
- 상태 변경은 의미 있는 행위 메서드를 통해서만 허용한다

```text
❌ public setter 남발
❌ 외부에서 상태 직접 조작
text
코드 복사
✅ 생성자 / 팩토리로 생성
✅ 의미 있는 도메인 메서드로 상태 변경
2.3 도메인의 책임 범위
Domain(Entity)은 다음 책임만 가진다.

상태(State)

자기 자신에 대한 무결성 검증

단일 도메인 내부 규칙

아래 책임은 가지지 않는다.

text
코드 복사
❌ 유즈케이스 흐름 제어
❌ 다른 도메인 조합
❌ 외부 시스템 접근
3. 레이어별 책임 분리
3.1 Presentation Layer (Controller)
HTTP 요청/응답 처리

Request / Response DTO 변환

비즈니스 로직 금지

text
코드 복사
Controller = 입출력 어댑터
3.2 Application Layer (Service / UseCase)
유즈케이스 흐름의 중심

여러 도메인을 조합

트랜잭션 경계 설정

Port(interface)를 통해 외부 기능 의존

text
코드 복사
Service = 유즈케이스 실행기
3.3 Domain Layer (Entity)
데이터 + 최소한의 비즈니스 규칙

다른 도메인 직접 참조 금지

외부 기술 의존성 금지

3.4 Infrastructure Layer
JPA Repository 구현

Port 구현체

외부 API 연동

기술 세부사항 담당

text
코드 복사
Infrastructure = 기술 구현 책임
4. 도메인 간 의존성 규칙
4.1 도메인 직접 참조 금지
Order → Member 엔티티 직접 참조 ❌

Order → Product 엔티티 직접 참조 ❌

※ 도메인 간 협력 시, 도메인 메서드는 다른 Aggregate의 Entity를 인자로 받지 않는다.

도메인 간 협력은 다음 방식만 허용한다.

ID 기반

Application Port(interface) 사용

java
코드 복사
Order order = Order.create(memberId, productId, quantity);
5. Port 설계 원칙
Port(interface)는 Application Layer에 위치

Service가 “필요로 하는 능력”을 정의

구현체는 Infrastructure Layer에 위치

Service는 구현체를 알지 못한다

6. 테스트 전략 핵심 원칙
6.1 단위 테스트
대상: Application(Service)

순수 Java 테스트

Mock 라이브러리 사용 금지

Fake / InMemory 구현체 사용

text
코드 복사
목표: 유즈케이스 로직 검증
6.2 통합 테스트
실제 API 호출

Spring + DB 포함

유즈케이스 시나리오 기준

6.3 성능 테스트
통합 테스트와 동일한 API

k6 사용

테스트 코드는 별도 작성

API 재사용, 로직 중복 금지

7. 설계 선택 기준
아래 기준에 해당하면 단순한 쪽을 선택한다.

추상화 vs 명확성 → 명확성

이론적 정합성 vs 팀 이해도 → 팀 이해도

미래 가능성 vs 현재 생산성 → 현재 생산성

8. 변경 규칙
본 문서(Core)는 팀 합의 없이는 변경할 수 없다

변경 시 반드시:

ADR 문서 작성

변경 이유 명시

기존 코드 영향 범위 설명

9. 한 줄 요약
> 이 프로젝트는
> **유즈케이스(Application Service) 중심 흐름과
> 비즈니스 로직을 포함한 JPA 엔티티 도메인**을 사용하며,
> **설명 가능한 선택과 팀 합의**를 최우선 가치로 한다.
```
