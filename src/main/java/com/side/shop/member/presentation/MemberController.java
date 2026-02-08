package com.side.shop.member.presentation;

import com.side.shop.member.application.MemberService;
import com.side.shop.member.presentation.dto.LoginRequestDto;
import com.side.shop.member.presentation.dto.LoginResponseDto;
import com.side.shop.member.presentation.dto.SignupRequestDto;
import com.side.shop.member.presentation.dto.TokenRequestDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원가입합니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "회원가입 성공",
                        content = @Content(schema = @Schema(implementation = Map.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)", content = @Content),
                @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일", content = @Content)
            })
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequestDto request) {
        memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "로그인 성공",
                        content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검사 실패)", content = @Content),
                @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)", content = @Content)
            })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = memberService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "토큰 재발급",
            description =
                    "API 요청 시 401 Unauthorized (Error Code: TOKEN_EXPIRED) 응답을 받은 경우, 이 API를 호출하여 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "토큰 재발급 성공",
                        content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 (Refresh Token 누락 등)", content = @Content),
                @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token", content = @Content)
            })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDto> reissue(@Valid @RequestBody TokenRequestDto request) {
        TokenResponseDto response = memberService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
