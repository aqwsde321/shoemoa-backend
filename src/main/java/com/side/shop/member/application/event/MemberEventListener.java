package com.side.shop.member.application.event;

import com.side.shop.common.application.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final MailService mailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberSignedUpEvent(MemberSignedUpEvent event) {
        sendVerificationEmail(event.getEmail(), event.getVerificationToken());
    }

    private void sendVerificationEmail(String email, String token) {
        String verificationUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/members/verify-email")
                .queryParam("token", token)
                .queryParam("email", email)
                .build()
                .toUriString();

        String subject = "Shoemoa 회원가입 이메일 인증";
        String text = "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n" + verificationUrl;

        mailService.sendEmail(email, subject, text);
    }
}
