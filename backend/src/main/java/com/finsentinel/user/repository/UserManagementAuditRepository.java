package com.finsentinel.user.repository;

import com.finsentinel.user.entity.UserManagementAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserManagementAuditRepository
        extends JpaRepository<UserManagementAudit, UUID> {

    List<UserManagementAudit> findByTargetUserIdOrderByCreatedAtDesc(
            UUID targetUserId
    );

    List<UserManagementAudit> findByActorUserIdOrderByCreatedAtDesc(
            UUID actorUserId
    );
}