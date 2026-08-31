CREATE TABLE purchase_order (
    id            SERIAL PRIMARY KEY,
    retailer_id   INTEGER       NOT NULL REFERENCES platform_user (id),
    supplier_id   INTEGER       NOT NULL REFERENCES platform_user (id),
    status        VARCHAR(20)   NOT NULL,
    total         INTEGER       NOT NULL DEFAULT 0,
    supplier_note VARCHAR(500),
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    CONSTRAINT ck_purchase_order_parties_differ CHECK (retailer_id <> supplier_id)
);

CREATE INDEX ix_purchase_order_retailer ON purchase_order (retailer_id, status);
CREATE INDEX ix_purchase_order_supplier ON purchase_order (supplier_id, status);

CREATE TABLE order_item (
    id                SERIAL PRIMARY KEY,
    order_id          INTEGER       NOT NULL REFERENCES purchase_order (id) ON DELETE CASCADE,
    catalog_item_id   INTEGER       NOT NULL REFERENCES catalog_item (id),
    master_product_id INTEGER       NOT NULL REFERENCES master_product (id),
    product_name      VARCHAR(120)  NOT NULL,
    unit              VARCHAR(30)   NOT NULL,
    quantity          INTEGER       NOT NULL,
    unit_price        INTEGER       NOT NULL,
    line_total        INTEGER       NOT NULL,
    CONSTRAINT uq_order_item UNIQUE (order_id, catalog_item_id)
);

CREATE INDEX ix_order_item_order ON order_item (order_id);
