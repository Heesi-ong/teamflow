package com.teamflow.activity;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 05-backend-architecture.md §3.1: "부가 효과" 흐름은 Event로 구독하여 task 모듈이 activity 구현을 몰라도 되게 한다. */
@Component
public class ActivityEventListener {

    private final ActivityLogService activityLogService;

    public ActivityEventListener(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        activityLogService.record(ActivityActionType.TASK_CREATED, event.projectId(), event.actorId(),
                "Task \"" + event.taskTitle() + "\" 생성됨");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        activityLogService.record(ActivityActionType.TASK_STATUS_CHANGED, event.projectId(), event.actorId(),
                "Task \"" + event.taskTitle() + "\" 상태 변경: " + event.beforeStatus() + " → " + event.afterStatus());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        activityLogService.record(ActivityActionType.COMMENT_ADDED, event.projectId(), event.actorId(),
                "Task \"" + event.taskTitle() + "\"에 댓글이 작성됨");
    }
}
