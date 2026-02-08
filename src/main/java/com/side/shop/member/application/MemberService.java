package com.side.shop.member.application;

import com.side.shop.member.domain.Member;
import com.side.shop.member.exception.DuplicateEmailException;
import com.side.shop.member.exception.InvalidCredentialsException;
import com.side.shop.member.exception.MemberNotFoundException;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * 회원가입
     */
    @Transactional
    public void signup(SignupRequestDto request) {
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
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        // 1. 회원 조회
        Member member =
                memberRepository.findByEmail(request.getEmail()).orElseThrow(() -> new InvalidCredentialsException());

        // 2. 비밀번호 검증 (Entity의 비즈니스 로직 사용)
        if (!member.matchesPassword(request.getPassword(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(
                member.getId(), member.getEmail(), member.getRole().name());
                member.getEmail(), member.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getEmail());

        // 4. Refresh Token 저장
        Instant expiryDate = Instant.now().plusMillis(jwtProperties.getRefreshExpiration());
        RefreshToken refreshTokenEntity =
                refreshTokenRepository.findByUserEmail(member.getEmail()).orElse(null);

        if (refreshTokenEntity != null) {
            refreshTokenEntity.updateToken(refreshToken, expiryDate);
        } else {
            refreshTokenEntity = RefreshToken.builder()
                    .userEmail(member.getEmail())
                    .token(refreshToken)
                    .expiryDate(expiryDate)
                    .build();
            refreshTokenRepository.save(refreshTokenEntity);
        }

        // 5. 응답 DTO 생성
        return new LoginResponseDto(
                accessToken, refreshToken, member.getEmail(), member.getRole().name());
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public TokenResponseDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException(); // 적절한 예외로 변경 필요
        }

        // 2. DB에서 토큰 조회
        RefreshToken tokenEntity = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException()); // 적절한 예외로 변경 필요

        // 3. 만료 확인 (DB에 저장된 만료 시간)
        if (tokenEntity.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(tokenEntity);
            throw new InvalidCredentialsException(); // 적절한 예외로 변경 필요
        }

        // 4. 사용자 조회
        Member member = memberRepository
                .findByEmail(tokenEntity.getUserEmail())
                .orElseThrow(() -> new MemberNotFoundException(tokenEntity.getUserEmail()));

        // 5. 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.generateToken(
                member.getEmail(), member.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getEmail());

        // 6. Refresh Token 업데이트 (Rotation)
        Instant expiryDate = Instant.now().plusMillis(jwtProperties.getRefreshExpiration());
        tokenEntity.updateToken(newRefreshToken, expiryDate);

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 이메일로 회원 조회 (추후 사용)
     */
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new MemberNotFoundException(email));
    }
}
