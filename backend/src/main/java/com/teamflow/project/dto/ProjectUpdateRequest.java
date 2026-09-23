package com.teamflow.project.dto;

import com.teamflow.project.ProjectStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 프로젝트 부분 수정 요청.
 *
 * 날짜는 JSON 필드 생략(기존 값 유지)과 명시적인 삭제를 구분한다.
 * Jackson 3의 null setter 정책에 의존하지 않도록 삭제는 clear 플래그로 표현한다.
 */
public class ProjectUpdateRequest {

    @Size(max = 200)
    private String name;
    private String description;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean clearStartDate;
    private boolean clearEndDate;

    public ProjectUpdateRequest() {
    }

    /** Unit test and programmatic construction helper; null dates mean "not supplied" here. */
    public ProjectUpdateRequest(String name, String description, ProjectStatus status,
            LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean hasStartDate() {
        return clearStartDate || startDate != null;
    }

    public boolean hasEndDate() {
        return clearEndDate || endDate != null;
    }

    public boolean isClearStartDate() {
        return clearStartDate;
    }

    public void setClearStartDate(boolean clearStartDate) {
        this.clearStartDate = clearStartDate;
    }

    public boolean isClearEndDate() {
        return clearEndDate;
    }

    public void setClearEndDate(boolean clearEndDate) {
        this.clearEndDate = clearEndDate;
    }
}
