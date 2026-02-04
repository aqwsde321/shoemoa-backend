package com.side.shop.member.application;

import static org.assertj.core.api.Assertions.*;

import com.side.shop.member.domain.Member;
import com.side.shop.member.domain.MemberRole;
import com.side.shop.member.exception.DuplicateEmailException;
import com.side.shop.member.exception.InvalidCredentialsException;
import com.side.shop.member.infrastructure.MemberRepository;
import com.side.shop.member.presentation.dto.LoginRequest;
import com.side.shop.member.presentation.dto.LoginResponse;
import com.side.shop.member.presentation.dto.SignupRequest;
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

    @Test
    @DisplayName("회원가입이 성공하면 DB에 저장된다")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest("newuser@example.com", "password123");

        // when
        memberService.signup(request);

        // then
        Member savedMember = memberRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.USER);
        assertThat(savedMember.getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
    void signup_DuplicateEmail() {
        // given
        SignupRequest request1 = new SignupRequest("duplicate@example.com", "password123");
        memberService.signup(request1);

        SignupRequest request2 = new SignupRequest("duplicate@example.com", "password456");

        // when & then
        assertThatThrownBy(() -> memberService.signup(request2))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("duplicate@example.com");
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인하면 JWT를 받는다")
    void login_Success() {
        // given
        SignupRequest signupRequest = new SignupRequest("loginuser@example.com", "password123");
        memberService.signup(signupRequest);

        LoginRequest loginRequest = new LoginRequest("loginuser@example.com", "password123");

        // when
        LoginResponse response = memberService.login(loginRequest);

        // then
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("loginuser@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
    void login_EmailNotFound() {
        // given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        // when & then
        assertThatThrownBy(() -> memberService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외가 발생한다")
    void login_WrongPassword() {
        // given
        SignupRequest signupRequest = new SignupRequest("user@example.com", "password123");
        memberService.signup(signupRequest);

        LoginRequest loginRequest = new LoginRequest("user@example.com", "wrongpassword");

        // when & then
        assertThatThrownBy(() -> memberService.login(loginRequest)).isInstanceOf(InvalidCredentialsException.class);
    }
}
