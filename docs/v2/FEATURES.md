# FEATURES.md

## 기능 구현 백로그

**이 문서는 구현할 기능 목록과 난이도, 학습 목표를 정리합니다.**

---

## 📖 이 문서 사용법

### 티켓 선택하기
1. **난이도 확인** (Lv1 → Lv2 → Lv3 순서 추천)
2. **학습 목표 확인** (이 티켓으로 뭘 배우는지)
3. **체크리스트 따라 구현**

### 난이도 기준

| 난이도 | 설명 | 예상 시간 | 대상 |
|--------|------|----------|------|
| **Lv1** | 기본 CRUD, Port 없음 | 2~4시간 | 프로젝트 처음 투입 |
| **Lv2** | Port 사용, Aggregate 협력 | 4~8시간 | 구조 이해한 사람 |
| **Lv3** | 복잡한 비즈니스 로직 | 1~2일 | 숙련자 |

---

## 🎯 Epic별 분류

```
1. 회원 관리 (Member)
2. 상품 관리 (Product)
3. 주문 관리 (Order)
4. 결제 관리 (Payment)
5. 할인/쿠폰 (Promotion)
6. 정산 (Settlement)
```

---

## ✅ 구현 완료

### [Lv2] 주문 생성
- **Epic**: 주문 관리
- **학습 목표**: Port를 통한 Aggregate 협력
- **완료일**: 2024-01-10
- **담당**: @홍길동

---

## 🚧 진행 중

### [Lv2] 주문 취소 + 재고 복구
- **Epic**: 주문 관리
- **학습 목표**: 트랜잭션 경계, 보상 트랜잭션
- **담당**: @김철수
- **예상 완료**: 2024-01-15

---

## 📋 백로그

---

## Epic 1: 회원 관리 (Member)

