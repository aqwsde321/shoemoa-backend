package com.side.shop.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.shop.common.exception.ErrorResponse;
import com.side.shop.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays; // Added for String.split
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // Added import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 어노테이션 사용을 위해
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용하므로)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Session 사용 안 함 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 불필요 경로
                        .requestMatchers(
                                "/api/members/signup",
                                "/api/members/login",
                                "/api/members/reissue",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()

                        // Product 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                        .permitAll()
                        // Product CUD는 ADMIN만 가능
                        .requestMatchers("/api/products/**")
                        .hasRole("ADMIN")

                        // 나머지는 인증 필요
                        .anyRequest()
                        .authenticated())

                // 인증 / 인가 과정에서 발생하는 예외 처리 설정
                .exceptionHandling(
                        exception -> exception
                                .authenticationEntryPoint(authenticationEntryPoint()) // [401 Unauthorized]: 너 누구냐?
                                .accessDeniedHandler(accessDeniedHandler()) // [403 Forbidden]: 누군지는 아는데 권한이 없다
                        )

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증 실패 시 401 Unauthorized 반환
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            String exception = (String) request.getAttribute("exception");

            if ("EXPIRED_TOKEN".equals(exception)) {
                ErrorResponse errorResponse = new ErrorResponse("TOKEN_EXPIRED", "Access Token이 만료되었습니다.");
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, errorResponse);
            } else {
                ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "인증이 필요합니다.");
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, errorResponse);
            }
        };
    }

    /**
     * 권한 없음 시 403 Forbidden 반환
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            ErrorResponse errorResponse = new ErrorResponse("FORBIDDEN", "접근 권한이 없습니다.");
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, errorResponse);
        };
    }

    /**
     * 에러 응답 전송 헬퍼 메서드
     */
    private void sendErrorResponse(HttpServletResponse response, int status, ErrorResponse errorResponse)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(","))); // 프론트엔드 주소
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * PasswordEncoder 빈 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
