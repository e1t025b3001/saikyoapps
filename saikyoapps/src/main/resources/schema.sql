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
