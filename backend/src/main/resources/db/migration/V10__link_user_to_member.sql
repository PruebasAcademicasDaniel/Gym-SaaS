-- Fase 13 (Portal del cliente): vínculo opcional entre un login (app_user) y
-- el socio (member) al que representa. Solo lo usan usuarios con role MEMBER
-- — ver la validación de dominio en User.java. Un socio tiene a lo sumo un
-- login propio (índice único parcial: permite múltiples NULL para
-- GYM_ADMIN/TRAINER/SUPER_ADMIN, que nunca tienen member_id).
ALTER TABLE app_user ADD COLUMN member_id UUID REFERENCES member(id);

CREATE UNIQUE INDEX ux_app_user_member_id ON app_user (member_id) WHERE member_id IS NOT NULL;
