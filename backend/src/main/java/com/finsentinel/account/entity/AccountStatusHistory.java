package com.finsentinel.account.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_status_history")
public class AccountStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "performed_by_user_id", nullable = false)
    private UUID performedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatusAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private AccountStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private AccountStatus newStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AccountStatusHistory() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(UUID performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public AccountStatusAction getAction() {
        return action;
    }

    public void setAction(AccountStatusAction action) {
        this.action = action;
    }

    public AccountStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(AccountStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public AccountStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AccountStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}