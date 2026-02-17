docs/
├─ README.md # 문서 네비게이션 (필수)
│
├─ core/
│ └─ CORE.md # 설계 철학 / 절대 규칙
│
├─ structure/
│ └─ STRUCTURE.md # 코드 구조 / 구현 규칙
│
├─ testing/
│ └─ TESTING.md # 테스트 전략 / 규칙
│
├─ workflow/
│ └─ WORKFLOW.md # Git / 협업 규칙
│
├─ glossary/
│ └─ GLOSSARY.md # 도메인 용어 사전
│
├─ planning/
│ ├─ FEATURES.md # 기능 개발 리스트 + API
│ ├─ SPRINT_PLAN.md # 스프린트 단위 계획
│ └─ ROADMAP.md # 중·장기 일정
│
└─ adr/
├─ 0001-stock-lock.md
├─ 0002-payment-pg.md
└─ README.md

1. docs/README.md (중요)

문서 진입점
새 팀원이 가장 먼저 보는 문서

역할

문서 전체 구조 설명

“어떤 상황에서 어떤 문서를 보면 되는지” 안내

문서 변경 규칙 요약

반드시 있어야 함 (없으면 문서 방치됨)

2. core/CORE.md

헌법 문서

성격

거의 바뀌지 않음

논쟁 끝난 내용만 기록

포함

아키텍처 원칙

DDD 적용 범위

Aggregate / 트랜잭션 기준

MSA 고려 방침

3. structure/STRUCTURE.md

개발자가 가장 자주 여는 문서

포함

패키지 구조

JPA Entity 설계 규칙

연관관계 기준

DTO / Domain 변환 규칙

Lombok 규칙

4. testing/TESTING.md

품질 기준 문서

포함

테스트 레벨 정의

Domain 단위 테스트

UseCase 테스트

통합 테스트

성능 테스트(k6) 연계 원칙

5. workflow/WORKFLOW.md

협업 계약서

포함

브랜치 전략

커밋 메시지

PR 규칙

리뷰 기준

6. glossary/GLOSSARY.md

Ubiquitous Language 기준 문서

포함

도메인 용어 정의

상태값 의미

혼동되기 쉬운 개념 구분

규칙

코드/문서/티켓은 여기 기준으로 작성

7. planning/FEATURES.md

실행 문서 (백로그)

포함

도메인별 기능 목록

API 엔드포인트

Jira 티켓 분해 기준

특징

변경 잦음

스프린트 끝나면 일부 폐기 가능

8. planning/SPRINT_PLAN.md

단기 실행 계획

포함

Sprint 목표

주차별 작업 항목

담당자

9. planning/ROADMAP.md

방향성 공유용

포함

단계별 목표

마일스톤

범위 조정 기록

10. adr/

설계 결정 기록

구조

1 ADR = 1 파일

번호 + 주제

README.md

ADR 작성 규칙

언제 ADR을 남기는지

문서 간 관계 요약
GLOSSARY
↓
CORE
↓
STRUCTURE / TESTING
↓
FEATURES / SPRINT_PLAN
