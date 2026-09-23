package com.teamflow.task.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record TaskAssigneeUpdateRequest(Long assigneeId, @NotNull Long version, boolean clearAssignee) {

    public TaskAssigneeUpdateRequest(Long assigneeId, Long version) {
        this(assigneeId, version, false);
    }

    @AssertTrue(message = "담당자 해제 여부와 담당자 값이 올바르지 않습니다.")
    public boolean isAssigneeSelectionValid() {
        return clearAssignee || assigneeId != null;
    }
}
