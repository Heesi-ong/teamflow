package com.teamflow.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Invitation() {
    }

    public Invitation(Long projectId, String email, String token, ProjectRole role, Long invitedBy, OffsetDateTime expiresAt) {
        this.projectId = projectId;
        this.email = email;
        this.token = token;
        this.role = role;
        this.invitedBy = invitedBy;
        this.status = InvitationStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isUsable() {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(OffsetDateTime.now());
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
    }

    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public ProjectRole getRole() {
        return role;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
