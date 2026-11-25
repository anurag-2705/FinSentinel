package com.finsentinel.user.controller;

import com.finsentinel.user.dto.AdminUserResponse;
import com.finsentinel.user.service.AdminUserManagementService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(
            AdminUserManagementService adminUserManagementService
    ) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                adminUserManagementService.getAllUsers()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                adminUserManagementService.getUserById(id)
        );
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<AdminUserResponse> assignRole(
            @PathVariable UUID id,
            @RequestParam String roleName,
            Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                adminUserManagementService.assignRole(
                        actorId, id, roleName
                )
        );
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<AdminUserResponse> removeRole(
            @PathVariable UUID id,
            @PathVariable String roleName,
            Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                adminUserManagementService.removeRole(
                        actorId, id, roleName
                )
        );
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AdminUserResponse> suspendUser(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                adminUserManagementService.suspendUser(
                        actorId, id
                )
        );
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<AdminUserResponse> reactivateUser(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                adminUserManagementService.reactivateUser(
                        actorId, id
                )
        );
    }
}