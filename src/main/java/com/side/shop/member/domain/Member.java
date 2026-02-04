package com.side.shop.member.domain;

import com.side.shop.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Entity
@Table(name = "members")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime tokenExpiresAt;

    protected Member() {
        // JPA를 위한 기본 생성자
    }

    private Member(String email, String password, MemberRole role, boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.emailVerified = emailVerified;
    }

    // 일반 회원 생성 (정적 팩토리 메서드)
    public static Member createUser(String email, String rawPassword, PasswordEncoder passwordEncoder) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return new Member(email, encodedPassword, MemberRole.USER, false);
    }

    // 관리자 생성 (정적 팩토리 메서드)
    public static Member createAdmin(String email, String rawPassword, PasswordEncoder passwordEncoder) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return new Member(email, encodedPassword, MemberRole.ADMIN, true);
    }

    // 비즈니스 로직: 비밀번호 검증
    public boolean matchesPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.password);
    }

    // 비즈니스 로직: 비밀번호 변경
    public void changePassword(String newRawPassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(newRawPassword);
    }

    // 비즈니스 로직: 이메일 인증 (추후 사용)
    public void verify() {
        if (this.emailVerified) {
            throw new IllegalStateException("이미 인증된 회원입니다.");
        }
        this.emailVerified = true;
        this.verificationToken = null;
        this.tokenExpiresAt = null;
    }

    // 비즈니스 로직: 인증 토큰 생성 (추후 사용)
    public void generateVerificationToken(String token, int expirationHours) {
        this.verificationToken = token;
        this.tokenExpiresAt = LocalDateTime.now().plusHours(expirationHours);
    }
}
