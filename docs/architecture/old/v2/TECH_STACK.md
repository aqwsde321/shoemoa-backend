# TECH_STACK.md

## 기술 스택

**이 문서는 프로젝트에서 사용하는 기술과 선택 이유를 정리합니다.**

---

## 📋 전체 스택 한눈에 보기

```
Backend
├─ Language: Java 17
├─ Framework: Spring Boot 3.2.x
├─ ORM: Spring Data JPA
├─ Query: QueryDSL 5.0.0
└─ Build: Gradle 8.5

Database
├─ Main: PostgreSQL 15.x
└─ Cache: Redis 7.x

Test
├─ Unit: JUnit 5
├─ Assertion: AssertJ
├─ Container: Testcontainers
└─ Performance: k6

Infrastructure
├─ Container: Docker
└─ Orchestration: Docker Compose

External API
└─ Payment: 토스페이먼츠 API v1
```

---

## 1. Backend

### Java 17
**선택 이유**:
- ✅ LTS (Long Term Support) 버전
- ✅ Record 클래스로 DTO 작성 간편
- ✅ Pattern Matching으로 코드 가독성 향상
- ✅ Spring Boot 3.x 권장 버전

**대안**:
- Java 21 (더 최신이지만 안정성 고려)

**버전 명시**:
```properties
java.version=17
```

---

### Spring Boot 3.2.x
**선택 이유**:
- ✅ 현재 최신 안정 버전
- ✅ Jakarta EE 지원
- ✅ Native Image 지원
- ✅ Observability 개선

**주요 의존성**:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

**대안**:
- Spring Boot 2.7.x (기존 프로젝트 호환성 필요 시)

---

### Spring Data JPA
**선택 이유**:
- ✅ Repository 패턴 기본 제공
- ✅ 기본 CRUD 자동 생성
- ✅ @Lock 지원 (동시성 제어)
- ✅ 트랜잭션 관리 용이

**사용 방식**:
```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 기본 CRUD 자동 제공
}
```

**대안**:
- MyBatis (SQL 직접 제어 필요 시)
- JOOQ (Type-safe SQL)

---

### QueryDSL 5.0.0
**선택 이유**:
- ✅ Type-safe 쿼리
- ✅ 동적 쿼리 작성 용이
- ✅ 컴파일 타임 오류 검증
- ✅ IDE 자동완성 지원

**사용 예시**:
```java
// 동적 쿼리
BooleanBuilder builder = new BooleanBuilder();
if (name != null) {
    builder.and(product.name.contains(name));
}
if (minPrice != null) {
    builder.and(product.price.goe(minPrice));
}

return queryFactory
    .selectFrom(product)
    .where(builder)
    .fetch();
```

**설정**:
```gradle
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
annotationProcessor "jakarta.annotation:jakarta.annotation-api"
annotationProcessor "jakarta.persistence:jakarta.persistence-api"
```

**대안**:
- JPA Criteria API (표준이지만 복잡함)
- JPQL (문자열 쿼리)

---

### Gradle 8.5
**선택 이유**:
- ✅ Maven보다 빠른 빌드 속도
- ✅ Groovy/Kotlin DSL 지원
- ✅ 증분 빌드 지원
- ✅ 멀티 모듈 관리 용이

**build.gradle 주요 설정**:
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.shop'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
    annotationProcessor "jakarta.annotation:jakarta.annotation-api"
    annotationProcessor "jakarta.persistence:jakarta.persistence-api"
    
    // Database
    runtimeOnly 'org.postgresql:postgresql'
    
    // Redis
    implementation 'org.redisson:redisson-spring-boot-starter:3.25.2'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
    testImplementation 'org.testcontainers:postgresql:1.19.3'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## 2. Database

### PostgreSQL 15.x
**선택 이유**:
- ✅ 오픈소스 RDBMS
- ✅ ACID 완벽 지원
- ✅ JSON 타입 지원
- ✅ 풍부한 인덱스 옵션
- ✅ 동시성 제어 우수

**주요 기능 활용**:
```sql
-- JSONB 타입 (필요 시)
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    data JSONB
);

-- 부분 인덱스
CREATE INDEX idx_active_members 
ON members (id) 
WHERE active = true;
```

