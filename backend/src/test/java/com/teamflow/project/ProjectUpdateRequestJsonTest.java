package com.teamflow.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamflow.project.dto.ProjectUpdateRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ProjectUpdateRequestJsonTest {

    private final JsonMapper objectMapper = new JsonMapper();

    @Test
    void clearDateFlag_isTrackedAsPresent() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                "{\"clearStartDate\":true,\"endDate\":\"2026-10-20\"}", ProjectUpdateRequest.class);

        assertThat(request.hasStartDate()).isTrue();
        assertThat(request.isClearStartDate()).isTrue();
        assertThat(request.getStartDate()).isNull();
        assertThat(request.hasEndDate()).isTrue();
        assertThat(request.getEndDate()).hasToString("2026-10-20");
    }

    @Test
    void omittedDate_isTrackedAsAbsent() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue("{}", ProjectUpdateRequest.class);

        assertThat(request.hasStartDate()).isFalse();
        assertThat(request.hasEndDate()).isFalse();
    }
}
