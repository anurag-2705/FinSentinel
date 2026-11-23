package com.finsentinel.user.controller;

import com.finsentinel.user.dto.LogoutResponse;

import com.finsentinel.user.dto.RegisterRequest;
import com.finsentinel.user.dto.UserResponse;
import com.finsentinel.user.dto.LoginRequest;
import com.finsentinel.user.dto.LoginResponse;
import com.finsentinel.user.dto.VerifyEmailRequest;
import com.finsentinel.user.dto.RefreshTokenRequest;

import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.service.UserRegistrationService;
import com.finsentinel.user.service.AuthenticationService;
import com.finsentinel.user.service.EmailVerificationService;
import com.finsentinel.user.service.RefreshTokenService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UserRegistrationService registrationService,
            AuthenticationService authenticationService,
            EmailVerificationService emailVerificationService,
            RefreshTokenService refreshTokenService
    ) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AppUser user = registrationService.register(request);
        UserResponse response = UserResponse.from(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        emailVerificationService.verifyEmail(request.token());

        return ResponseEntity.ok(
                "Email verified successfully. Your account is now active."
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginResponse response = refreshTokenService.rotateRefreshToken(
                request.refreshToken()
        );

        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());

        return ResponseEntity.ok(
                new LogoutResponse("Logout successful")
        );
    }
}