**Docker 설정**:
```yaml
postgres:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: shop
    POSTGRES_USER: shop
    POSTGRES_PASSWORD: shop123
  ports:
    - "5432:5432"
```

**대안**:
- MySQL 8.0+ (더 널리 사용됨)
- MariaDB (MySQL 호환)

---

### Redis 7.x
**선택 이유**:
- ✅ 빠른 캐싱
- ✅ 분산 락 지원
- ✅ Pub/Sub 기능
- ✅ 다양한 자료구조

**사용 목적**:
1. **캐싱**: 상품 조회 결과
2. **분산 락**: 재고 동시성 제어
3. **세션**: 로그인 세션 (향후)

**Docker 설정**:
```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  command: redis-server --appendonly yes
```

**대안**:
- Memcached (캐싱만 필요 시)
- Hazelcast (Java 네이티브)

---

## 3. Test

### JUnit 5
**선택 이유**:
- ✅ Spring Boot 3.x 기본 테스트 프레임워크
- ✅ @ParameterizedTest 지원
- ✅ @Nested 테스트 구조화
- ✅ 확장 모델 우수

**사용 예시**:
```java
@Test
void 주문_생성_성공() {
    // given
    Order order = Order.create(1L, 10L, 3);
    
    // when
    orderRepository.save(order);
    
    // then
    assertThat(order.getId()).isNotNull();
}

@ParameterizedTest
@ValueSource(ints = {0, -1, -10})
void 수량이_0이하면_예외발생(int quantity) {
    assertThatThrownBy(() -> Order.create(1L, 10L, quantity))
        .isInstanceOf(IllegalArgumentException.class);
}
```

---

### AssertJ
**선택 이유**:
- ✅ 유창한 API (Fluent API)
- ✅ 가독성 높은 테스트
- ✅ 풍부한 Assertion

**사용 예시**:
```java
// JUnit 기본
assertEquals(OrderStatus.CREATED, order.getStatus());

// AssertJ (더 읽기 쉬움)
assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

// 컬렉션 테스트
assertThat(orders)
    .hasSize(3)
    .extracting(Order::getStatus)
    .containsExactly(CREATED, CREATED, PAID);
```

---

### Testcontainers
**선택 이유**:
- ✅ 실제 DB로 테스트
- ✅ Docker 기반 격리 환경
- ✅ 테스트 후 자동 정리
- ✅ CI/CD 통합 용이

**사용 예시**:
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void 통합_테스트() {
        // 실제 PostgreSQL로 테스트
    }
}
```

**대안**:
- H2 (In-Memory DB, 빠르지만 실제 DB와 차이)
- Mock (통합 테스트 불가)

---

### k6
**선택 이유**:
- ✅ Go 기반 고성능
- ✅ JavaScript로 시나리오 작성
- ✅ CLI 기반 실행
- ✅ 다양한 리포트

**사용 예시**:
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '10s', target: 0 },
  ],
};

export default function () {
  const payload = JSON.stringify({
    memberId: 1,
    productId: 10,
    quantity: 2,
  });

  const res = http.post(
    'http://localhost:8080/api/orders',
    payload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
```

**실행**:
```bash
k6 run performance-test.js
```

**대안**:
- JMeter (GUI 기반)
- Gatling (Scala 기반)

---

## 4. Infrastructure

### Docker + Docker Compose
**선택 이유**:
- ✅ 환경 일관성
- ✅ 로컬 개발 간편
- ✅ CI/CD 통합 용이
- ✅ 팀원 온보딩 빠름

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: shop-postgres
    environment:
      POSTGRES_DB: shop
      POSTGRES_USER: shop
      POSTGRES_PASSWORD: shop123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U shop"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: shop-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

**실행**:
```bash
docker-compose up -d
```

---

## 5. External API

### 토스페이먼츠 API v1
**선택 이유**:
- ✅ 국내 점유율 높음
- ✅ 문서 매우 잘 되어있음
- ✅ 테스트 환경 제공
- ✅ 다양한 결제 수단

