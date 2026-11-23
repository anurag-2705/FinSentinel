package com.finsentinel.user.service;

import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.EmailVerificationToken;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.EmailVerificationTokenRepository;
import com.finsentinel.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String createVerificationToken(AppUser user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setUser(user);
        verificationToken.setTokenHash(hashToken(rawToken));
        verificationToken.setExpiresAt(
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );

        tokenRepository.save(verificationToken);

        return rawToken;
    }

    @Transactional
    public void verifyEmail(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Verification token is required"
            );
        }

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid verification token"
                        ));

        if (verificationToken.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "Verification token has already been used"
            );
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException(
                    "Verification token has expired"
            );
        }

        AppUser user = verificationToken.getUser();

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException(
                    "Account is not awaiting email verification"
            );
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);

        verificationToken.setUsedAt(Instant.now());

        userRepository.save(user);
        tokenRepository.save(verificationToken);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to process verification token",
                    exception
            );
        }
    }
}