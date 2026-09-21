package com.teamflow.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "task_assignees")
public class TaskAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    protected TaskAssignee() {
    }

    public TaskAssignee(Long taskId, Long userId) {
        this.taskId = taskId;
        this.userId = userId;
        this.assignedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }
}
