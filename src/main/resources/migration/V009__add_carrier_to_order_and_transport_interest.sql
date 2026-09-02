-- Platform-carrier transport, simplified flow (see transporte_carrier.md, mercado abierto sin
-- ofertas de día/precio): the retailer flags PLATFORM_CARRIER on the order, carriers see it and
-- mark interest, the retailer accepts one straight onto the order.

ALTER TABLE purchase_order
    ADD COLUMN carrier_id INTEGER REFERENCES platform_user (id);

CREATE INDEX ix_purchase_order_carrier ON purchase_order (carrier_id);

CREATE TABLE transport_interest (
    id         SERIAL PRIMARY KEY,
    order_id   INTEGER   NOT NULL REFERENCES purchase_order (id) ON DELETE CASCADE,
    carrier_id INTEGER   NOT NULL REFERENCES platform_user (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_transport_interest UNIQUE (order_id, carrier_id)
);

CREATE INDEX ix_transport_interest_order ON transport_interest (order_id);
