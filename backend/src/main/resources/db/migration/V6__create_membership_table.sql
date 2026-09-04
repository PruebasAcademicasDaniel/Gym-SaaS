CREATE TABLE membership (
    id         UUID PRIMARY KEY,
    gym_id     UUID NOT NULL REFERENCES gym (id),
    member_id  UUID NOT NULL REFERENCES member (id),
    plan_id    UUID NOT NULL REFERENCES plan (id),
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL,
    -- EXPIRED nunca se persiste — se deriva comparando end_date contra hoy (ver Membership.getEffectiveStatus).
    -- El CHECK igual acepta el valor por si algún día se decide materializarlo (ej. para reportes).
    status     VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_membership_gym_id ON membership (gym_id);
CREATE INDEX idx_membership_member_id ON membership (member_id);
