CREATE TABLE IF NOT EXISTS matching_queue (
    id IDENTITY,

    user_name VARCHAR(255),

    -- "marubatsu" | "darour" | ...
    requested_game VARCHAR(128) NOT NULL,

    -- WAITING / MATCHED
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);

-- 新增 players_status 表紀錄使用者目前狀態（lobby / matching / playing）及所屬遊戲
CREATE TABLE IF NOT EXISTS players_status (
    user_name VARCHAR(255) PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'lobby',
    current_game VARCHAR(128)
);

-- 為每個遊戲建立 view，以便在 H2 Console 顯示每個遊戲專屬的 matching queue（命名規則：{game}_matching_queue）
CREATE VIEW IF NOT EXISTS marubatsu_matching_queue AS
  SELECT * FROM matching_queue WHERE requested_game = 'marubatsu';

CREATE VIEW IF NOT EXISTS othello_matching_queue AS
  SELECT * FROM matching_queue WHERE requested_game = 'othello';

CREATE VIEW IF NOT EXISTS gomoku_matching_queue AS
  SELECT * FROM matching_queue WHERE requested_game = 'gomoku';

CREATE VIEW IF NOT EXISTS shinkeisuijaku_matching_queue AS
  SELECT * FROM matching_queue WHERE requested_game = 'shinkeisuijaku';

CREATE VIEW IF NOT EXISTS uno_matching_queue AS
  SELECT * FROM matching_queue WHERE requested_game = 'uno';

CREATE VIEW IF NOT EXISTS dairo_matching_queue AS
  SELECT * FROM matching_queue WHERE requested_game = 'dairo';

-- 新增五目並べ相關資料表
CREATE TABLE IF NOT EXISTS gomoku_game (
    id IDENTITY,
    game_id VARCHAR(64) UNIQUE,
    player_black VARCHAR(255),
    player_white VARCHAR(255),
    board_state CLOB,
    turn VARCHAR(255),
    status VARCHAR(32) DEFAULT 'playing',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gomoku_move (
    id IDENTITY,
    game_id VARCHAR(64),
    player VARCHAR(255),
    x INT,
    y INT,
    move_no INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    extra CLOB
);

CREATE TABLE IF NOT EXISTS match_history (
    id IDENTITY,
    game_name VARCHAR(64),
    player1 VARCHAR(255),
    player2 VARCHAR(255),
    winner VARCHAR(255),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    extra CLOB
);

-- i18n_config: ユーザごとのロケール設定を保存
CREATE TABLE IF NOT EXISTS i18n_config (
    id IDENTITY,
    login_user VARCHAR(255) UNIQUE NOT NULL,
    locale VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS marubatsu_game (
    id IDENTITY,
    game_id VARCHAR(64) UNIQUE,
    player_x VARCHAR(255),
    player_o VARCHAR(255),
    board_state CLOB,
    turn VARCHAR(32),
    status VARCHAR(32) DEFAULT 'playing',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS marubatsu_move (
    id IDENTITY,
    game_id VARCHAR(64),
    player VARCHAR(255),
    x INT,
    y INT,
    move_no INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    extra CLOB
);
