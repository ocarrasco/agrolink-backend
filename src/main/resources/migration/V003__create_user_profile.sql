CREATE TABLE user_profile (
    user_id       INTEGER   PRIMARY KEY REFERENCES platform_user (id),
    delivery      BOOLEAN   NOT NULL DEFAULT FALSE,
    availability  JSONB     NOT NULL DEFAULT '{}'::jsonb,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP
);
