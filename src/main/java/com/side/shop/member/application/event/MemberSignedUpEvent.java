package com.side.shop.member.application.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSignedUpEvent {
    private String email;
    private String verificationToken;
}
