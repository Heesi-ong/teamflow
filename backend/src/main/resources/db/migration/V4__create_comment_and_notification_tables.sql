CREATE TABLE task_comments (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id    BIGINT      NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id),
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_comments_task ON task_comments (task_id);

CREATE TABLE notifications (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(30)  NOT NULL
                   CHECK (type IN ('TASK_ASSIGNED', 'TASK_STATUS_CHANGED', 'MENTION', 'PROJECT_INVITE',
                                    'COMMENT_ADDED', 'DUE_SOON', 'ANNOUNCEMENT')),
    message    VARCHAR(500) NOT NULL,
    target_url VARCHAR(500),
    is_read    BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at DESC);
