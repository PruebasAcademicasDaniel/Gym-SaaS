CREATE TABLE notification (
    id            UUID PRIMARY KEY,
    gym_id        UUID NOT NULL REFERENCES gym (id),
    member_id     UUID NOT NULL REFERENCES member (id),
    membership_id UUID NOT NULL REFERENCES membership (id),
    type          VARCHAR(30) NOT NULL CHECK (type IN ('MEMBERSHIP_EXPIRING_SOON')),
    message       TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

-- Un aviso de vencimiento por membresía, para siempre (la fecha de fin no
-- cambia una vez contratada) — así se evita reenviar el mismo recordatorio
-- cada vez que corre el scheduler, sin necesitar una ventana de fechas.
CREATE UNIQUE INDEX idx_notification_membership_type ON notification (membership_id, type);
CREATE INDEX idx_notification_gym_id ON notification (gym_id);
