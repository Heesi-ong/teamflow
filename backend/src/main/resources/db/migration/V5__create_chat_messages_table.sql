CREATE TABLE chat_messages (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id),
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_project_created ON chat_messages (project_id, created_at DESC);
