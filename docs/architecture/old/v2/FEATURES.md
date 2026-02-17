# FEATURES.md

## 기능 구현 백로그

**이 문서는 구현할 기능 목록과 난이도, 학습 목표를 정리합니다.**

---

## 📖 이 문서 사용법

### 티켓 선택하기

1. **난이도 확인** (Lv1 → Lv2 → Lv3 → Lv4 순서 추천)
2. **학습 목표 확인** (이 티켓으로 뭘 배우는지)
3. **체크리스트 따라 구현**

### 난이도 기준

| 난이도  | 설명                      | 예상 시간 | 대상               | 핵심 기술             |
| ------- | ------------------------- | --------- | ------------------ | --------------------- |
| **Lv1** | 기본 CRUD, Port 없음      | 2~4시간   | 프로젝트 처음 투입 | JPA 기본              |
| **Lv2** | Port 사용, Aggregate 협력 | 4~8시간   | 구조 이해한 사람   | Port, 트랜잭션        |
| **Lv3** | 복잡한 비즈니스 로직      | 1~2일     | 숙련자             | QueryDSL, 도메인 로직 |
| **Lv4** | 동시성, 성능, 외부 연동   | 2~3일     | 고급               | Lock, Redis, 외부 API |

---

## 🎯 Epic별 분류

```
1. 회원 관리 (Member)
2. 상품 관리 (Product)
3. 주문 관리 (Order)
4. 결제 관리 (Payment)
5. 재고 관리 (Stock) ⭐ 핵심
6. 할인/쿠폰 (Promotion)
7. 캐싱 (Cache)
8. 정산 (Settlement)
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
- 비밀번호 암호화 (BCrypt)
- 자동 활성화 상태

**구현 체크리스트**:

```
[ ] Member Entity 작성
    [ ] Setter 없음
    [ ] 정적 팩토리 메서드
    [ ] 이메일 검증 로직
    [ ] 비밀번호 암호화 메서드
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
**학습 목도**:

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

- Entity 작성
- 기본 검증

**요구사항**:

- 상품명, 가격, 초기 재고
- 가격은 0 이상
- 재고는 0 이상

**구현 체크리스트**:

```
[ ] Product Entity 작성
    [ ] 가격 검증
    [ ] 초기 재고 검증
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

### [Lv3] 상품 목록 조회 (검색 + 페이징)

**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 6~8시간  
**학습 목표**:

- QueryDSL 사용
- 동적 쿼리
- 페이징 처리

**요구사항**:

- 상품명 검색
- 가격 범위 필터
- 카테고리 필터
- 페이징 (20개씩)

**구현 체크리스트**:

```
[ ] ProductDslRepository 인터페이스 작성
[ ] ProductDslRepositoryImpl 구현
    [ ] QueryDSL BooleanBuilder 사용
    [ ] 동적 쿼리 (검색, 필터)
    [ ] Pageable 처리
[ ] ProductSearchRequest DTO 작성
[ ] ProductListResponse DTO 작성
[ ] ProductService.searchProducts() 작성
[ ] Controller Test 작성
    [ ] 검색 조건별 테스트
    [ ] 페이징 테스트
```

**참고 문서**:

- STRUCTURE.md § 6 (Repository 설계)

---

## Epic 3: 재고 관리 (Stock) ⭐ 핵심 Epic

### [Lv2] 재고 엔티티 설계

**난이도**: Lv2 ⭐⭐  
**예상 시간**: 3~4시간  
**학습 목표**:

- Aggregate 설계
- 재고 도메인 이해

**요구사항**:

- Product와 1:1 관계
- 재고 수량 관리
- 재고 이력 기록 (선택)

**구현 체크리스트**:

```
[ ] Stock Entity 작성
    [ ] Product ID 참조 (Entity 참조 아님)
    [ ] 수량 필드
    [ ] Version 필드 (낙관적 락 대비)
[ ] StockRepository 작성
[ ] Domain Test 작성
    [ ] 재고 생성
    [ ] 기본 검증
