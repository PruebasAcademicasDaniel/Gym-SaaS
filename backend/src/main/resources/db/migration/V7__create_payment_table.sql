CREATE TABLE payment (
    id            UUID PRIMARY KEY,
    gym_id        UUID NOT NULL REFERENCES gym (id),
    membership_id UUID NOT NULL REFERENCES membership (id),
    amount        NUMERIC(10, 2) NOT NULL,
    method        VARCHAR(20) NOT NULL CHECK (method IN ('CASH', 'CARD', 'TRANSFER', 'OTHER')),
    payment_date  DATE NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_gym_id ON payment (gym_id);
CREATE INDEX idx_payment_membership_id ON payment (membership_id);
