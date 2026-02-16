package com.side.shop.member.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.shop.member.domain.Member;
import com.side.shop.member.infrastructure.MemberRepository;
import com.side.shop.member.presentation.dto.LoginRequestDto;
import com.side.shop.member.presentation.dto.SignupRequestDto;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void signup_Success() throws Exception {
        // given
        SignupRequestDto request = new SignupRequestDto("newuser@example.com", "password123");

        // when & then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입 요청이 완료되었습니다. 이메일을 확인하여 인증을 완료해주세요."));
    }

    @Test
    @DisplayName("이메일 인증 API 성공")
    void verifyEmail_Success() throws Exception {
        // given
        String email = "verify@example.com";
        SignupRequestDto request = new SignupRequestDto(email, "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        Member member = memberRepository.findByEmail(email).orElseThrow();
        String token = member.getVerificationToken();

        // when & then
        mockMvc.perform(get("/api/members/verify-email").param("email", email).param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));
    }

    @Test
    @DisplayName("회원가입 API - 이메일 형식 검증 실패")
    void signup_InvalidEmail() throws Exception {
        // given
        SignupRequestDto request = new SignupRequestDto("invalid-email", "password123");

        // when & then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("회원가입 API - 비밀번호 길이 검증 실패")
    void signup_ShortPassword() throws Exception {
        // given
        SignupRequestDto request = new SignupRequestDto("user@example.com", "short");

        // when & then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.password").value("비밀번호는 최소 8자 이상이어야 합니다."));
    }

    @Test
    @DisplayName("회원가입 API - 중복 이메일")
    void signup_DuplicateEmail() throws Exception {
        // given
        SignupRequestDto request1 = new SignupRequestDto("duplicate@example.com", "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        SignupRequestDto request2 = new SignupRequestDto("duplicate@example.com", "password456");

        // when & then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("로그인 API 성공")
    void login_Success() throws Exception {
        // given
        String email = "loginuser@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

        // 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        LoginRequestDto loginRequestDto = new LoginRequestDto(email, "password123");

        // when & then
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist()) // Body에 없어야 함
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(cookie().exists("refreshToken")) // Cookie에 있어야 함
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true));
    }

    @Test
    @DisplayName("로그인 API - 잘못된 비밀번호")
    void login_WrongPassword() throws Exception {
        // given
        String email = "user@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

        // 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        LoginRequestDto loginRequestDto = new LoginRequestDto(email, "wrongpassword");

        // when & then
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("토큰 재발급 API 성공")
    void reissue_Success() throws Exception {
        // given
        // 1. 회원가입
        String email = "reissue@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

        // 2. 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        // 3. 로그인하여 Refresh Token 획득
        LoginRequestDto loginRequestDto = new LoginRequestDto(email, "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");
        String refreshToken = refreshTokenCookie.getValue();

        // 토큰 발급 시간 차이를 두기 위해 잠시 대기
        Thread.sleep(1000);

        // when & then
        // 4. 재발급 요청
        mockMvc.perform(post("/api/members/reissue").cookie(new Cookie("refreshToken", refreshToken))) // 쿠키 전송
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist()) // Body에 없어야 함
                .andExpect(cookie().exists("refreshToken")) // 새 쿠키 확인
                .andExpect(cookie().value("refreshToken", org.hamcrest.Matchers.not(refreshToken))); // Rotation 확인
    }

    @Test
    @DisplayName("토큰 재발급 API - Refresh Token 쿠키 누락 시 실패")
    void reissue_MissingToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/members/reissue")) // 쿠키 없이 요청
                .andExpect(status().isBadRequest()); // MissingRequestCookieException -> 400
    }
}