```

---

### [Lv2] 재고 차감/복구

**난이도**: Lv2 ⭐⭐  
**예상 시간**: 4~5시간  
**학습 목표**:

- [도메인 로직](GLOSSARY.md#domain-도메인) 작성
- 동시성 이슈 인지 (해결은 Lv4)

**요구사항**:

- 재고 차감 시 부족하면 예외
- 재고 복구 시 원래 재고량 초과 가능

**구현 체크리스트**:

```
[ ] Stock.decrease() 메서드 작성
    [ ] 재고 부족 검증
    [ ] InsufficientStockException
[ ] Stock.increase() 메서드 작성
[ ] Domain Test 작성
    [ ] 정상 차감
    [ ] 재고 부족 예외
    [ ] 복구 성공
    [ ] 음수 입력 불가
[ ] StockManager Port 작성
[ ] JpaStockManager 구현
[ ] Controller Test 작성
```

**참고 문서**:

- [CORE.md (비즈니스 로직은 Entity에)](CORE.md#1-비즈니스-로직은-entity-메서드에)

---

### [Lv4] 재고 동시성 제어 (Pessimistic Lock)

**난이도**: Lv4 ⭐⭐⭐⭐  
**예상 시간**: 1~2일  
**학습 목표**:

- 동시성 문제 이해
- Pessimistic Lock 적용
- 동시성 테스트 작성

**요구사항**:

- 동시 차감 시 재고 음수 방지
- 100명 동시 주문 처리

**기술 스택**:

- `@Lock(PESSIMISTIC_WRITE)`
- `@Transactional` 격리 수준

**구현 체크리스트**:

```
[ ] StockRepository에 락 쿼리 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithLock(@Param("id") Long id);

[ ] JpaStockManager 수정
    [ ] findByIdWithLock() 사용

[ ] 동시성 테스트 작성
    [ ] ExecutorService 100 스레드
    [ ] CountDownLatch 사용
    [ ] 재고 음수 발생 안 함 검증

[ ] 성능 측정
    [ ] 100건 처리 시간 측정
    [ ] 병목 지점 확인

[ ] ADR 작성
    [ ] "재고 락 전략 선택 이유"
    [ ] Pessimistic vs Optimistic 비교
```

**테스트 예시**:

```java
@Test
void 동시에_100명이_재고_1개_상품_주문시_1명만_성공() {
    // given
    Stock stock = createStock(productId, 1);
    int threadCount = 100;
    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    // when
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                stockManager.decrease(productId, 1);
                successCount.incrementAndGet();
            } catch (InsufficientStockException e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    // then
    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failCount.get()).isEqualTo(99);

    Stock result = stockRepository.findById(stock.getId()).get();
    assertThat(result.getQuantity()).isEqualTo(0);
}
```

**참고 자료**:

- "재고시스템으로 알아보는 동시성이슈 해결방법" (인프런)
- JPA Lock 공식 문서

---

### [Lv4] 재고 동시성 제어 (Redis 분산 락)

**난이도**: Lv4 ⭐⭐⭐⭐  
**예상 시간**: 2~3일  
**학습 목표**:

- Redis 분산 락 이해
- Redisson 사용
- 락 타임아웃 처리

**요구사항**:

- 여러 서버 환경 대비
- Redisson 사용
- 락 획득 실패 시 재시도

**기술 스택**:

- Redis
- Redisson
- `@DistributedLock` 어노테이션 (직접 구현)

**구현 체크리스트**:

```
[ ] Redis 의존성 추가
    implementation 'org.redisson:redisson-spring-boot-starter:3.x.x'

[ ] RedissonClient 설정

[ ] DistributedLock 어노테이션 작성
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DistributedLock {
        String key();
        long waitTime() default 5000;
        long leaseTime() default 3000;
    }

[ ] DistributedLockAspect 작성
    [ ] Redisson RLock 사용
    [ ] tryLock() 재시도 로직

[ ] StockService에 적용
    @DistributedLock(key = "#productId")
    public void decrease(Long productId, int quantity)

[ ] 동시성 테스트
    [ ] 100 스레드 동시 실행

