package com.teamflow.project;

import com.teamflow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Project() {
    }

    public Project(String name, String description, LocalDate startDate, LocalDate endDate, Long ownerId) {
        this.name = name;
        this.description = description;
        this.status = ProjectStatus.PLANNING;
        this.startDate = startDate;
        this.endDate = endDate;
        this.ownerId = ownerId;
    }

    public void update(String name, String description, ProjectStatus status, LocalDate startDate, LocalDate endDate) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (status != null) {
            this.status = status;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public void changeOwner(Long newOwnerId) {
        this.ownerId = newOwnerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
