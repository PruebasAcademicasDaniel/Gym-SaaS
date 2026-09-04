CREATE TABLE member (
    id         UUID PRIMARY KEY,
    gym_id     UUID NOT NULL REFERENCES gym (id),
    first_name TEXT NOT NULL,
    last_name  TEXT NOT NULL,
    email      TEXT,
    phone      TEXT,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

-- Toda query contra member va a filtrar por gym_id (Hibernate @TenantId, Fase 4) —
-- el índice es para esas queries, no para el email/teléfono (sin unicidad: son
-- datos de contacto, no una credencial de login como en app_user).
CREATE INDEX idx_member_gym_id ON member (gym_id);
