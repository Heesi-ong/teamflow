CREATE TABLE projects (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PLANNING'
                    CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'ARCHIVED')),
    start_date  DATE,
    end_date    DATE,
    owner_id    BIGINT        NOT NULL REFERENCES users (id),
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_owner ON projects (owner_id);

CREATE TABLE project_members (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'GUEST')),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_project_members_project_user ON project_members (project_id, user_id);

CREATE TABLE invitations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    email       VARCHAR(255),
    token       VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER', 'GUEST')),
    invited_by  BIGINT       NOT NULL REFERENCES users (id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_invitations_token ON invitations (token);
CREATE INDEX idx_invitations_project_status ON invitations (project_id, status);
