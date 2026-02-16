package com.side.shop.member.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("일반 회원 생성 시 USER 역할이 부여된다")
    void createUser() {
        // given
        String email = "user@example.com";
        String password = "password123";

        // when
        Member member = Member.createUser(email, password, passwordEncoder);

        // then
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.isEmailVerified()).isFalse();
        assertThat(member.getPassword()).isNotEqualTo(password); // 암호화되어야 함
    }

    @Test
    @DisplayName("관리자 생성 시 ADMIN 역할이 부여되고 이메일 인증은 true다")
    void createAdmin() {
        // given
        String email = "admin@example.com";
        String password = "admin123";

        // when
        Member member = Member.createAdmin(email, password, passwordEncoder);

        // then
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(member.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("비밀번호가 일치하면 true를 반환한다")
    void matchesPassword_Success() {
        // given
        String rawPassword = "password123";
        Member member = Member.createUser("user@example.com", rawPassword, passwordEncoder);

        // when
        boolean matches = member.matchesPassword(rawPassword, passwordEncoder);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 false를 반환한다")
    void matchesPassword_Fail() {
        // given
        Member member = Member.createUser("user@example.com", "password123", passwordEncoder);

        // when
        boolean matches = member.matchesPassword("wrongpassword", passwordEncoder);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("비밀번호를 변경할 수 있다")
    void changePassword() {
        // given
        String oldPassword = "oldpassword";
        String newPassword = "newpassword";
        Member member = Member.createUser("user@example.com", oldPassword, passwordEncoder);

        // when
        member.changePassword(newPassword, passwordEncoder);

        // then
        assertThat(member.matchesPassword(newPassword, passwordEncoder)).isTrue();
        assertThat(member.matchesPassword(oldPassword, passwordEncoder)).isFalse();
    }

    @Test
    @DisplayName("이메일 인증을 완료할 수 있다")
    void verify() {
        // given
        Member member = Member.createUser("user@example.com", "password123", passwordEncoder);
        member.generateVerificationToken("token123");

        // when
        member.verify();

        // then
        assertThat(member.isEmailVerified()).isTrue();
        assertThat(member.getVerificationToken()).isNull();
    }

    @Test
    @DisplayName("이미 인증된 회원은 다시 인증할 수 없다")
    void verify_AlreadyVerified() {
        // given
        Member member = Member.createAdmin("admin@example.com", "password123", passwordEncoder);

        // when & then
        assertThatThrownBy(member::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 인증된 회원입니다.");
    }

    @Test
    @DisplayName("인증 토큰을 생성할 수 있다")
    void generateVerificationToken() {
        // given
        Member member = Member.createUser("user@example.com", "password123", passwordEncoder);
        String token = "token123";

        // when
        member.generateVerificationToken(token);

        // then
        assertThat(member.getVerificationToken()).isEqualTo(token);
    }
}
