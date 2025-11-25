CREATE TABLE IF NOT EXISTS matching_queue (
    id IDENTITY,

    user_name VARCHAR(255),

    -- "marubatsu" | "darour" | ...
    requested_game VARCHAR(128) NOT NULL,

    -- WAITING / MATCHED
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);
