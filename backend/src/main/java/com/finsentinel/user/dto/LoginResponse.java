package com.finsentinel.user.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshTokenExpiresIn,
        UUID userId,
        String email,
        String firstName,
        String lastName
) {
}