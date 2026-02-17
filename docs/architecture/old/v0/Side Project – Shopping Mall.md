# Side Project – Shopping Mall

이 프로젝트는 **실무 친화적인 구조를 기반으로 한 학습용 사이드 프로젝트**이다.  
과도한 추상화나 이론 중심 설계를 지양하고, **실제 현업에서 가장 많이 쓰이는 방식**을 기준으로 설계한다.

---

## 🎯 프로젝트 목표

- JPA 기반 서버 애플리케이션 구조에 대한 이해
- Service(Application) 중심 유즈케이스 설계 경험
- 단위 테스트 / 통합 테스트 / 성능 테스트를 분리한 테스트 전략 수립
- 추후 MSA 분리를 고려한 모듈 경계 학습

---

## 🧱 핵심 설계 방향 요약

### 1. 도메인 모델

- **도메인 = JPA 엔티티**
- 도메인과 영속 모델을 분리하지 않는다
- DDD는 개념·사고방식 수준에서만 적용한다

```text
Domain(Entity)
- 상태
- 최소한의 무결성 보장
2. 레이어 역할 분리
text
코드 복사
Controller (Presentation)
- 요청/응답 DTO
- HTTP 책임만 가진다

Service (Application)
- 유즈케이스 흐름
- 여러 도메인 조합
- 트랜잭션 경계

Domain (Entity)
- 데이터 + 최소 규칙
3. 서비스 구조
Service는 도메인 객체를 직접 조합

다른 도메인 접근이 필요한 경우:

ID 기반

Port(interface) 사용

java
코드 복사
Order order = Order.create(memberId, productId, quantity);
4. Port & Infrastructure
Port(interface)는 Application 레이어

구현체는 Infrastructure 레이어

Service는 구현체를 알지 못한다

text
코드 복사
Application
 └─ port (interface)

Infrastructure
 └─ port 구현체 (JPA, 외부 API 등)
🧪 테스트 전략 요약
단위 테스트
대상: Service(Application)

순수 Java

Mock 라이브러리 사용하지 않음

Fake / InMemory 구현체 사용

통합 테스트
실제 API 호출

Spring + DB 포함

유즈케이스 기준 시나리오 테스트

성능 테스트
통합 테스트와 동일한 API

k6 사용

테스트 코드는 별도 작성 (API 재사용)

📂 문서 구조
text
코드 복사
docs/
├─ core/
│  └─ CORE.md          # 팀 공통 설계 원칙 (헌법)
├─ structure/
│  └─ STRUCTURE.md     # 패키지/레이어 구조 규칙
├─ testing/
│  └─ TESTING.md       # 테스트 전략 상세
├─ workflow/
│  └─ WORKFLOW.md      # 브랜치, PR, 리뷰 규칙
├─ planning/
│  ├─ FEATURES.md      # 기능 개발 리스트 + API
│  ├─ SPRINT_PLAN.md
│  └─ ROADMAP.md
└─ adr/                # 주요 설계 결정 기록
📌 반드시 읽어야 할 문서
docs/core/CORE.md

docs/structure/STRUCTURE.md

docs/testing/TESTING.md

이 문서들은 팀 공통 규칙이며,
코드 리뷰 및 설계 논의 시 기준으로 사용한다.

⚠️ 이 프로젝트에서 하지 않는 것
도메인 모델 분리 (Entity / Aggregate / VO 과도 분해)

무분별한 추상화

테스트를 위한 테스트 코드

👥 대상
JPA / Spring 기반 서버 개발 경험을 쌓고 싶은 개발자

실무 구조를 기반으로 한 사이드 프로젝트를 원하는 팀

📝 참고
이 프로젝트는 “정답 아키텍처”를 목표로 하지 않는다.
**“설명 가능한 선택”과 “팀 합의된 구조”를 최우선으로 한다.
```
