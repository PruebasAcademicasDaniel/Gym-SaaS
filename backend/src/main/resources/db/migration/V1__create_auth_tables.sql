CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role          VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'GYM_ADMIN', 'TRAINER', 'MEMBER')),
    gym_id        UUID,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL
);
-- gym_id no tiene FK todavia: la tabla gym recien se crea en la Fase 5.
-- Cuando exista, se agrega la constraint en una migracion nueva (no se edita esta).

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);

CREATE TABLE audit_log (
    id            UUID PRIMARY KEY,
    gym_id        UUID,
    actor_user_id UUID,
    action        VARCHAR(50) NOT NULL,
    detail        TEXT,
    created_at    TIMESTAMPTZ NOT NULL
);
-- actor_user_id sin FK a proposito: un registro de auditoria debe sobrevivir aunque el usuario se borre.
CREATE INDEX idx_audit_log_gym_id ON audit_log (gym_id);