[ ] 성능 비교
    [ ] Pessimistic Lock vs Redis Lock

[ ] ADR 작성
    [ ] "분산 환경에서 Redis 락 선택 이유"
```

**참고 자료**:

- Redisson 공식 문서
- "분산 락으로 동시성 처리하기" 글

---

### [Lv3] 재고 이력 관리

**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 6~8시간  
**학습 목표**:

- 이벤트 기반 설계
- 도메인 이벤트

**요구사항**:

- 재고 변경 시 이력 기록
- 누가, 언제, 얼마나 변경했는지

**구현 체크리스트**:

```
[ ] StockHistory Entity 작성
[ ] StockHistoryType Enum (INCREASE, DECREASE)
[ ] Stock.decrease() 수정
    [ ] 이력 기록 로직 추가
[ ] StockHistoryRepository 작성
[ ] 조회 API 작성
[ ] Controller Test 작성
```

---

## Epic 4: 주문 관리 (Order)

### [Lv2] 주문 생성

**난이도**: Lv2 ⭐⭐  
**예상 시간**: 6~8시간  
**학습 목표**:

- Port를 통한 Aggregate 협력
- 트랜잭션 경계
- UseCase Test 작성

**요구사항**:

- 회원 활성화 여부 확인
- 상품 존재 확인
- 재고 차감
- 주문 총액 계산

**구현 체크리스트**:

```
[ ] MemberValidator Port 작성
[ ] ProductValidator Port 작성
[ ] StockManager Port 작성
[ ] JpaMemberValidator 구현
[ ] JpaProductValidator 구현
[ ] JpaStockManager 구현 (이미 있으면 스킵)
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

- Sample_code.md (전체 예시)
- STRUCTURE.md § 5 (Port 설계)
- TESTING.md (UseCase Test)

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

- STRUCTURE.md § 6 (Repository 설계)

---

## Epic 5: 결제 관리 (Payment)

### [Lv2] 결제 요청 (Mock)

**난이도**: Lv2 ⭐⭐  
**예상 시간**: 4~6시간  
**학습 목표**:

- Gateway Port 사용
- 외부 시스템 연동 추상화

**요구사항**:

- 주문 확정 후 결제 가능
- PG사 API 호출 (Mock)

**구현 체크리스트**:

```
[ ] PaymentGateway Port 작성
    PaymentResult requestPayment(PaymentRequest);
    PaymentResult confirmPayment(String paymentKey);

[ ] MockPaymentGateway 구현 (개발용)
    [ ] 항상 성공 반환

[ ] Payment Entity 작성
[ ] PaymentService.requestPayment() 작성
[ ] Domain Test 작성
[ ] UseCase Test 작성 (FakePaymentGateway)
[ ] Controller Test 작성
```

**참고 문서**:

- STRUCTURE.md § 5.2 (Port 네이밍)

---

### [Lv4] 토스페이먼츠 연동

**난이도**: Lv4 ⭐⭐⭐⭐  
**예상 시간**: 2~3일  
**학습 목표**:

- 실제 PG 연동
- 웹훅 처리
- 결제 실패 시나리오

**요구사항**:

- 토스페이먼츠 테스트 계정
- 결제 요청/승인 API
- 웹훅 처리

**기술 스택**:

- RestTemplate or WebClient
- 토스페이먼츠 API v1

**구현 체크리스트**:

```
[ ] 토스페이먼츠 계정 생성
    [ ] 테스트 시크릿 키 발급

[ ] TossPaymentGateway 구현
    [ ] RestTemplate Bean 설정
    [ ] 결제 요청 API 호출
    [ ] 결제 승인 API 호출
    [ ] 시크릿 키 Base64 인코딩

[ ] PaymentWebhookController 작성
    [ ] POST /api/payments/webhook
    [ ] 웹훅 검증 (시그니처)

[ ] PaymentService 수정
    [ ] 결제 상태 업데이트
    [ ] 주문 상태 연동

[ ] 실패 시나리오 처리
    [ ] 결제 실패 시 주문 취소
    [ ] 재고 복구

[ ] Controller Test 작성
    [ ] MockServer 사용

[ ] ADR 작성
    [ ] "토스페이먼츠 선택 이유"
```

