package com.side.shop.member.application;

import static org.assertj.core.api.Assertions.*;

import com.side.shop.member.domain.Member;
import com.side.shop.member.domain.MemberRole;
import com.side.shop.member.exception.DuplicateEmailException;
import com.side.shop.member.exception.InvalidCredentialsException;
import com.side.shop.member.infrastructure.MemberRepository;
import com.side.shop.member.presentation.dto.LoginRequestDto;
import com.side.shop.member.presentation.dto.LoginResponseDto;
import com.side.shop.member.presentation.dto.SignupRequestDto;
import com.side.shop.member.presentation.dto.TokenResponseDto;
import com.side.shop.security.auth.RefreshToken;
import com.side.shop.security.auth.RefreshTokenRepository;
import com.side.shop.security.jwt.JwtProperties;
import com.side.shop.security.jwt.JwtTokenProvider;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional // 각 테스트마다 롤백
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("회원가입이 성공하면 DB에 저장된다")
    void signup_Success() {
        // given
        SignupRequestDto request = new SignupRequestDto("newuser@example.com", "password123");

        // when
        memberService.signup(request);

        // then
        Member savedMember = memberRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.USER);
        assertThat(savedMember.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
    void signup_DuplicateEmail() {
        // given
        SignupRequestDto request1 = new SignupRequestDto("duplicate@example.com", "password123");
        memberService.signup(request1);

        SignupRequestDto request2 = new SignupRequestDto("duplicate@example.com", "password456");

        // when & then
        assertThatThrownBy(() -> memberService.signup(request2))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("duplicate@example.com");
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인하면 JWT를 받는다")
    void login_Success() {
        // given
        String email = "loginuser@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        memberService.signup(signupRequestDto);

        // 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        LoginRequestDto loginRequestDto = new LoginRequestDto(email, "password123");

        // when
        LoginResponseDto response = memberService.login(loginRequestDto);

        // then
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRole()).isEqualTo("USER");

        // Refresh Token이 DB에 저장되었는지 확인
        RefreshToken savedRefreshToken =
                refreshTokenRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(savedRefreshToken.getToken()).isEqualTo(response.getRefreshToken());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
    void login_EmailNotFound() {
        // given
        LoginRequestDto request = new LoginRequestDto("nonexistent@example.com", "password123");

        // when & then
        assertThatThrownBy(() -> memberService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외가 발생한다")
    void login_WrongPassword() {
        // given
        String email = "user@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        memberService.signup(signupRequestDto);

        // 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        LoginRequestDto loginRequestDto = new LoginRequestDto(email, "wrongpassword");

        // when & then
        assertThatThrownBy(() -> memberService.login(loginRequestDto)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 Access Token을 재발급받을 수 있다")
    void reissue_Success() throws InterruptedException {
        // given
        String email = "reissue@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        memberService.signup(signupRequestDto);

        // 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        LoginResponseDto loginResponse = memberService.login(new LoginRequestDto(email, "password123"));
        String refreshToken = loginResponse.getRefreshToken();

        // 토큰 발급 시간 차이를 두기 위해 잠시 대기
        Thread.sleep(1000);

        // when
        TokenResponseDto tokenResponse = memberService.reissue(refreshToken);

        // then
        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getRefreshToken()).isNotNull();
        assertThat(tokenResponse.getRefreshToken()).isNotEqualTo(refreshToken); // Rotation 확인

        // DB에 새로운 Refresh Token이 저장되었는지 확인
        RefreshToken savedRefreshToken =
                refreshTokenRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(savedRefreshToken.getToken()).isEqualTo(tokenResponse.getRefreshToken());
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급 시도 시 예외가 발생하고 DB에서 삭제된다")
    void reissue_ExpiredToken() {
        // given
        SignupRequestDto signupRequestDto = new SignupRequestDto("expired@example.com", "password123");
        memberService.signup(signupRequestDto);
        Member member = memberRepository.findByEmail("expired@example.com").orElseThrow();

        // 만료된 토큰 생성 및 DB 저장
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret(jwtProperties.getSecret());
        expiredProperties.setRefreshExpiration(-1000L); // 만료 설정
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
        String expiredRefreshToken = expiredProvider.generateRefreshToken(member.getId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .memberId(member.getId())
                .token(expiredRefreshToken)
                .expiryDate(Instant.now().minusSeconds(10))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // when & then
        assertThatThrownBy(() -> memberService.reissue(expiredRefreshToken))
                .isInstanceOf(InvalidCredentialsException.class);

        // DB에서 삭제되었는지 확인
        assertThat(refreshTokenRepository.findByMemberId(member.getId())).isEmpty();
    }

    @Test
    @DisplayName("DB에 없는 Refresh Token으로 재발급 시도 시 예외가 발생한다")
    void reissue_TokenNotFoundInDB() {
        // given
        SignupRequestDto signupRequestDto = new SignupRequestDto("notfound@example.com", "password123");
        memberService.signup(signupRequestDto);
        Member member = memberRepository.findByEmail("notfound@example.com").orElseThrow();

        // 유효한 토큰이지만 DB에는 저장하지 않음
        String validRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        // when & then
        assertThatThrownBy(() -> memberService.reissue(validRefreshToken))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
