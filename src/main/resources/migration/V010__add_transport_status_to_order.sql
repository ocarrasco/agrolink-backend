-- Platform-carrier transport execution (see transporte_carrier.md). The transport leg of a
-- PLATFORM_CARRIER order runs AWAITING_CARRIER -> ASSIGNED -> IN_TRANSIT -> DELIVERED; on
-- DELIVERED the order's own status flips to FULFILLED. Null for PICKUP / SUPPLIER_DELIVERY.

ALTER TABLE purchase_order
    ADD COLUMN transport_status VARCHAR(20);
