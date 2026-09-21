package com.teamflow.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_checklists")
public class TaskChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private String content;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TaskChecklist() {
    }

    public TaskChecklist(Long taskId, String content, int sortOrder) {
        this.taskId = taskId;
        this.content = content;
        this.done = false;
        this.sortOrder = sortOrder;
    }

    public void update(String content, Boolean done) {
        if (content != null) {
            this.content = content;
        }
        if (done != null) {
            this.done = done;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getContent() {
        return content;
    }

    public boolean isDone() {
        return done;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
