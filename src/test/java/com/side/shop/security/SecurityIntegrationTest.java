package com.side.shop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.shop.member.domain.Member;
import com.side.shop.member.infrastructure.MemberRepository;
import com.side.shop.member.presentation.dto.LoginRequestDto;
import com.side.shop.member.presentation.dto.LoginResponseDto;
import com.side.shop.member.presentation.dto.SignupRequestDto;
import com.side.shop.member.presentation.dto.TokenResponseDto;
import com.side.shop.security.jwt.JwtProperties;
import com.side.shop.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("일반 회원은 상품 등록이 불가능하다")
    void user_CannotCreateProduct() throws Exception {
        // given - 일반 회원 가입 및 로그인
        String email = "user@example.com";
        SignupRequestDto signupRequestDto = new SignupRequestDto(email, "password123");
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

        // 이메일 인증 처리
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.verify();
        memberRepository.saveAndFlush(member);

        LoginRequestDto loginRequestDto = new LoginRequestDto(email, "password123");
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

    @Test
    @DisplayName("만료된 Access Token으로 요청 시 401 응답 후 Refresh Token으로 재발급받아 성공한다")
    void expiredAccessToken_ReissueAndSucceed() throws Exception {
        // given - 관리자 계정 생성 및 로그인
        String email = "admin_expired@example.com";
        String password = "password123";
        Member admin = Member.createAdmin(email, password, passwordEncoder);
        memberRepository.save(admin);

        LoginRequestDto loginRequestDto = new LoginRequestDto(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");

        // 만료된 Access Token 생성 (테스트를 위해 강제로 만료된 토큰 생성)
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret(jwtProperties.getSecret());
        expiredProperties.setExpiration(-1000L); // 만료 설정
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
        String expiredAccessToken = expiredProvider.generateToken(admin.getId(), email, "ADMIN");

        // when - 1. 만료된 토큰으로 상품 등록 시도 -> 401 실패 예상
        String productJson =
                """
                {
                    "name": "뉴발란스 993",
                    "brand": "New Balance",
                    "price": 250000,
                    "description": "편안한 착화감",
                    "color": "Grey"
                }
                """;
        MockMultipartFile data =
                new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes());
        MockMultipartFile image = new MockMultipartFile(
                "images", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

        mockMvc.perform(multipart("/api/products")
                        .file(data)
                        .file(image)
                        .header("Authorization", "Bearer " + expiredAccessToken))
                .andExpect(status().isUnauthorized()) // 401 Unauthorized
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED")); // ⭐️ 에러 코드가 TOKEN_EXPIRED 인지 검증

        // when - 2. Refresh Token으로 Access Token 재발급
        MvcResult reissueResult = mockMvc.perform(post("/api/members/reissue").cookie(refreshTokenCookie)) // 쿠키 전송
                .andExpect(status().isOk())
                .andReturn();

        String reissueResponseBody = reissueResult.getResponse().getContentAsString();
        TokenResponseDto tokenResponseDto = objectMapper.readValue(reissueResponseBody, TokenResponseDto.class);
        String newAccessToken = tokenResponseDto.getAccessToken();

        // when - 3. 재발급받은 토큰으로 다시 상품 등록 시도 -> 성공 예상
        // MockMultipartFile은 재사용이 안될 수 있으므로 다시 생성
        MockMultipartFile newData =
                new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes());
        MockMultipartFile newImage = new MockMultipartFile(
                "images", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

        mockMvc.perform(multipart("/api/products")
                        .file(newData)
                        .file(newImage)
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());
    }
}
