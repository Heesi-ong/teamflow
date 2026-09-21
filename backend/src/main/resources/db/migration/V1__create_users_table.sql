CREATE TABLE users (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(255)  NOT NULL,
    password_hash      VARCHAR(255)  NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    profile_image_url  VARCHAR(500),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email ON users (email);
