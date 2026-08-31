-- Datos de contacto / ubicación del perfil, agregados después de V003 para evitar un nuke.
-- Se pliegan en V003 cuando se consolide el esquema (y ahí sí se nukea).
--
-- `address`: dirección física (texto libre, alcance = Región de Valparaíso). La ve el retailer
--   para el retiro y el transportista para armar la ruta.
-- `phone`: teléfono de contacto del perfil.
-- Ambos nullable: el perfil se autoprovisiona vacío; la obligatoriedad llega con la UI
-- dedicada de perfil. Geolocalización / multi-región -> improvements.md #2.

ALTER TABLE user_profile ADD COLUMN address VARCHAR(255);
ALTER TABLE user_profile ADD COLUMN phone   VARCHAR(30);
