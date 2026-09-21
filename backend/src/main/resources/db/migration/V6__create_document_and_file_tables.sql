CREATE TABLE documents (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    author_id  BIGINT       NOT NULL REFERENCES users (id),
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE project_files (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    task_id      BIGINT       REFERENCES tasks (id) ON DELETE SET NULL,
    uploader_id  BIGINT       NOT NULL REFERENCES users (id),
    file_name    VARCHAR(255) NOT NULL,
    s3_key       VARCHAR(500) NOT NULL,
    file_size    BIGINT       NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_project_files_s3_key ON project_files (s3_key);
