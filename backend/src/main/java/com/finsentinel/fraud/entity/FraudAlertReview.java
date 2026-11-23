package com.finsentinel.fraud.entity;

import com.finsentinel.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_alert_reviews")
public class FraudAlertReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private FraudAlert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyst_id", nullable = false)
    private AppUser analyst;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudReviewAction action;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public FraudAlert getAlert() {
        return alert;
    }

    public void setAlert(FraudAlert alert) {
        this.alert = alert;
    }

    public AppUser getAnalyst() {
        return analyst;
    }

    public void setAnalyst(AppUser analyst) {
        this.analyst = analyst;
    }

    public FraudReviewAction getAction() {
        return action;
    }

    public void setAction(FraudReviewAction action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}