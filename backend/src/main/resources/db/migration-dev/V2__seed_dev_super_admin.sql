-- Solo corre con el perfil "dev" (spring.flyway.locations en application-dev.yml).
-- No se aplica nunca sobre un esquema de producción.
--
-- Credenciales de desarrollo: admin@gymflow.dev / GymFlow!Dev2026
INSERT INTO app_user (id, email, password_hash, role, gym_id, enabled, created_at)
VALUES (
    '1c0a0d0c-a91a-4304-a404-b43842747a4e',
    'admin@gymflow.dev',
    '$2a$10$5D9ndgjLNC6jK60.UZk3dOyS6aS8ijkmNKVHMLkNfifZcKuqc1a76',
    'SUPER_ADMIN',
    NULL,
    TRUE,
    now()
);
