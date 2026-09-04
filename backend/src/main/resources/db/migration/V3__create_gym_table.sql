CREATE TABLE gym (
    id         UUID PRIMARY KEY,
    name       TEXT NOT NULL,
    slug       TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

-- Cierra el TODO dejado en V1: gym_id no tenía FK porque esta tabla no existía todavía.
-- NULL sigue siendo válido (SUPER_ADMIN no tiene gym), la FK solo valida los no-null.
ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_gym FOREIGN KEY (gym_id) REFERENCES gym (id);
