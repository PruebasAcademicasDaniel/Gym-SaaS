CREATE TABLE plan (
    id            UUID PRIMARY KEY,
    gym_id        UUID NOT NULL REFERENCES gym (id),
    name          TEXT NOT NULL,
    description   TEXT,
    price         NUMERIC(10, 2) NOT NULL,
    duration_days INTEGER NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_plan_gym_id ON plan (gym_id);
