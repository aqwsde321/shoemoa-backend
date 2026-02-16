package com.side.shop.common.infrastructure.mail;

import com.side.shop.common.application.MailService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class FakeMailService implements MailService {

    @Override
    public void sendEmail(String to, String subject, String text) {
        System.out.println("[TEST] Fake email sent to: " + to);
        System.out.println("[TEST] Subject: " + subject);
        System.out.println("[TEST] Content: " + text);
    }
}
