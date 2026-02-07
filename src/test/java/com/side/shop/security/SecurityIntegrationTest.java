package com.side.shop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.shop.member.domain.Member;
import com.side.shop.member.infrastructure.MemberRepository;
import com.side.shop.member.presentation.dto.LoginRequestDto;
import com.side.shop.member.presentation.dto.LoginResponseDto;
import com.side.shop.member.presentation.dto.SignupRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("일반 회원은 상품 등록이 불가능하다")
    void user_CannotCreateProduct() throws Exception {
        // given - 일반 회원 가입 및 로그인
        SignupRequestDto signupRequestDto = new SignupRequestDto("user@example.com", "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

        LoginRequestDto loginRequestDto = new LoginRequestDto("user@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        LoginResponseDto loginResponseDto = objectMapper.readValue(responseBody, LoginResponseDto.class);
        String userToken = loginResponseDto.getAccessToken();

        // when & then - 상품 등록 시도 (권한 없음)
        String productJson =
                """
                {
                    "name": "아디다스 슈퍼스타",
                    "brand": "Adidas",
                    "price": 100000,
                    "description": "스트릿 패션",
                    "color": "White"
                }
                """;

        MockMultipartFile data =
                new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes());

        MockMultipartFile image = new MockMultipartFile(
                "images", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

        mockMvc.perform(multipart("/api/products")
                        .file(data)
                        .file(image)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 상품 등록이 가능하다")
    void admin_CanCreateProduct() throws Exception {
        // given - 관리자 계정 생성 및 저장
        String email = "admin@example.com";
        String password = "adminPassword123";
        Member admin = Member.createAdmin(email, password, passwordEncoder);
        memberRepository.save(admin);

        // 로그인하여 토큰 발급
        LoginRequestDto loginRequestDto = new LoginRequestDto(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        LoginResponseDto loginResponseDto = objectMapper.readValue(responseBody, LoginResponseDto.class);
        String adminToken = loginResponseDto.getAccessToken();

        // when & then - 상품 등록 시도
        String productJson =
                """
                {
                    "name": "나이키 에어포스",
                    "brand": "Nike",
                    "price": 120000,
                    "description": "클래식한 디자인",
                    "color": "White"
                }
                """;

        MockMultipartFile data =
                new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes());

        MockMultipartFile image = new MockMultipartFile(
                "images", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

        mockMvc.perform(multipart("/api/products")
                        .file(data)
                        .file(image)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 상품 등록 시도하면 401 Unauthorized")
    void noAuth_CannotCreateProduct() throws Exception {
        // given
        String productJson =
                """
                {
                    "name": "퓨마 클라이드",
                    "brand": "Puma",
                    "price": 90000,
                    "description": "레트로 스타일",
                    "color": "Black"
                }
                """;

        MockMultipartFile data =
                new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes());

        MockMultipartFile image = new MockMultipartFile(
                "images", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

        // when & then
        mockMvc.perform(multipart("/api/products").file(data).file(image)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("잘못된 토큰으로 요청 시 401 Unauthorized")
    void invalidToken_Returns401() throws Exception {
        // given
        String invalidToken = "invalid.jwt.token";
        String productJson =
                """
                {
                    "name": "리복 클래식",
                    "brand": "Reebok",
                    "price": 80000,
                    "description": "편안한 착용감",
                    "color": "Blue"
                }
                """;

        MockMultipartFile data =
                new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes());

        MockMultipartFile image = new MockMultipartFile(
                "images", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

        // when & then
        mockMvc.perform(multipart("/api/products")
                        .file(data)
                        .file(image)
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("누구나 상품 조회는 가능하다")
    void anyone_CanViewProducts() throws Exception {
        // when & then - 인증 없이 상품 조회
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }
}