**참고 자료**:

- 토스페이먼츠 공식 문서: https://docs.tosspayments.com/

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
    [ ] PG 승인 실패
    [ ] 네트워크 타임아웃

[ ] 보상 트랜잭션 구현
    [ ] Order.failPayment() 메서드
    [ ] 재고 복구 (StockManager)

[ ] UseCase Test 작성
    [ ] 결제 실패 케이스
    [ ] 롤백 확인

[ ] 문서화 (트랜잭션 전략)
```

---

## Epic 6: 할인/쿠폰 (Promotion)

### [Lv3] 쿠폰 발급 및 사용

**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 1~2일  
**학습 목표**:

- 복잡한 도메인 규칙
- Value Object 활용

**요구사항**:

- 쿠폰 종류: 정액, 정률
- 최소 주문 금액
- 1회만 사용 가능

**구현 체크리스트**:

```
[ ] Coupon Entity 작성
[ ] DiscountPolicy Value Object 작성
    [ ] 정액 할인 계산
    [ ] 정률 할인 계산
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

- STRUCTURE.md § 9 (Value Object)

---

## Epic 7: 캐싱 (Cache)

### [Lv4] 상품 조회 캐싱 (Redis)

**난이도**: Lv4 ⭐⭐⭐⭐  
**예상 시간**: 1~2일  
**학습 목표**:

- Redis 캐싱 전략
- 캐시 무효화
- 성능 개선

**요구사항**:

- 상품 상세 조회 캐싱
- TTL 30분
- 상품 수정 시 캐시 무효화

**기술 스택**:

- Redis
- Spring Cache Abstraction

**구현 체크리스트**:

```
[ ] Redis 설정
    [ ] RedisTemplate Bean
    [ ] CacheManager 설정

[ ] ProductService 수정
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProduct(Long id)

    @CacheEvict(value = "products", key = "#id")
    public void updateProduct(Long id, ...)

[ ] 성능 테스트
    [ ] 캐시 적용 전
    [ ] 캐시 적용 후
    [ ] 응답 시간 비교

[ ] 캐시 모니터링
    [ ] Redis CLI로 확인
    [ ] 히트율 측정

[ ] ADR 작성
    [ ] "캐시 전략 선택 이유"
    [ ] TTL 30분 선택 이유
```

---

### [Lv4] 재고 캐싱 (주의 필요)

**난이도**: Lv4 ⭐⭐⭐⭐  
**예상 시간**: 2일  
**학습 목표**:

- 캐시 일관성 문제
- Write-Through vs Write-Behind

**요구사항**:

- 재고 조회 캐싱
- 재고 변경 시 즉시 캐시 업데이트
- 동시성 보장

**⚠️ 주의사항**:
재고는 정합성이 중요하므로 캐싱 시 매우 조심해야 함

**구현 체크리스트**:

```
[ ] 캐싱 전략 결정
    [ ] Cache-Aside
    [ ] Write-Through 선택

[ ] StockService 수정
    @Cacheable(value = "stocks", key = "#productId")
    public int getStock(Long productId)

    @CachePut(value = "stocks", key = "#productId")
    public int decreaseStock(Long productId, int qty)

[ ] 동시성 테스트
    [ ] 캐시 + 락 조합 테스트

[ ] 정합성 검증
    [ ] DB vs 캐시 데이터 일치 확인

[ ] ADR 작성
    [ ] "재고 캐싱 전략과 위험성"
```

---

## Epic 8: 정산 (Settlement)

### [Lv3] 일별 정산

**난이도**: Lv3 ⭐⭐⭐  
**예상 시간**: 2일  
**학습 목표**:

- 복잡한 조회 로직
- 배치 처리 기초
- 스케줄러 사용

**요구사항**:

- 일별 주문 금액 합계
- 일별 취소 금액 합계
- 순매출 계산

**구현 체크리스트**:

