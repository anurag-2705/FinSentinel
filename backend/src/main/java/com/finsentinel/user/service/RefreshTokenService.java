package com.finsentinel.user.service;

import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.repository.UserRoleRepository;
import com.finsentinel.security.JwtService;
import com.finsentinel.user.dto.LoginResponse;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.RefreshToken;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.RefreshTokenRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtService jwtService;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRoleRepository userRoleRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String createRefreshToken(AppUser user) {

        String rawToken = generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(
                Instant.now().plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public LoginResponse rotateRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken oldToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid refresh token"));

        if (oldToken.getRevokedAt() != null) {
            throw new BadCredentialsException("Refresh token has already been used");
        }

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        AppUser user = oldToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is not active");
        }

        String newRawToken = generateRawToken();

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setTokenHash(hashToken(newRawToken));
        newToken.setExpiresAt(
                Instant.now().plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS)
        );

        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);

        oldToken.setRevokedAt(Instant.now());
        oldToken.setReplacedBy(savedNewToken);

        refreshTokenRepository.save(oldToken);

        List<String> roles = userRoleRepository
                .findByUser_Id(user.getId())
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .toList();

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        return new LoginResponse(
        accessToken,
        newRawToken,
        "Bearer",
        900,
        604800,
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        roles
);
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null) {
            throw new BadCredentialsException("Refresh token has already been revoked");
        }

        refreshToken.setRevokedAt(Instant.now());

        refreshTokenRepository.save(refreshToken);
    }

    private String generateRawToken() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash refresh token", exception
            );
        }
    }
}