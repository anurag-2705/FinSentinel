package com.finsentinel.user.service;

import com.finsentinel.role.entity.Role;
import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.repository.RoleRepository;
import com.finsentinel.role.repository.UserRoleRepository;

import com.finsentinel.user.dto.RegisterRequest;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final VerificationEmailSender verificationEmailSender;

    public UserRegistrationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            VerificationEmailSender verificationEmailSender
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.verificationEmailSender = verificationEmailSender;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        String phoneNumber = request.phoneNumber();

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            phoneNumber = phoneNumber.trim();

            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException(
                        "An account with this phone number already exists"
                );
            }
        } else {
            phoneNumber = null;
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException(
                        "CUSTOMER role is missing from the database"
                ));

        Instant now = Instant.now();

        AppUser user = new AppUser();

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        user.setEmailVerified(false);
        user.setPhoneVerified(false);

        user.setFailedLoginAttempts(0);
        user.setPasswordChangedAt(now);

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        AppUser savedUser = userRepository.saveAndFlush(user);

        UserRole userRole = new UserRole(savedUser, customerRole);
        userRoleRepository.save(userRole);

        String verificationToken =
                emailVerificationService.createVerificationToken(savedUser);

        verificationEmailSender.sendVerificationEmail(
                savedUser.getEmail(),
                verificationToken
        );

        return savedUser;
    }
}