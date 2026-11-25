package com.finsentinel.user.dto;

import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        int failedLoginAttempts,
        Instant lockedUntil,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> roles
) {
    public static AdminUserResponse from(
            AppUser user,
            List<String> roles
    ) {
        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roles
        );
    }
}