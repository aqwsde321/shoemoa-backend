# SETUP.md

## 로컬 환경 세팅 가이드

**이 문서는 프로젝트를 로컬에서 실행하기 위한 단계별 가이드입니다.**

---

## 📋 목차

1. [사전 준비](#1-사전-준비)
2. [프로젝트 클론](#2-프로젝트-클론)
3. [Docker 실행](#3-docker-실행)
4. [환경 변수 설정](#4-환경-변수-설정)
5. [데이터베이스 초기화](#5-데이터베이스-초기화)
6. [애플리케이션 실행](#6-애플리케이션-실행)
7. [토스페이먼츠 설정](#7-토스페이먼츠-설정)
8. [동작 확인](#8-동작-확인)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. 사전 준비

### 1.1 필수 설치

#### JDK 17 설치
```bash
# macOS (Homebrew)
brew install openjdk@17

# 환경 변수 설정
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 버전 확인
java -version
# openjdk version "17.x.x"
```

```bash
# Windows
# https://adoptium.net/ 에서 JDK 17 다운로드
# 설치 후 환경 변수 자동 설정됨

# PowerShell에서 확인
java -version
```

```bash
# Ubuntu
sudo apt update
sudo apt install openjdk-17-jdk

java -version
```

---

#### Docker Desktop 설치
```bash
# macOS
brew install --cask docker

# Windows
# https://www.docker.com/products/docker-desktop/ 에서 다운로드

# Ubuntu
sudo apt update
sudo apt install docker.io docker-compose

# 설치 확인
docker --version
docker-compose --version
```

**Docker Desktop 실행 후 확인**:
```bash
docker ps
# CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

---

#### Git 설치 (이미 있으면 스킵)
```bash
# macOS
brew install git

# Windows
# https://git-scm.com/download/win

# Ubuntu
sudo apt install git

# 확인
git --version
```

---

### 1.2 권장 설치

#### IntelliJ IDEA
- Community (무료) 또는 Ultimate (유료)
- https://www.jetbrains.com/idea/download/

**필수 플러그인**:
- Lombok
- Spring Boot Assistant (Ultimate만)

#### Postman
- API 테스트용
- https://www.postman.com/downloads/

---

## 2. 프로젝트 클론

### 2.1 저장소 클론
```bash
# HTTPS
git clone https://github.com/[your-org]/[your-repo].git

# SSH (권장)
git clone git@github.com:[your-org]/[your-repo].git

cd [your-repo]
```

### 2.2 브랜치 확인
```bash
git branch
# * develop

# main 브랜치로 전환 (배포 버전)
git checkout main

# develop 브랜치로 전환 (개발 버전)
git checkout develop
```

---

## 3. Docker 실행

### 3.1 Docker Compose 파일 확인
```yaml
# docker-compose.yml
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

### 3.2 컨테이너 실행
```bash
# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 상태 확인
docker-compose ps
# NAME              IMAGE                 STATUS
# shop-postgres     postgres:15-alpine    Up (healthy)
# shop-redis        redis:7-alpine        Up (healthy)
```

### 3.3 컨테이너 접속 (확인용)
```bash
# PostgreSQL 접속
docker exec -it shop-postgres psql -U shop -d shop

shop=# \dt
# 테이블 목록 (초기에는 비어있음)

shop=# \q
# 종료

# Redis 접속
docker exec -it shop-redis redis-cli

127.0.0.1:6379> ping
# PONG

127.0.0.1:6379> exit
```

---

## 4. 환경 변수 설정

### 4.1 application.yml 설정

**src/main/resources/application.yml**:
```yaml
spring:
  application:
    name: shop
  
  profiles:
    active: local
  
  datasource:
    url: jdbc:postgresql://localhost:5432/shop
    username: shop
    password: shop123
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: create  # 최초 실행 시, 이후 validate로 변경
    properties:
      hibernate:
        format_sql: true
        show_sql: true
        default_batch_fetch_size: 100
    open-in-view: false
  
  data:
    redis:
      host: localhost
      port: 6379
  
  # 로깅
  logging:
    level:
      com.shop: DEBUG
      org.hibernate.SQL: DEBUG
      org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# 토스페이먼츠 (나중에 설정)
payment:
  toss:
    secret-key: ${TOSS_SECRET_KEY:test_sk_xxxx}
    client-key: test_ck_xxxx
```

### 4.2 환경별 설정 파일

**application-local.yml** (로컬 개발):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        show_sql: true

logging:
  level:
    com.shop: DEBUG
```

**application-test.yml** (테스트):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        show_sql: false
```

**application-prod.yml** (운영 - 나중에):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        show_sql: false

logging:
  level:
    com.shop: INFO
```

### 4.3 환경 변수 파일 생성

**.env** (Git에 커밋하지 않음):
```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/shop
DB_USERNAME=shop
DB_PASSWORD=shop123

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# 토스페이먼츠 (테스트 키)
TOSS_SECRET_KEY=test_sk_xxxx
TOSS_CLIENT_KEY=test_ck_xxxx
```

**.gitignore에 추가**:
```
.env
application-local.yml
```

---

## 5. 데이터베이스 초기화

### 5.1 스키마 생성 (JPA가 자동으로 하지만 수동 확인)
```bash
# PostgreSQL 접속
docker exec -it shop-postgres psql -U shop -d shop

# 테이블 확인
shop=# \dt
```

### 5.2 초기 데이터 삽입 (선택)

**src/main/resources/data.sql** (개발용):
```sql
-- 회원
INSERT INTO members (id, email, name, active, created_at) VALUES
(1, 'user1@test.com', '홍길동', true, NOW()),
(2, 'user2@test.com', '김철수', true, NOW()),
(3, 'user3@test.com', '이영희', false, NOW());

-- 상품
INSERT INTO products (id, name, price, created_at) VALUES
(1, '노트북', 1500000, NOW()),
(2, '마우스', 30000, NOW()),
(3, '키보드', 80000, NOW());

-- 재고
INSERT INTO stocks (id, product_id, quantity, version) VALUES
(1, 1, 100, 0),
(2, 2, 500, 0),
(3, 3, 300, 0);
```

**application.yml에 추가**:
```yaml
spring:
  sql:
    init:
      mode: always  # 또는 never (운영)
      data-locations: classpath:data.sql
```

---

## 6. 애플리케이션 실행

### 6.1 Gradle로 빌드
```bash
# 의존성 다운로드 및 빌드
./gradlew clean build

# 테스트 제외하고 빌드 (빠름)
./gradlew clean build -x test
```

### 6.2 애플리케이션 실행

**방법 1: Gradle**
```bash
./gradlew bootRun

# 또는 프로필 지정
./gradlew bootRun --args='--spring.profiles.active=local'
```

**방법 2: JAR 실행**
```bash
java -jar build/libs/shop-0.0.1-SNAPSHOT.jar

# 또는 프로필 지정
java -jar build/libs/shop-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**방법 3: IntelliJ IDEA**
1. `ShopApplication.java` 열기
2. `main` 메서드 옆 실행 버튼 클릭
3. 또는 `Shift + F10`

### 6.3 실행 확인
```bash
# 로그 확인
# Started ShopApplication in 3.456 seconds

# 헬스체크
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## 7. 토스페이먼츠 설정

### 7.1 계정 생성
1. https://developers.tosspayments.com/ 접속
2. 회원가입
3. "개발자센터" → "내 앱 관리"
4. "새 앱 만들기"

### 7.2 API 키 발급
```
테스트 키:
- Client Key: test_ck_xxxxxxxxxxxx
- Secret Key: test_sk_xxxxxxxxxxxx
```

**⚠️ 주의**: Secret Key는 절대 Git에 커밋하지 않음!

### 7.3 환경 변수에 추가
```bash
# .env 파일
TOSS_SECRET_KEY=test_sk_xxxxxxxxxxxx
TOSS_CLIENT_KEY=test_ck_xxxxxxxxxxxx
```

### 7.4 application.yml 수정
```yaml
payment:
  toss:
    secret-key: ${TOSS_SECRET_KEY}
    client-key: ${TOSS_CLIENT_KEY}
    api-url: https://api.tosspayments.com
```

### 7.5 Config 클래스 작성
```java
@Configuration
@ConfigurationProperties(prefix = "payment.toss")
@Getter
@Setter
public class TossPaymentConfig {
    private String secretKey;
    private String clientKey;
    private String apiUrl;
    
    @Bean
    public RestTemplate tossRestTemplate() {
        return new RestTemplate();
    }
}
```

---

## 8. 동작 확인

### 8.1 API 엔드포인트 테스트

**헬스체크**:
```bash
curl http://localhost:8080/actuator/health
```

**회원 가입** (구현 후):
```bash
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "테스트",
    "password": "password123"
  }'
```

**상품 조회** (구현 후):
```bash
curl http://localhost:8080/api/products/1
```

### 8.2 Postman Collection 임포트

**Postman Collection 생성**:
1. Postman 실행
2. "Import" 클릭
3. `docs/postman/shop-api.json` 선택

### 8.3 데이터베이스 확인
```bash
# PostgreSQL 접속
docker exec -it shop-postgres psql -U shop -d shop

# 회원 목록
SELECT * FROM members;

# 상품 목록
SELECT * FROM products;
```

### 8.4 Redis 확인
```bash
# Redis CLI 접속
docker exec -it shop-redis redis-cli

# 모든 키 확인
127.0.0.1:6379> KEYS *

# 특정 키 조회
127.0.0.1:6379> GET products:1
```

---

## 9. 트러블슈팅

### 9.1 포트 충돌

**증상**:
```
Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
Bind to 0.0.0.0:8080 failed
```

**해결**:
```bash
# 8080 포트 사용 중인 프로세스 확인
lsof -i :8080

# 프로세스 종료
kill -9 [PID]

# 또는 application.yml에서 포트 변경
server:
  port: 8081
```

---

### 9.2 Docker 컨테이너 실행 실패

**증상**:
```
Error response from daemon: Conflict. The container name "/shop-postgres" is already in use
```

**해결**:
```bash
# 기존 컨테이너 중지 및 삭제
docker-compose down

# 볼륨까지 삭제 (데이터도 삭제됨)
docker-compose down -v

# 다시 실행
docker-compose up -d
```

---

### 9.3 PostgreSQL 연결 실패

**증상**:
```
HikariPool-1 - Exception during pool initialization.
org.postgresql.util.PSQLException: Connection refused
```

**해결**:
```bash
# Docker 컨테이너 상태 확인
docker-compose ps

# PostgreSQL 로그 확인
docker-compose logs postgres

# 컨테이너 재시작
docker-compose restart postgres

# 헬스체크 확인
docker exec shop-postgres pg_isready -U shop
```

---

### 9.4 Redis 연결 실패

**증상**:
```
Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException
```

**해결**:
```bash
# Redis 상태 확인
docker-compose ps redis

# Redis 로그 확인
docker-compose logs redis

# Redis 접속 테스트
docker exec -it shop-redis redis-cli ping
# PONG

# 컨테이너 재시작
docker-compose restart redis
```

---

### 9.5 QueryDSL Q 클래스 생성 안 됨

**증상**:
```
Cannot resolve symbol 'QOrder'
```

**해결**:
```bash
# Gradle 빌드로 Q 클래스 생성
./gradlew clean build

# IntelliJ에서
# 1. Gradle 탭 열기
# 2. Tasks → other → compileJava 실행
# 3. build/generated/sources/annotationProcessor/java/main 확인
```

**IntelliJ 설정**:
```
Settings → Build, Execution, Deployment → Build Tools → Gradle
- Build and run using: Gradle
- Run tests using: Gradle
```

---

### 9.6 Lombok 동작 안 함

**증상**:
```
Cannot resolve method 'builder()'
```

**해결**:
```bash
# IntelliJ 플러그인 확인
Settings → Plugins → "Lombok" 검색 → 설치

# Annotation Processing 활성화
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
- ✅ Enable annotation processing
```

---

### 9.7 테스트 실행 실패 (Testcontainers)

**증상**:
```
Could not start container
org.testcontainers.containers.ContainerLaunchException
```

**해결**:
```bash
# Docker Desktop이 실행 중인지 확인
docker ps

# Docker가 실행 중이 아니면
# Docker Desktop 실행 후 재시도

# Testcontainers 로그 확인
# src/test/resources/testcontainers.properties
testcontainers.reuse.enable=true
```

---

### 9.8 환경 변수 인식 안 됨

**증상**:
```
property 'payment.toss.secret-key' not found
```

**해결**:

**방법 1: IDE 설정**
```
IntelliJ Run Configuration:
1. Run → Edit Configurations
2. Environment Variables → 편집
3. TOSS_SECRET_KEY=test_sk_xxxx 추가
```

**방법 2: .env 파일 사용**
```bash
# .env 파일 생성 (위 참조)

# Spring Boot에서 .env 로드 (application.yml)
spring:
  config:
    import: optional:file:.env[.properties]
```

---

### 9.9 JPA ddl-auto=create로 데이터 날아감

**증상**:
```
애플리케이션 재시작할 때마다 데이터가 초기화됨
```

**해결**:
```yaml
# application.yml 수정
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 또는 validate
```

**ddl-auto 옵션**:
- `create`: 매번 DROP → CREATE
- `create-drop`: 종료 시 DROP
- `update`: 변경사항만 반영 (개발용)
- `validate`: 스키마 검증만 (운영용)
- `none`: 아무것도 안 함

---

### 9.10 Git 커밋 시 Secret 노출

**증상**:
```
GitHub Secret scanning이 API 키 발견
```

**해결**:
```bash
# .gitignore에 추가
.env
application-local.yml
*-local.yml

# 이미 커밋된 파일 제거
git rm --cached .env
git rm --cached src/main/resources/application-local.yml

git commit -m "Remove sensitive files"
git push
```

**API 키 재발급**:
1. 토스페이먼츠 개발자센터
2. 기존 키 삭제
3. 새 키 발급

---

## 10. 개발 워크플로우

### 10.1 일반적인 작업 흐름
```bash
# 1. 최신 코드 받기
git pull origin develop

# 2. 기능 브랜치 생성
git checkout -b feature/member-signup

# 3. Docker 실행
docker-compose up -d

# 4. 애플리케이션 실행
./gradlew bootRun

# 5. 개발 (코드 수정)
# ...

# 6. 테스트 실행
./gradlew test

# 7. 커밋 및 푸시
git add .
git commit -m "feat: 회원 가입 기능 구현"
git push origin feature/member-signup

# 8. PR 생성
```

### 10.2 매일 시작 시
```bash
# Docker 상태 확인
docker-compose ps

# 실행 안 되어있으면
docker-compose up -d

# 최신 코드 받기
git pull origin develop

# 애플리케이션 실행
./gradlew bootRun
```

### 10.3 작업 종료 시
```bash
# 애플리케이션 종료 (Ctrl + C)

# Docker 종료 (선택)
docker-compose down

# 또는 실행 유지 (다음에 빠르게 시작)
```

---

## 11. 유용한 명령어 모음

### Gradle
```bash
# 빌드
./gradlew clean build

# 테스트만
./gradlew test

# 특정 테스트만
./gradlew test --tests OrderServiceTest

# 의존성 확인
./gradlew dependencies

# 캐시 삭제
./gradlew clean --refresh-dependencies
```

### Docker
```bash
# 실행
docker-compose up -d

# 중지
docker-compose down

# 로그
docker-compose logs -f

# 특정 서비스만
docker-compose logs -f postgres

# 재시작
docker-compose restart postgres

# 볼륨까지 삭제
docker-compose down -v
```

### PostgreSQL
```bash
# 접속
docker exec -it shop-postgres psql -U shop -d shop

# 유용한 명령
\dt         # 테이블 목록
\d members  # 테이블 구조
\l          # 데이터베이스 목록
\q          # 종료
```

### Redis
```bash
# 접속
docker exec -it shop-redis redis-cli

# 유용한 명령
KEYS *                  # 모든 키
GET products:1          # 값 조회
DEL products:1          # 삭제
FLUSHALL                # 전체 삭제
TTL products:1          # TTL 확인
```

---

## 12. 다음 단계

### 세팅 완료 후
1. ✅ GLOSSARY.md 읽기 (용어 이해)
2. ✅ STRUCTURE.md 읽기 (코드 작성법)
3. ✅ FEATURES.md에서 첫 티켓 선택
4. ✅ 구현 시작!

### 추가 세팅 (나중에)
- CI/CD (GitHub Actions)
- 모니터링 (Prometheus + Grafana)
- 로그 수집 (ELK Stack)

---

## 한 줄 요약

> **"JDK 17 + Docker 설치 →  
> 프로젝트 클론 → docker-compose up →  
> ./gradlew bootRun → 개발 시작!"**

---