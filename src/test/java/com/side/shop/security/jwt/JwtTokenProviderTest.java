package com.side.shop.security.jwt;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 테스트용 JwtProperties 설정
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
        jwtProperties.setExpiration(3600000L); // 1시간

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("JWT 토큰을 생성할 수 있다")
    void generateToken() {
        // given
        Long memberId = 1L;
        String email = "user@example.com";
        String role = "USER";

        // when
        String token = jwtTokenProvider.generateToken(memberId, email, role);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("토큰에서 사용자 ID를 추출할 수 있다")
    void getMemberIdFromToken() {
        // given
        Long memberId = 1L;
        String email = "user@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(memberId, email, role);

        // when
        Long extractedMemberId = jwtTokenProvider.getMemberIdFromToken(token);

        // then
        assertThat(extractedMemberId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("토큰에서 이메일을 추출할 수 있다")
    void getEmailFromToken() {
        // given
        Long memberId = 1L;
        String email = "user@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(memberId, email, role);

        // when
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("토큰에서 역할을 추출할 수 있다")
    void getRoleFromToken() {
        // given
        Long memberId = 1L;
        String email = "admin@example.com";
        String role = "ADMIN";
        String token = jwtTokenProvider.generateToken(memberId, email, role);

        // when
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("유효한 토큰은 검증을 통과한다")
    void validateToken_Valid() {
        // given
        Long memberId = 1L;
        String email = "user@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(memberId, email, role);

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰은 검증을 실패한다")
    void validateToken_Invalid() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증을 실패한다")
    void validateToken_Expired() {
        // given
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
        expiredProperties.setExpiration(-1000L); // 이미 만료된 시간 설정

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
        String token = expiredProvider.generateToken(1L, "user@example.com", "USER");

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isFalse();
    }
}
