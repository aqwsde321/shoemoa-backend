package com.side.shop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                    "price": 100000,
                    "description": "스트릿 패션"
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
    @DisplayName("인증 없이 상품 등록 시도하면 401 Unauthorized")
    void noAuth_CannotCreateProduct() throws Exception {
        // given
        String productJson =
                """
                {
                    "name": "퓨마 클라이드",
                    "price": 90000,
                    "description": "레트로 스타일"
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
                    "price": 80000,
                    "description": "편안한 착용감"
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
