CREATE TABLE master_product (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    unit        VARCHAR(30)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP
);

-- case-insensitive uniqueness so "Papa" and "papa" cannot coexist
CREATE UNIQUE INDEX ux_master_product_name_lower ON master_product (lower(name));

CREATE TABLE catalog_item (
    id                  SERIAL PRIMARY KEY,
    supplier_id         INTEGER        NOT NULL REFERENCES platform_user (id),
    master_product_id   INTEGER        NOT NULL REFERENCES master_product (id),
    unit                VARCHAR(30)    NOT NULL,
    price_per_unit      INTEGER        NOT NULL,
    available_quantity  INTEGER        NOT NULL DEFAULT 0,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP,
    CONSTRAINT uq_catalog_item UNIQUE (supplier_id, master_product_id)
);

CREATE INDEX ix_catalog_item_supplier ON catalog_item (supplier_id);
