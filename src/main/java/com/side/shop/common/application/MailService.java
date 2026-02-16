package com.side.shop.common.application;

public interface MailService {
    void sendEmail(String to, String subject, String text);
}
