package com.side.shop.member.application;

import com.side.shop.member.domain.Member;
import com.side.shop.member.exception.DuplicateEmailException;
import com.side.shop.member.exception.InvalidCredentialsException;
import com.side.shop.member.exception.MemberNotFoundException;
import com.side.shop.member.infrastructure.MemberRepository;
import com.side.shop.member.presentation.dto.LoginRequest;
import com.side.shop.member.presentation.dto.LoginResponse;
import com.side.shop.member.presentation.dto.SignupRequest;
import com.side.shop.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public void signup(SignupRequest request) {
        // 1. 중복 이메일 검증
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // 2. Member 엔티티 생성 (비즈니스 로직은 Entity에)
        Member member = Member.createUser(request.getEmail(), request.getPassword(), passwordEncoder);

        // 3. 저장
        memberRepository.save(member);
    }

    /**
     * 로그인
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 회원 조회
        Member member =
                memberRepository.findByEmail(request.getEmail()).orElseThrow(() -> new InvalidCredentialsException());

        // 2. 비밀번호 검증 (Entity의 비즈니스 로직 사용)
        if (!member.matchesPassword(request.getPassword(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(
                member.getEmail(), member.getRole().name());

        // 4. 응답 DTO 생성
        return new LoginResponse(
                accessToken, member.getEmail(), member.getRole().name());
    }

    /**
     * 이메일로 회원 조회 (추후 사용)
     */
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new MemberNotFoundException(email));
    }
}
