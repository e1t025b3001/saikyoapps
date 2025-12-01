CREATE TABLE IF NOT EXISTS matching_queue (
    id IDENTITY,

    user_name VARCHAR(255),

    -- "marubatsu" | "darour" | ...
    requested_game VARCHAR(128) NOT NULL,

    -- WAITING / MATCHED
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);

-- Darour 用の永続化テーブル（鋤大D専用）
CREATE TABLE IF NOT EXISTS darour_games (
    darour_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(255),
    state_json CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS darour_players (
    player_id VARCHAR(36) PRIMARY KEY,
    darour_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255),
    name VARCHAR(255),
    hand_json CLOB,
    position INT,
    CONSTRAINT fk_darour_players_game FOREIGN KEY (darour_id) REFERENCES darour_games(darour_id)
);
