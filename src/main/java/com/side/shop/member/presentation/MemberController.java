package com.side.shop.member.presentation;

import com.side.shop.member.application.MemberService;
import com.side.shop.member.presentation.dto.LoginRequestDto;
import com.side.shop.member.presentation.dto.LoginResponseDto;
import com.side.shop.member.presentation.dto.SignupRequestDto;
import com.side.shop.member.presentation.dto.TokenResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원가입을 요청하고 인증 메일을 발송합니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "회원가입 요청 성공",
                        content = @Content(schema = @Schema(implementation = Map.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)", content = @Content),
                @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일", content = @Content)
            })
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequestDto request) {
        memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "회원가입 요청이 완료되었습니다. 이메일을 확인하여 인증을 완료해주세요."));
    }

    @Operation(summary = "이메일 인증", description = "메일로 발송된 링크를 통해 이메일 인증을 완료합니다.")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 토큰 등)"),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
            })
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam("email") String email, @RequestParam("token") String token) {
        memberService.verifyEmail(email, token);
        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하여 JWT Access Token을 발급받습니다. Refresh Token은 HttpOnly 쿠키로 설정됩니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "로그인 성공",
                        content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)", content = @Content),
                @ApiResponse(responseCode = "401", description = "인증 실패 (이메일, 비밀번호 불일치 또는 이메일 미인증)", content = @Content)
            })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = memberService.login(request);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "토큰 재발급", description = "Cookie에 저장된 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "토큰 재발급 성공",
                        content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (Refresh Token 쿠키 누락)", content = @Content),
                @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token", content = @Content)
            })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDto> reissue(@CookieValue("refreshToken") String refreshToken) {
        TokenResponseDto response = memberService.reissue(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
