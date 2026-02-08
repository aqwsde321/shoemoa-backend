package com.side.shop.member.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private String email;
    private String role;
}
