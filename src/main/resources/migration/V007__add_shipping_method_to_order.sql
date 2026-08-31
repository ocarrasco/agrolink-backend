-- Método de entrega de la orden. ALTER aditivo (no nuke); se pliega en V004 al consolidar.
-- Hoy solo se usan PICKUP y SUPPLIER_DELIVERY; PLATFORM_CARRIER se habilita en la iteración
-- de transporte (ver transporte_carrier.md). DEFAULT PICKUP para las filas existentes.

ALTER TABLE purchase_order
    ADD COLUMN shipping_method VARCHAR(20) NOT NULL DEFAULT 'PICKUP';
