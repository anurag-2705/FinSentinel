package com.finsentinel.user.service;

import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.repository.UserRoleRepository;
import com.finsentinel.security.JwtService;
import com.finsentinel.user.dto.LoginRequest;
import com.finsentinel.user.dto.LoginResponse;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException(
                    "Your account is not active. Please complete account verification."
            );
        }

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

        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResponse(
        accessToken,
        refreshToken,
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
}