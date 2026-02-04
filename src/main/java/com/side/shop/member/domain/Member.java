package com.side.shop.member.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Entity
@Table(name = "members")
public class Member {

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Member() {
        // JPA를 위한 기본 생성자
    }

    // 일반 회원 생성 (정적 팩토리 메서드)
    public static Member createUser(String email, String rawPassword, PasswordEncoder passwordEncoder) {
        Member member = new Member();
        member.email = email;
        member.password = passwordEncoder.encode(rawPassword);
        member.role = MemberRole.USER;
        member.emailVerified = false;
        member.createdAt = LocalDateTime.now();
        member.updatedAt = LocalDateTime.now();
        return member;
    }

    // 관리자 생성 (정적 팩토리 메서드)
    public static Member createAdmin(String email, String rawPassword, PasswordEncoder passwordEncoder) {
        Member member = new Member();
        member.email = email;
        member.password = passwordEncoder.encode(rawPassword);
        member.role = MemberRole.ADMIN;
        member.emailVerified = true; // 관리자는 인증 불필요
        member.createdAt = LocalDateTime.now();
        member.updatedAt = LocalDateTime.now();
        return member;
    }

    // 비즈니스 로직: 비밀번호 검증
    public boolean matchesPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.password);
    }

    // 비즈니스 로직: 비밀번호 변경
    public void changePassword(String newRawPassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(newRawPassword);
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 이메일 인증 (추후 사용)
    public void verify() {
        if (this.emailVerified) {
            throw new IllegalStateException("이미 인증된 회원입니다.");
        }
        this.emailVerified = true;
        this.verificationToken = null;
        this.tokenExpiresAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 인증 토큰 생성 (추후 사용)
    public void generateVerificationToken(String token, int expirationHours) {
        this.verificationToken = token;
        this.tokenExpiresAt = LocalDateTime.now().plusHours(expirationHours);
        this.updatedAt = LocalDateTime.now();
    }
}
