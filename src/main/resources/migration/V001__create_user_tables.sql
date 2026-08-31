CREATE TABLE platform_user (
    id            SERIAL PRIMARY KEY,
    keycloak_id   UUID NOT NULL UNIQUE,      -- JWT claim "sub"
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,     -- SUPPLIER / RETAILER / CARRIER (admins are not stored here)
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP
);