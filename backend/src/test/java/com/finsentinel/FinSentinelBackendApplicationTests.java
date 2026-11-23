package com.finsentinel;

import com.finsentinel.role.entity.Role;
import com.finsentinel.role.repository.RoleRepository;
import com.finsentinel.role.repository.UserRoleRepository;

import com.finsentinel.user.dto.RegisterRequest;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;
import com.finsentinel.user.service.UserRegistrationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FinSentinelBackendApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRegistrationService registrationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void applicationContextLoads() {
        assertNotNull(userRepository);
        assertNotNull(roleRepository);
        assertNotNull(registrationService);
    }

    @Test
    void shouldConnectToDatabase() {
        assertNotNull(userRepository.count());
    }

    @Test
    void shouldFindAllDefaultRoles() {
        assertEquals(4, roleRepository.count());
    }

    @Test
    void shouldFindCustomerRole() {
        Optional<Role> customerRole =
                roleRepository.findByName("CUSTOMER");

        assertTrue(customerRole.isPresent());
        assertEquals("CUSTOMER", customerRole.get().getName());
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest(
                "Anurag",
                "Test",
                email,
                null,
                "StrongPassword123!"
        );

        AppUser user = registrationService.register(request);

        assertNotNull(user.getId());
        assertEquals(email, user.getEmail());

        assertEquals(
                UserStatus.PENDING_VERIFICATION,
                user.getStatus()
        );

        assertFalse(user.isEmailVerified());

        assertTrue(
                passwordEncoder.matches(
                        "StrongPassword123!",
                        user.getPasswordHash()
                )
        );

        assertNotEquals(
                "StrongPassword123!",
                user.getPasswordHash()
        );
    }

    @Test
    void shouldAssignCustomerRoleAutomatically() {

        String email = "role-" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest(
                "Test",
                "Customer",
                email,
                null,
                "StrongPassword123!"
        );

        AppUser user = registrationService.register(request);

        assertTrue(
                userRoleRepository.existsByUser_IdAndRole_Name(
                        user.getId(),
                        "CUSTOMER"
                )
        );
    }

    @Test
    void shouldRejectDuplicateEmail() {

        String email = "duplicate-" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest(
                "Test",
                "Customer",
                email,
                null,
                "StrongPassword123!"
        );

        registrationService.register(request);

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(request)
        );
    }
}