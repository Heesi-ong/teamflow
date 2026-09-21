CREATE TABLE tasks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    author_id   BIGINT       NOT NULL REFERENCES users (id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                    CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM'
                    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    start_date  DATE,
    due_date    DATE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);

CREATE TABLE task_assignees (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id     BIGINT      NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_task_assignees_task_user ON task_assignees (task_id, user_id);

CREATE TABLE task_checklists (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id    BIGINT       NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    content    VARCHAR(500) NOT NULL,
    is_done    BOOLEAN      NOT NULL DEFAULT false,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE activity_logs (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    actor_id    BIGINT      NOT NULL REFERENCES users (id),
    action_type VARCHAR(50) NOT NULL,
    description TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_logs_project_created ON activity_logs (project_id, created_at DESC);