### [Lv1] 회원 가입
**난이도**: Lv1 ⭐  
**예상 시간**: 2~3시간  
**학습 목표**: 
- [Entity](GLOSSARY.md#entity-엔티티) 작성 기본
- [Domain Test](GLOSSARY.md#domain-test-도메인-테스트) 작성
- [Controller Test](GLOSSARY.md#controller-test-컨트롤러-테스트) 작성

**요구사항**:
- 이메일 중복 체크
- 비밀번호 암호화
- 자동 활성화 상태

**구현 체크리스트**:
```
[ ] Member Entity 작성
    [ ] Setter 없음
    [ ] 정적 팩토리 메서드
    [ ] 이메일 검증 로직
[ ] MemberRepository 작성
[ ] MemberService 작성
    [ ] Port 사용 안 함 (단순 CRUD)
[ ] Request/Response DTO 작성
[ ] MemberController 작성
[ ] Domain Test 작성 (이메일 검증)
[ ] Controller Test 작성 (가입 API)
```

**참고 문서**:
- [STRUCTURE.md § 3 (Entity 작성)](STRUCTURE.md#3-entity-작성-규칙)
- [TESTING.md (Domain Test)](TESTING.md#2-domain-test-순수-자바)

---

### [Lv1] 회원 조회
**난이도**: Lv1 ⭐  
**예상 시간**: 1~2시간  
**학습 목표**: 
- [Repository](GLOSSARY.md#repository-리포지토리) 사용
- DTO 변환

**요구사항**:
- ID로 회원 조회
- 존재하지 않으면 404

**구현 체크리스트**:
```
[ ] MemberService.getMember() 작성
[ ] MemberResponse DTO 작성
[ ] MemberController.getMember() 작성
[ ] Controller Test 작성
    [ ] 정상 조회
    [ ] 존재하지 않는 ID
```

---

### [Lv2] 회원 등급 관리
**난이도**: Lv2 ⭐⭐  
**예상 시간**: 4~6시간  
**학습 목표**:
- Enum 활용
- [도메인 로직](GLOSSARY.md#domain-도메인) 작성
- 상태 전이 규칙

**요구사항**:
- 등급: BRONZE → SILVER → GOLD → VIP
- 구매 금액에 따라 자동 승급
- 강등 없음

**구현 체크리스트**:
```
[ ] MemberGrade Enum 작성
[ ] Member.upgradeGrade() 메서드 작성
    [ ] 승급 가능 여부 검증
    [ ] 상태 전이 로직
[ ] Domain Test 작성
    [ ] 정상 승급
    [ ] 이미 최고 등급
    [ ] 건너뛰기 승급 불가
[ ] MemberService.upgradeGrade() 작성
[ ] Controller Test 작성
```

**참고 문서**:
- [CORE.md (비즈니스 로직은 Entity에)](CORE.md#1-비즈니스-로직은-entity-메서드에)

---

### [Lv2] 회원 비활성화
**난이도**: Lv2 ⭐⭐  
**예상 시간**: 3~4시간  
**학습 목표**:
- 상태 변경 로직
- [도메인 규칙](GLOSSARY.md#domain-도메인) 검증
- [Port](GLOSSARY.md#port-포트) 사용 (주문 확인)

**요구사항**:
- 진행 중인 주문이 있으면 비활성화 불가
- 비활성화 후 재활성화 가능

**구현 체크리스트**:
```
[ ] OrderValidator Port 작성
    [ ] hasActiveOrders(memberId) 메서드
[ ] JpaOrderValidator 구현
[ ] Member.deactivate() 메서드 작성
[ ] MemberService.deactivateMember() 작성
    [ ] OrderValidator로 검증
[ ] Domain Test 작성
[ ] UseCase Test 작성 (FakeOrderValidator)
[ ] Controller Test 작성
```

**참고 문서**:
- [STRUCTURE.md § 5 (Port 설계)](STRUCTURE.md#5-port-설계)
- [TESTING.md (UseCase Test)](TESTING.md#3-usecase-test-fake-port)

---

## Epic 2: 상품 관리 (Product)

### [Lv1] 상품 등록
**난이도**: Lv1 ⭐  
**예상 시간**: 2~3시간  
**학습 목표**:
- [Entity](GLOSSARY.md#entity-엔티티) 작성
- 기본 검증

**요구사항**:
- 상품명, 가격, 재고
- 가격은 0 이상
- 재고는 0 이상

**구현 체크리스트**:
```
[ ] Product Entity 작성
    [ ] 가격 검증
    [ ] 재고 검증
[ ] ProductRepository 작성
[ ] ProductService 작성
[ ] Request/Response DTO 작성
[ ] ProductController 작성
[ ] Domain Test 작성
    [ ] 가격 음수 불가
    [ ] 재고 음수 불가
[ ] Controller Test 작성
```

---

### [Lv2] 재고 차감/복구
**난이도**: Lv2 ⭐⭐  
**예상 시간**: 4~5시간  
**학습 목표**:
- [도메인 로직](GLOSSARY.md#domain-도메인) 작성
- 동시성 이슈 인지

**요구사항**:
- 재고 차감 시 부족하면 예외
- 재고 복구 시 원래 재고량 초과 가능

**구현 체크리스트**:
```
[ ] Product.decreaseStock() 메서드 작성
    [ ] 재고 부족 검증
[ ] Product.increaseStock() 메서드 작성
[ ] Domain Test 작성
    [ ] 정상 차감
    [ ] 재고 부족 예외
    [ ] 복구 성공
[ ] ProductService 메서드 작성
[ ] Controller Test 작성
```

**참고 문서**:
- [CORE.md (비즈니스 로직은 Entity에)](CORE.md#1-비즈니스-로직은-entity-메서드에)

---

### [Lv3] 재고 동시성 처리
**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 1~2일  
**학습 목표**:
- 비관적 락 적용
- 동시성 테스트

**요구사항**:
- 동시 차감 시 재고 음수 방지
- @Lock 사용

**구현 체크리스트**:
```
[ ] ProductRepository에 @Lock 추가
[ ] 동시성 테스트 작성
    [ ] ExecutorService 사용
    [ ] 100건 동시 요청
[ ] 성능 테스트 (k6)
[ ] 문서화 (동시성 처리 방식)
```

**참고 문서**:
- [TESTING.md (성능 테스트)](TESTING.md#5-성능-테스트-k6)

---

## Epic 3: 주문 관리 (Order)

### [Lv2] 주문 생성
**난이도**: Lv2 ⭐⭐  
**예상 시간**: 6~8시간  
**학습 목표**:
- [Port](GLOSSARY.md#port-포트)를 통한 [Aggregate](GLOSSARY.md#aggregate-애그리거트) 협력
- 트랜잭션 경계
- [UseCase Test](GLOSSARY.md#usecase-test-유즈케이스-테스트) 작성

**요구사항**:
- 회원 활성화 여부 확인
- 상품 재고 확인 및 차감
- 주문 총액 계산

**구현 체크리스트**:
```
[ ] MemberValidator Port 작성
[ ] ProductValidator Port 작성  
[ ] StockManager Port 작성
[ ] JpaMemberValidator 구현
[ ] JpaProductValidator 구현
[ ] JpaStockManager 구현
[ ] Order Entity 작성
    [ ] 총액 계산 로직
[ ] OrderService.createOrder() 작성
    [ ] @Transactional
    [ ] Port 호출 순서
[ ] Domain Test 작성
[ ] UseCase Test 작성
    [ ] Fake Port 작성
    [ ] 정상 생성
    [ ] 비활성 회원
    [ ] 재고 부족
[ ] Controller Test 작성
```

**참고 문서**:
- [Sample_code.md (전체 예시)](Sample_code.md)
- [STRUCTURE.md § 5 (Port 설계)](STRUCTURE.md#5-port-설계)
- [TESTING.md (UseCase Test)](TESTING.md#3-usecase-test-fake-port)

---

### [Lv2] 주문 취소
**난이도**: Lv2 ⭐⭐  
**예상 시간**: 4~6시간  
**학습 목표**:
- 상태 전이 규칙
- 보상 트랜잭션

**요구사항**:
- CREATED 상태에서만 취소 가능
- 취소 시 재고 복구

**구현 체크리스트**:
```
[ ] Order.cancel() 메서드 작성
    [ ] 상태 검증
[ ] OrderService.cancelOrder() 작성
    [ ] StockManager.increase() 호출
[ ] Domain Test 작성
    [ ] 정상 취소
    [ ] 이미 확정된 주문
[ ] UseCase Test 작성
    [ ] 재고 복구 확인
[ ] Controller Test 작성
```

---

### [Lv3] 주문 목록 조회 (페이징)
**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 6~8시간  
**학습 목표**:
- QueryDSL 사용
- 페이징 처리
- DTO 프로젝션

**요구사항**:
- 회원별 주문 목록
- 상태별 필터
- 페이징 (20개씩)

**구현 체크리스트**:
```
[ ] OrderDslRepository 인터페이스 작성
[ ] OrderDslRepositoryImpl 구현
    [ ] QueryDSL 사용
    [ ] 동적 쿼리 (상태 필터)
[ ] OrderListResponse DTO 작성
[ ] OrderService.getOrders() 작성
[ ] Controller Test 작성
    [ ] 페이징 확인
    [ ] 필터링 확인
```

**참고 문서**:
- [STRUCTURE.md § 6 (Repository 설계)](STRUCTURE.md#6-repository-설계)

---

## Epic 4: 결제 관리 (Payment)

### [Lv2] 결제 요청
**난이도**: Lv2 ⭐⭐  
**예상 시간**: 4~6시간  
**학습 목표**:
- [Gateway Port](GLOSSARY.md#port-포트) 사용
- 외부 시스템 연동 추상화

**요구사항**:
- 주문 확정 후 결제 가능
- PG사 API 호출 (Mock)

**구현 체크리스트**:
```
[ ] PaymentGateway Port 작성
[ ] MockPaymentGateway 구현 (개발용)
[ ] Payment Entity 작성
[ ] PaymentService.requestPayment() 작성
[ ] Domain Test 작성
[ ] UseCase Test 작성 (FakePaymentGateway)
[ ] Controller Test 작성
```

**참고 문서**:
- [STRUCTURE.md § 5.2 (Port 네이밍)](STRUCTURE.md#52-port-종류와-네이밍)

---

### [Lv3] 결제 실패 시 주문 롤백
**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 1일  
**학습 목표**:
- 분산 트랜잭션 문제 인지
- Saga 패턴 기초

**요구사항**:
- 결제 실패 시 주문 상태 원복
- 재고 복구

**구현 체크리스트**:
```
[ ] 실패 시나리오 정의
[ ] 보상 트랜잭션 구현
[ ] UseCase Test 작성
    [ ] 결제 실패 케이스
    [ ] 롤백 확인
[ ] 문서화 (트랜잭션 전략)
```

---

## Epic 5: 할인/쿠폰 (Promotion)

## Epic 5: 할인/쿠폰 (Promotion)

### [Lv3] 쿠폰 발급 및 사용
**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 1~2일  
**학습 목표**:
- 복잡한 [도메인 규칙](GLOSSARY.md#domain-도메인)
- [Value Object](GLOSSARY.md#value-object-값-객체) 활용

**요구사항**:
- 쿠폰 종류: 정액, 정률
- 최소 주문 금액
- 1회만 사용 가능

**구임 체크리스트**:
```
[ ] Coupon Entity 작성
[ ] DiscountPolicy Value Object 작성
[ ] Coupon.use() 메서드 작성
    [ ] 이미 사용된 쿠폰
    [ ] 최소 금액 미달
[ ] Order.applyCoupon() 메서드 작성
[ ] Domain Test 작성
    [ ] 정액 할인
    [ ] 정률 할인
    [ ] 사용 불가 케이스
[ ] UseCase Test 작성
[ ] Controller Test 작성
```

**참고 문서**:
- [STRUCTURE.md § 9 (Value Object)](STRUCTURE.md#9-value-object-사용-기준)

---

## Epic 6: 정산 (Settlement)

### [Lv3] 일별 정산
**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 2일  
**학습 목표**:
- 복잡한 조회 로직
- 배치 처리 기초

**요구사항**:
- 일별 주문 금액 합계
- 일별 취소 금액 합계
- 순매출 계산

**구현 체크리스트**:
```
[ ] Settlement Entity 작성
[ ] SettlementDslRepository 작성
    [ ] 일별 집계 쿼리
[ ] SettlementService 작성
[ ] 스케줄러 설정 (@Scheduled)
[ ] Controller Test 작성
[ ] 성능 테스트 (대량 데이터)
```

---

## 🎓 학습 로드맵 (추천 순서)

### Week 1: 기초 다지기 (Lv1)
```
1. [Lv1] 회원 가입
2. [Lv1] 회원 조회
3. [Lv1] 상품 등록
```
**학습 목표**: Entity 작성, Repository 사용, Domain Test

---

### Week 2: Port 익히기 (Lv2)
```
4. [Lv2] 회원 비활성화
5. [Lv2] 재고 차감/복구
6. [Lv2] 주문 생성 ⭐ (핵심)
```
**학습 목표**: Port 설계, UseCase Test, Aggregate 협력

---

### Week 3: 심화 (Lv2~Lv3)
```
7. [Lv2] 주문 취소
8. [Lv2] 결제 요청
9. [Lv3] 주문 목록 조회
```
**학습 목표**: QueryDSL, 보상 트랜잭션

---

### Week 4: 고급 (Lv3)
```
10. [Lv3] 재고 동시성 처리
11. [Lv3] 쿠폰 발급 및 사용
12. [Lv3] 일별 정산
```
**학습 목표**: 동시성, 복잡한 도메인 로직

---

## 📝 티켓 작성 템플릿

새 기능 추가 시 이 템플릿 사용:

```markdown
### [LvX] 기능명
**난이도**: LvX ⭐  
**예상 시간**: X시간  
**학습 목표**: 
- 배우는 것 1
- 배우는 것 2

**요구사항**:
- 요구사항 1
- 요구사항 2

**구현 체크리스트**:
```
[ ] Entity 작성
[ ] Service 작성
[ ] Port 작성 (필요 시)
[ ] Domain Test 작성
[ ] UseCase Test 작성 (Port 사용 시)
[ ] Controller Test 작성
```

**참고 문서**:
- STRUCTURE.md § X
```

---

## 🔄 진행 상태 업데이트

티켓 시작/완료 시:

```markdown
// 시작 시
## 🚧 진행 중
### [LvX] 기능명
- **담당**: @username
- **시작일**: YYYY-MM-DD
- **예상 완료**: YYYY-MM-DD

// 완료 시
## ✅ 구현 완료
### [LvX] 기능명
- **담당**: @username
- **완료일**: YYYY-MM-DD
- **PR**: #123
```

---

## 한 줄 요약

> **"난이도 보고 선택,  
> 학습 목표 확인,  
> 체크리스트 따라 구현."**

---