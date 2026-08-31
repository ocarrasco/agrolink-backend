-- Preferencia de entrega del retailer (día + turno AM/PM) sobre la orden. ALTER aditivo
-- para no nukear; se pliega en V004 al consolidar el esquema.
--
-- El MÉTODO de envío (despacho propio / transportista de plataforma) y la solicitud de
-- transporte NO se modelan todavía — ver improvements.md #5. Ambas columnas nullable:
-- la preferencia es opcional al crear la orden.

ALTER TABLE purchase_order ADD COLUMN delivery_day  VARCHAR(10);
ALTER TABLE purchase_order ADD COLUMN delivery_slot VARCHAR(5);
