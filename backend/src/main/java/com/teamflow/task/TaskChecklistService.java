package com.teamflow.task;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.task.dto.TaskChecklistCreateRequest;
import com.teamflow.task.dto.TaskChecklistResponse;
import com.teamflow.task.dto.TaskChecklistUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 03-functional-specification.md §3.10, 08-api-specification.md §4 checklist 엔드포인트. */
@Service
public class TaskChecklistService {

    private final TaskRepository taskRepository;
    private final TaskChecklistRepository taskChecklistRepository;
    private final ProjectMemberService projectMemberService;

    public TaskChecklistService(TaskRepository taskRepository, TaskChecklistRepository taskChecklistRepository,
            ProjectMemberService projectMemberService) {
        this.taskRepository = taskRepository;
        this.taskChecklistRepository = taskChecklistRepository;
        this.projectMemberService = projectMemberService;
    }

    @Transactional
    public TaskChecklistResponse add(Long taskId, Long userId, TaskChecklistCreateRequest request) {
        Task task = requireTask(taskId, userId);
        int nextOrder = taskChecklistRepository.countByTaskId(taskId);
        TaskChecklist checklist = taskChecklistRepository.save(new TaskChecklist(task.getId(), request.content(), nextOrder));
        return TaskChecklistResponse.from(checklist);
    }

    @Transactional
    public TaskChecklistResponse update(Long taskId, Long checklistId, Long userId, TaskChecklistUpdateRequest request) {
        requireTask(taskId, userId);
        // content는 부분 업데이트(체크박스만 토글할 때는 null로 옴)라 @NotBlank를 못 쓴다 — 값이
        // 왔을 때만 공백이 아닌지 본다.
        if (request.content() != null && request.content().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        TaskChecklist checklist = findInTask(taskId, checklistId);
        checklist.update(request.content(), request.isDone());
        return TaskChecklistResponse.from(checklist);
    }

    @Transactional
    public void delete(Long taskId, Long checklistId, Long userId) {
        requireTask(taskId, userId);
        TaskChecklist checklist = findInTask(taskId, checklistId);
        taskChecklistRepository.delete(checklist);
    }

    private Task requireTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        projectMemberService.requireAtLeast(task.getProjectId(), userId, ProjectRole.MEMBER);
        return task;
    }

    private TaskChecklist findInTask(Long taskId, Long checklistId) {
        TaskChecklist checklist = taskChecklistRepository.findById(checklistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_NOT_FOUND));
        if (!checklist.getTaskId().equals(taskId)) {
            throw new BusinessException(ErrorCode.CHECKLIST_NOT_FOUND);
        }
        return checklist;
    }
}