```
[ ] Settlement Entity 작성
    [ ] 정산 날짜
    [ ] 총 주문 금액
    [ ] 총 취소 금액
    [ ] 순매출
[ ] SettlementDslRepository 작성
    [ ] 일별 집계 쿼리
    [ ] GROUP BY 날짜
[ ] SettlementService 작성
    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시
    public void calculateDailySettlement()
[ ] 스케줄러 설정
    @EnableScheduling
[ ] Controller Test 작성
[ ] 성능 테스트 (대량 데이터)
```

---

## 🎓 학습 로드맵 (추천 순서)

### Phase 1: 기초 다지기 (1~2주) - Lv1

```
Week 1:
1. [Lv1] 회원 가입
2. [Lv1] 회원 조회
3. [Lv1] 상품 등록

Week 2:
4. [Lv2] 회원 등급 관리
5. [Lv2] 재고 엔티티 설계
6. [Lv2] 재고 차감/복구
```

**학습 목표**: Entity 작성, Repository 사용, Domain Test, Port 기초

---

### Phase 2: Port 마스터하기 (2~3주) - Lv2

```
Week 3:
7. [Lv2] 회원 비활성화 (Port 사용)
8. [Lv2] 주문 생성 ⭐ (핵심)

Week 4:
9. [Lv2] 주문 취소
10. [Lv2] 결제 요청 (Mock)
```

**학습 목표**: Port 설계, UseCase Test, Aggregate 협력, 트랜잭션 경계

---

### Phase 3: 심화 학습 (3~4주) - Lv3

```
Week 5:
11. [Lv3] 상품 목록 조회 (QueryDSL)
12. [Lv3] 주문 목록 조회 (페이징)

Week 6:
13. [Lv3] 쿠폰 발급 및 사용
14. [Lv3] 재고 이력 관리

Week 7:
15. [Lv3] 결제 실패 시 주문 롤백
16. [Lv3] 일별 정산
```

**학습 목표**: QueryDSL, 복잡한 도메인 로직, 보상 트랜잭션

---

### Phase 4: 고급 주제 (4~6주) - Lv4

```
Week 8-9: 동시성 제어
17. [Lv4] 재고 동시성 (Pessimistic Lock) ⭐ 핵심
18. [Lv4] 재고 동시성 (Redis 분산 락) ⭐ 핵심

Week 10-11: 외부 연동
19. [Lv4] 토스페이먼츠 연동 ⭐ 핵심

Week 12: 성능 최적화
20. [Lv4] 상품 조회 캐싱 (Redis)
21. [Lv4] 재고 캐싱 (주의)
```

**학습 목표**: 동시성 제어, 분산 락, 외부 API, Redis 캐싱

---

## 🔧 기술 스택 요구사항

### 필수

```yaml
Backend:
  - Java 17+
  - Spring Boot 3.2+
  - Spring Data JPA
  - QueryDSL 5.0+

Database:
  - PostgreSQL 15+ (또는 MySQL 8.0+)

Test:
  - JUnit 5
  - AssertJ
  - Testcontainers
```

### Lv4 티켓 필요

```yaml
Cache:
  - Redis 7+
  - Redisson 3.x (분산 락용)

Payment:
  - 토스페이먼츠 API v1
  - RestTemplate or WebClient
```

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

**기술 스택** (Lv4만):

- 기술 1
- 기술 2

**구현 체크리스트**:
```

[ ] Entity 작성
[ ] Service 작성
[ ] Port 작성 (필요 시)
[ ] Domain Test 작성
[ ] UseCase Test 작성 (Port 사용 시)
[ ] Controller Test 작성
[ ] ADR 작성 (Lv4만)

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
- **주요 학습**: Port 설계, 동시성 제어 등
```

---

## 💡 팁과 주의사항

### Lv4 티켓 주의사항

#### 재고 동시성 (Pessimistic Lock)

```
⚠️ 주의:
- 트랜잭션 범위 확인 필수
- 락 타임아웃 설정
- 데드락 가능성 고려

✅ 체크포인트:
- @Transactional이 Service 메서드에 있는가?
- findByIdWithLock()을 사용하는가?
- 동시성 테스트가 통과하는가?
```

