package com.finsentinel.user.service;

import com.finsentinel.role.entity.Role;
import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.repository.RoleRepository;
import com.finsentinel.role.repository.UserRoleRepository;
import com.finsentinel.user.dto.AdminUserResponse;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserManagementAudit;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserManagementAuditRepository;
import com.finsentinel.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserManagementAuditRepository auditRepository;

    public AdminUserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserManagementAuditRepository auditRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {

        List<AppUser> users = userRepository.findAll();

        List<UUID> userIds = users.stream()
                .map(AppUser::getId)
                .toList();

        List<UserRole> mappings = userIds.isEmpty()
                ? List.of()
                : userRoleRepository.findByUser_IdIn(userIds);

        Map<UUID, List<String>> rolesByUser = mappings.stream()
                .collect(
                        Collectors.groupingBy(
                                mapping -> mapping.getUser().getId(),
                                Collectors.mapping(
                                        mapping -> mapping.getRole().getName(),
                                        Collectors.toList()
                                )
                        )
                );

        return users.stream()
                .map(user -> AdminUserResponse.from(
                        user,
                        rolesByUser.getOrDefault(user.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID userId) {

        AppUser user = findUser(userId);

        List<String> roles = getUserRoles(userId);

        return AdminUserResponse.from(user, roles);
    }

    @Transactional
    public AdminUserResponse assignRole(
            UUID actorId,
            UUID targetUserId,
            String roleName
    ) {

        AppUser targetUser = findUser(targetUserId);

        String normalizedRoleName = roleName.trim().toUpperCase();

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role does not exist: " + normalizedRoleName
                ));

        if (userRoleRepository.existsByUser_IdAndRole_Name(
                targetUserId,
                normalizedRoleName
        )) {
            throw new IllegalArgumentException(
                    "User already has this role"
            );
        }

        userRoleRepository.save(
                new UserRole(targetUser, role)
        );

        recordAudit(
                actorId,
                targetUserId,
                "ROLE_ASSIGNED",
                Map.of("role", normalizedRoleName)
        );

        return getUserById(targetUserId);
    }

    @Transactional
    public AdminUserResponse removeRole(
            UUID actorId,
            UUID targetUserId,
            String roleName
    ) {

        String normalizedRoleName = roleName.trim().toUpperCase();

        if (actorId.equals(targetUserId)
                && normalizedRoleName.equals("ADMIN")) {
            throw new IllegalArgumentException(
                    "You cannot remove your own ADMIN role"
            );
        }

        findUser(targetUserId);

        roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role does not exist: " + normalizedRoleName
                ));

        UserRole mapping = userRoleRepository.findByUser_Id(targetUserId)
                .stream()
                .filter(userRole -> userRole.getRole()
                        .getName()
                        .equals(normalizedRoleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "User does not have this role"
                ));

        if (normalizedRoleName.equals("ADMIN")) {

            List<UserRole> admins =
                    userRoleRepository.findByRole_Name("ADMIN");

            if (admins.size() <= 1) {
                throw new IllegalArgumentException(
                        "Cannot remove the last ADMIN role"
                );
            }
        }

        userRoleRepository.delete(mapping);

        recordAudit(
                actorId,
                targetUserId,
                "ROLE_REMOVED",
                Map.of("role", normalizedRoleName)
        );

        return getUserById(targetUserId);
    }

    @Transactional
    public AdminUserResponse suspendUser(
            UUID actorId,
            UUID targetUserId
    ) {

        if (actorId.equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "You cannot suspend your own account"
            );
        }

        AppUser user = findUser(targetUserId);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "User is already suspended"
            );
        }

        if (user.getStatus() == UserStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "Closed accounts cannot be suspended"
            );
        }

        String previousStatus = user.getStatus().name();

        user.setStatus(UserStatus.SUSPENDED);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        recordAudit(
                actorId,
                targetUserId,
                "USER_SUSPENDED",
                Map.of("previousStatus", previousStatus)
        );

        return getUserById(targetUserId);
    }

    @Transactional
    public AdminUserResponse reactivateUser(
            UUID actorId,
            UUID targetUserId
    ) {

        AppUser user = findUser(targetUserId);

        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "Only suspended users can be reactivated"
            );
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Cannot reactivate an unverified user"
            );
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        recordAudit(
                actorId,
                targetUserId,
                "USER_REACTIVATED",
                Map.of("status", "ACTIVE")
        );

        return getUserById(targetUserId);
    }

    private AppUser findUser(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"
                ));
    }

    private List<String> getUserRoles(UUID userId) {

        return userRoleRepository.findByUser_Id(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
    }

    private void recordAudit(
            UUID actorId,
            UUID targetUserId,
            String action,
            Map<String, Object> details
    ) {

        String detailsJson = toJson(details);

        auditRepository.save(
                new UserManagementAudit(
                        actorId,
                        targetUserId,
                        action,
                        detailsJson
                )
        );
    }

    private String toJson(Map<String, Object> details) {

        StringBuilder json = new StringBuilder("{");

        boolean first = true;

        for (Map.Entry<String, Object> entry : details.entrySet()) {

            if (!first) {
                json.append(",");
            }

            first = false;

            json.append("\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\":\"")
                    .append(escapeJson(String.valueOf(entry.getValue())))
                    .append("\"");
        }

        json.append("}");

        return json.toString();
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}