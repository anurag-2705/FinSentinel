package com.finsentinel.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_management_audit")
public class UserManagementAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(nullable = false, length = 50)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String details = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserManagementAudit() {
    }

    public UserManagementAudit(
            UUID actorUserId,
            UUID targetUserId,
            String action,
            String details
    ) {
        this.actorUserId = actorUserId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.details = details == null ? "{}" : details;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}