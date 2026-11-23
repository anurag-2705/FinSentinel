package com.finsentinel.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class VerificationEmailSender {

    private final String verificationBaseUrl;

    public VerificationEmailSender(
            @Value("${app.security.verification-base-url}")
            String verificationBaseUrl
    ) {
        this.verificationBaseUrl = verificationBaseUrl;
    }

    public void sendVerificationEmail(String email, String token) {

        String verificationLink = verificationBaseUrl
                + "?token=" + token;

        System.out.println("-----------------------------------");
        System.out.println("DEVELOPMENT EMAIL VERIFICATION");
        System.out.println("Recipient: " + email);
        System.out.println("Verification link: " + verificationLink);
        System.out.println("-----------------------------------");
    }
}