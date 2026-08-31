-- Nombre de la persona de contacto del perfil (par de `phone`). ALTER aditivo (no nuke);
-- se pliega en V003 al consolidar el esquema.

ALTER TABLE user_profile ADD COLUMN contact_name VARCHAR(120);