#### Redis 분산 락

```
⚠️ 주의:
- Redis 장애 시 락 획득 실패
- 락 리소스 해제 누락 방지
- TTL 적절히 설정

✅ 체크포인트:
- try-finally로 락 해제하는가?
- 락 획득 실패 시 재시도하는가?
- 락 타임아웃이 적절한가?
```

#### 토스페이먼츠 연동

```
⚠️ 주의:
- 시크릿 키 노출 금지
- 웹훅 검증 필수
- 멱등성 처리

✅ 체크포인트:
- 시크릿 키가 환경변수인가?
- 웹훅 시그니처를 검증하는가?
- 중복 결제를 방지하는가?
```

---

## 📚 학습 자료

### 재고 동시성

```
무료 강의:
- "재고시스템으로 알아보는 동시성이슈 해결방법" (인프런)

블로그:
- "Pessimistic Lock vs Optimistic Lock"
- "재고 시스템 설계하기"

공식 문서:
- JPA Lock 모드 (@Lock)
- Spring Transaction Propagation
```

### Redis

```
공식 문서:
- Redis 공식 문서: https://redis.io/docs/
- Redisson 문서: https://github.com/redisson/redisson/wiki

블로그:
- "분산 락으로 동시성 처리하기"
- "Redis를 이용한 캐싱 전략"
```

### 토스페이먼츠

```
공식 문서:
- https://docs.tosspayments.com/
- 테스트 카드 번호
- 웹훅 가이드

튜토리얼:
- "토스페이먼츠 결제 연동 가이드"
```

### QueryDSL

```
공식 문서:
- http://querydsl.com/

블로그:
- "QueryDSL 동적 쿼리 작성법"
- "QueryDSL과 Pageable"
```

---

## 🎯 목표 설정 가이드

### 단기 목표 (1~2주)

```
✅ Lv1 티켓 3개 완료
✅ Domain Test 작성 숙달
✅ Entity 작성 규칙 체득
```

### 중기 목표 (4~6주)

```
✅ Lv2 티켓 5개 완료
✅ Port 설계 능숙
✅ UseCase Test 작성 능숙
✅ 주문-재고-결제 흐름 구현
```

### 장기 목표 (8~12주)

```
✅ Lv4 티켓 3개 완료
✅ 재고 동시성 제어 완벽 이해
✅ Redis 분산 락 구현
✅ 토스페이먼츠 연동 완료
✅ 포트폴리오 완성
```

---

## 📊 진행률 추적

```markdown
## 전체 진행률

### Lv1 (기초) - 4개

- [x] 회원 가입
- [x] 회원 조회
- [ ] 상품 등록
- [ ] ...

### Lv2 (중급) - 7개

- [ ] 회원 등급 관리
- [ ] 회원 비활성화
- [x] 재고 차감/복구
- [ ] 주문 생성
- [ ] ...

### Lv3 (고급) - 6개

- [ ] 상품 목록 조회
- [ ] 주문 목록 조회
- [ ] ...

### Lv4 (전문가) - 5개

- [ ] 재고 동시성 (Pessimistic)
- [ ] 재고 동시성 (Redis)
- [ ] 토스페이먼츠 연동
- [ ] ...

**전체**: 3 / 22 (13.6%)
```

---

## 🔗 다른 문서와의 연계

### FEATURES.md 사용 시 참고 문서

| 상황                  | 문서             |
| --------------------- | ---------------- |
| Entity 작성 방법 모름 | STRUCTURE.md § 3 |
| Port 설계 방법 모름   | STRUCTURE.md § 5 |
| 테스트 작성 방법 모름 | TESTING.md       |
| 용어 이해 안 됨       | GLOSSARY.md      |
| 왜 이렇게 설계?       | CORE.md          |
| 전체 코드 예시 필요   | Sample_code.md   |

---

## 한 줄 요약

> **"Lv1부터 시작,  
> 난이도별로 단계적 학습,  
> 체크리스트 따라 구현."**

---
