# JWT Authentication & Authorization

이 문서는 Shoemoa 프로젝트의 JWT(JSON Web Token) 기반 인증 및 인가 시스템에 대해 설명합니다.

## 1. 개요

Shoemoa는 Stateless한 인증 방식을 위해 JWT를 사용합니다. Access Token과 Refresh Token을 함께 사용하여 보안성과 사용자 편의성을 모두 고려했습니다.

## 2. 토큰 구조 및 정책

### 2.1 Access Token
*   **용도**: API 요청 시 인증 수단으로 사용
*   **유효 기간**: 1시간 (3600000ms)
*   **전송 방식**: HTTP Header `Authorization: Bearer <token>`
*   **Payload 정보**:
    *   `sub`: 사용자 ID (Member ID)
    *   `email`: 사용자 이메일
    *   `role`: 사용자 권한 (USER, ADMIN)
    *   `iat`: 발급 시간
    *   `exp`: 만료 시간

### 2.2 Refresh Token
*   **용도**: Access Token 만료 시 재발급 용도
*   **유효 기간**: 7일 (604800000ms)
*   **저장소**: DB (`refresh_tokens` 테이블)
*   **전송 방식**: HTTP-only Cookie
    *   `HttpOnly`: JavaScript에서 접근 불가 (XSS 방어)
    *   `Secure`: HTTPS 환경에서만 전송
    *   `SameSite=Strict`: 동일 사이트 요청에서만 쿠키 전송 (CSRF 방어)
*   **Payload 정보**:
    *   `sub`: 사용자 ID (Member ID)
    *   `type`: "refresh" (토큰 타입 명시)
    *   `iat`: 발급 시간
    *   `exp`: 만료 시간
*   **Rotation 정책**: Refresh Token 사용 시(재발급 요청 시) 새로운 Refresh Token으로 교체됩니다. (RTR: Refresh Token Rotation)

## 3. 인증 프로세스

### 3.1 로그인 (Login)
1.  사용자가 이메일/비밀번호로 로그인 요청 (`POST /api/members/login`)
2.  서버에서 인증 성공 시 Access Token과 Refresh Token 생성
3.  Refresh Token은 DB에 저장 (기존 토큰이 있다면 업데이트)
4.  Access Token은 **JSON 응답 본문**으로, Refresh Token은 **HTTP-only 쿠키**로 클라이언트에게 전송

### 3.2 API 요청 (Request)
1.  클라이언트는 API 요청 시 Header에 Access Token 포함
2.  `JwtAuthenticationFilter`에서 토큰 검증
    *   **유효한 토큰**: `SecurityContext`에 인증 정보 설정 -> 요청 처리
    *   **만료된 토큰**: `401 Unauthorized` (Error Code: `TOKEN_EXPIRED`) 응답
    *   **잘못된 토큰**: `401 Unauthorized` (Error Code: `INVALID_TOKEN` 등) 응답

### 3.3 토큰 재발급 (Reissue)
1.  클라이언트가 `TOKEN_EXPIRED` 에러를 받으면, 브라우저는 자동으로 쿠키에 담긴 Refresh Token과 함께 재발급 요청 (`POST /api/members/reissue`)
2.  서버에서 쿠키의 Refresh Token 검증
    *   DB에 존재하는지 확인
    *   만료되었는지 확인
3.  검증 성공 시:
    *   새로운 Access Token 생성 (JSON 응답 본문)
    *   새로운 Refresh Token 생성 (HTTP-only 쿠키)
    *   DB의 Refresh Token 업데이트
4.  검증 실패 시 (만료됨, DB에 없음 등):
    *   DB에서 해당 토큰 삭제 (만료된 경우)
    *   `401 Unauthorized` 응답 -> 클라이언트는 로그아웃 처리 필요

## 4. 에러 코드 (Error Codes)

인증 실패 시 `401 Unauthorized` 상태 코드와 함께 아래의 에러 코드가 반환됩니다.

| Error Code | Description | Action |
| :--- | :--- | :--- |
| `TOKEN_EXPIRED` | Access Token이 만료됨 | Refresh Token으로 재발급 요청 (`/api/members/reissue`) |
| `INVALID_TOKEN` | 유효하지 않은 토큰 (서명 불일치, 구조 오류 등) | 재로그인 필요 |
| `UNSUPPORTED_TOKEN` | 지원되지 않는 토큰 형식 | 재로그인 필요 |
| `UNAUTHORIZED` | 토큰이 없거나 인증 실패 | 로그인 필요 |
| `MISSING_COOKIE` | 필수 쿠키(Refresh Token) 누락 | 재로그인 필요 |

## 5. 주요 클래스

*   **`JwtTokenProvider`**: 토큰 생성, 파싱, 유효성 검증 담당
*   **`JwtAuthenticationFilter`**: 요청마다 헤더의 토큰을 검사하여 인증 처리
*   **`SecurityConfig`**: Spring Security 설정, 필터 등록, 예외 처리 핸들러 등록
*   **`MemberController`**: 로그인, 회원가입, 토큰 재발급 API 및 쿠키 처리
*   **`MemberService`**: 로그인, 회원가입, 토큰 재발급 비즈니스 로직 처리
*   **`RefreshTokenRepository`**: Refresh Token DB 접근
*   **`GlobalExceptionHandler`**: `MissingRequestCookieException` 등 인증 외 예외 처리

## 6. 보안 고려사항

*   **HTTP-only Cookie**: Refresh Token을 JavaScript가 접근할 수 없는 쿠키에 저장하여 XSS 공격으로부터 토큰 탈취를 방지합니다.
*   **RTR (Refresh Token Rotation)**: Refresh Token 탈취 시 피해를 최소화하기 위해 사용 시마다 교체합니다.
*   **DB 저장**: Refresh Token을 DB에 저장하여, 필요 시(예: 강제 로그아웃) 서버 측에서 토큰을 무효화할 수 있습니다.
*   **예외 처리**: 토큰 만료와 위변조를 구분하여 클라이언트가 적절히 대응할 수 있도록 합니다.