**주요 기능**:
- 카드 결제
- 가상계좌
- 계좌이체
- 간편결제

**API 엔드포인트**:
```
테스트: https://api.tosspayments.com
운영: https://api.tosspayments.com
```

**인증**:
```java
String encodedAuth = Base64.getEncoder()
    .encodeToString((secretKey + ":").getBytes());

HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Basic " + encodedAuth);
headers.setContentType(MediaType.APPLICATION_JSON);
```

**대안**:
- 나이스페이
- KG이니시스
- 카카오페이 (간편결제만)

---

## 6. 라이브러리

### Lombok
**선택 이유**:
- ✅ Boilerplate 코드 제거
- ✅ Getter/Setter 자동 생성
- ✅ Builder 패턴 간편

**사용**:
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id @GeneratedValue
    private Long id;
    
    private Long memberId;
    private int quantity;
    
    // Getter 자동 생성
    // protected 생성자 자동 생성
}
```

**주의사항**:
- Setter는 사용 금지 (프로젝트 규칙)
- @Data 사용 금지 (너무 많은 메서드 생성)

---

### Redisson
**선택 이유**:
- ✅ Redis 분산 락 구현체
- ✅ Spring Boot Starter 제공
- ✅ RLock 인터페이스

**사용 예시**:
```java
@Component
public class DistributedLockAspect {
    private final RedissonClient redissonClient;
    
    public Object lock(String key) {
        RLock lock = redissonClient.getLock(key);
        
        try {
            boolean acquired = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("락 획득 실패");
            }
            
            // 비즈니스 로직
            
        } finally {
            lock.unlock();
        }
    }
}
```

**대안**:
- Lettuce (Spring Boot 기본, 분산 락 직접 구현 필요)
- Jedis (구버전)

---

## 7. 버전 호환성 매트릭스

| 기술 | 버전 | 호환 | 비고 |
|------|------|------|------|
| Java | 17 | ✅ | LTS |
| Spring Boot | 3.2.x | ✅ | 최신 안정 |
| QueryDSL | 5.0.0 | ✅ | Jakarta EE |
| PostgreSQL | 15.x | ✅ | |
| Redis | 7.x | ✅ | |
| Testcontainers | 1.19.x | ✅ | |
| Redisson | 3.25.x | ✅ | Spring Boot 3 지원 |

---

## 8. 개발 환경 요구사항

### 필수
```
- JDK 17+
- Docker Desktop
- IDE (IntelliJ IDEA 권장)
- Gradle 8.x (Wrapper 사용 권장)
```

### 권장
```
- IntelliJ IDEA Ultimate (Spring 플러그인)
- Postman (API 테스트)
- Docker Desktop (로컬 DB)
- Redis CLI (캐시 확인)
```

---

## 9. 의존성 버전 관리

### Spring Boot Dependency Management 활용
```gradle
// 버전 명시 불필요 (Spring Boot가 관리)
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

// 버전 명시 필요
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
implementation 'org.redisson:redisson-spring-boot-starter:3.25.2'
```

### 주요 버전 명시가 필요한 라이브러리
```gradle
ext {
    querydslVersion = '5.0.0'
    redissonVersion = '3.25.2'
    testcontainersVersion = '1.19.3'
}
```

---

## 10. 기술 스택 학습 순서

### 1주차: 기본
```
✅ Java 17 기본 문법
✅ Spring Boot 구조
✅ JPA 기초
✅ Docker 기본 명령
```

### 2주차: 심화
```
✅ QueryDSL 동적 쿼리
✅ @Lock 사용법
✅ Testcontainers
```

### 3주차: 고급
```
✅ Redis 캐싱
✅ Redisson 분산 락
✅ k6 성능 테스트
```

### 4주차: 외부 연동
```
✅ 토스페이먼츠 API
✅ RestTemplate
✅ 웹훅 처리
```

---

## 한 줄 요약

> **"Java 17 + Spring Boot 3.2 + PostgreSQL + Redis,  
> QueryDSL로 쿼리, Testcontainers로 테스트,  
> Redisson으로 분산 락."**

---