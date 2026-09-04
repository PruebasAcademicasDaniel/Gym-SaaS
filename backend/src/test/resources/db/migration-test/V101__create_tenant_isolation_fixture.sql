-- Solo para TenantIsolationIT — nunca corre fuera de tests (ver @TestPropertySource
-- que agrega esta location a spring.flyway.locations). Numerada lejos de V1/V2 para
-- no chocar nunca con migraciones reales de src/main.
CREATE TABLE tenant_isolation_fixture (
    id     UUID PRIMARY KEY,
    gym_id UUID NOT NULL,
    label  TEXT NOT NULL
);
