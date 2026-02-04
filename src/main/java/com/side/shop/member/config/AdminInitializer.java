package com.side.shop.member.config;

import com.side.shop.member.domain.Member;
import com.side.shop.member.infrastructure.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 관리자 계정이 이미 존재하는지 확인
        String adminEmail = "admin@shoemoa.com";

        if (memberRepository.existsByEmail(adminEmail)) {
            log.info("관리자 계정이 이미 존재합니다: {}", adminEmail);
            return;
        }

        // 관리자 계정 생성
        Member admin = Member.createAdmin(
                adminEmail,
                "admin1234", // 실제 운영에서는 환경 변수로 관리
                passwordEncoder);

        memberRepository.save(admin);
        log.info("관리자 계정이 생성되었습니다: {}", adminEmail);
        log.info("초기 비밀번호: admin1234 (반드시 변경하세요!)");
    }
}
