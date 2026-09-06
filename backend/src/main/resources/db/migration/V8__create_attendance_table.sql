CREATE TABLE attendance (
    id          UUID PRIMARY KEY,
    gym_id      UUID NOT NULL REFERENCES gym (id),
    member_id   UUID NOT NULL REFERENCES member (id),
    checked_in_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

-- Un registro por check-in, sin deduplicar ni por día. Si el motor de riesgo
-- (Fase 14) necesita "días distintos asistidos", se agrupa por fecha en esa
-- consulta puntual — no es responsabilidad de este módulo.
CREATE INDEX idx_attendance_gym_id ON attendance (gym_id);
CREATE INDEX idx_attendance_member_id ON attendance (member_id);
