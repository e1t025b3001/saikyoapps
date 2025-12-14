# SaikyoApps 遊戲套件 使用說明書（中文 / 日文）

---

## 中文說明

### 專案概述
本專案為多遊戲平臺（包含五目並べ Gomoku、三目並べ TicTacToe / まるばつ、以及其他遊戲頁面），採用 Java Spring Boot + Thymeleaf 前後端混合實作，資料存放於內建 H2（開發環境）。系統以「配對（matching）→ 建立對戰 session → 即時對戰（polling）」的流程為基礎，短期使用 polling，同步資料至資料庫的即時表與對戰歷史。

### 功能要點
- 配對機制：`/matching` 與 `/matching/status`，使用 `matching_queue` 與 `players_status` 管理等待與狀態。
- 即時對戰資料：每個遊戲有 game 表及 move 表（例：`gomoku_game` / `gomoku_move`、`tictactoe_game` / `tictactoe_move`）。
- 對戰歷史：勝負結果寫入 `match_history` 作永久紀錄。
- 前端採用短輪詢（/matching/status 與 /{game}/{gameId}），保留未來以 WebSocket 升級的空間。
- 開發便利：在開發環境放寬 CSRF 對 `/gomoku/**`、`/tictactoe/**` 的要求（請勿在生產環境直接使用）。

### 快速安裝與啟動
1. 取得原始碼並切到工作分支。
2. 啟動：
   - 開發環境 (bash): `./gradlew bootRun`（Windows 可用 `gradlew.bat bootRun`）
3. 於瀏覽器開啟：`http://localhost:8080/`，登入範例使用者（若使用 InMemory 用戶）

### 重要資料表（摘要）
- gomoku_game(game_id, player_black, player_white, board_state, turn, status)
- gomoku_move(id, game_id, player, x, y, move_no, created_at)
- tictactoe_game(game_id, player_x, player_o, board_state, turn, status)
- tictactoe_move(id, game_id, player, x, y, move_no, created_at)
- matching_queue / players_status（配對與玩家狀態）
- match_history（對戰結果）

### 主要 HTTP API（摘要）
- GET /matching/status?game={game}  — 取得配對/遊玩狀態，若狀態為 playing 且尚無 game session，server 會 on‑demand 建立該遊戲的 game 記錄並回傳 gameId。
- GET /{game}/{gameId}  — 取得對戰狀態（board, turn, finished, myColor/mySide）
- POST /{game}/{gameId}/move  — 提交落子（伺服器端驗證回合、寫入 move、勝負判定）
- POST /{game}/{gameId}/forfeit — 玩家投降（寫入 match_history、刪除 moves、回復 players_status）
- POST /match/leave — 使用者主動返回大廳時呼叫（會清除 game/move，回到 lobby）
- （Tictactoe）GET /tictactoe/session — 當 matching/status 未回傳 gameId 時，前端可呼叫此 API 建立或取得 session

> 注意：{game} 代表路徑如 `gomoku`、`tictactoe`。

### 前端行為說明
- 配對流程：前端向 `/matching/status?game={game}` 以 credentials:'include' 輪詢，當 status 變為 `playing` 且回傳 `gameId` 後，前端啟動對戰輪詢。
- 對戰輪詢：前端每秒輪詢 `GET /{game}/{gameId}` 以更新棋盤與回合；點擊落子時呼叫 `POST /{game}/{gameId}/move`，伺服器成功回傳後再更新畫面。
- 勝負處理：伺服器在勝利或投降時寫入 `match_history`，刪除當局 `*_move` 記錄，並刪除或清空 `*_game`（以避免殘留舊局）。
- 投降按鈕：前端在遊戲頁面提供投降按鈕呼叫 `/.../forfeit`。

### 測試與驗收（DoD）
1. 啟動應用並登入兩個測試帳號（A, B）。
2. A 選擇遊戲並按配對，B 選擇相同遊戲並配對，等待配對成功。
3. 配對成功後兩者應進入該遊戲頁面並各自收到 `gameId` 與 myColor/mySide，且任一方可以合法下子。
4. 每一步下子會在對應的 `*_move` 表中新增一筆紀錄。
5. 當一方勝利或投降時：
   - `match_history` 有一筆勝負紀錄；
   - 對應的 `*_move` 記錄被刪除；
   - `*_game` 被刪除或 board_state 被清空；
   - 兩位玩家的 `players_status` 回到 `lobby`；
   - 前端顯示勝敗並回到大廳（自動或確認）。

### 開發者注意事項
- 不要在生產環境忽略 CSRF；目前為開發方便才忽略 `/gomoku/**`、`/tictactoe/**`。
- 若要改成 WebSocket 實時推送，後端已有 `WebSocketConfig`，可在未來以 STOMP 主題替換 polling。

---

## 日本語説明

### プロジェクト概要
このプロジェクトは複数の対戦ゲーム（五目並べ Gomoku、三目並べ TicTacToe / まるばつ 等）を提供する Web アプリケーションです。バックエンドは Spring Boot、テンプレートは Thymeleaf を使用し、短期的には polling による同期方式を採用しています。対戦履歴はデータベースに永続化します。

### 主な機能
- マッチング機能 (`/matching`, `/matching/status`)：待機キューとプレイヤーステータスで管理します。
- 対戦データ：各ゲームは `*_game`（セッション）と `*_move`（手の履歴）を持ちます。
- 対戦履歴：勝敗は `match_history` に保存されます。
- フロントエンドは polling ベース。将来的に WebSocket(STOMP) に移行可能。

### 起動方法
1. ソースを取得後、プロジェクトルートで起動：`./gradlew bootRun`（Windows は `gradlew.bat bootRun`）。
2. ブラウザで `http://localhost:8080/` を開き、テストユーザでログインします。

### 主要テーブル（概要）
- gomoku_game / gomoku_move
- tictactoe_game / tictactoe_move
- matching_queue / players_status
- match_history

### 主要 API（概要）
- GET /matching/status?game={game}
- GET /{game}/{gameId}
- POST /{game}/{gameId}/move
- POST /{game}/{gameId}/forfeit
- POST /match/leave
- GET /tictactoe/session （tictactoe 用の session 取得 API）

### フロントエンドの挙動
- `/matching/status` を輪番で呼び、`playing` かつ `gameId` を受け取ったらゲームループを開始。
- 各手は `POST /{game}/{gameId}/move` でサーバに送信し、サーバの承認を得て画面を更新。
- 勝敗または投了時にサーバは `match_history` に保存し、`*_move` を削除、`*_game` を削除または初期化する。

### 動作確認（DoD）
1. 2 ユーザでマッチングしてゲーム開始。
2. 手ごとに `*_move` が記録されること。
3. 勝敗または投了時に `match_history` が書き込まれ、対戦一時データが消去されること。

---

## 玩家向け：遊戲玩法說明（中文 / 日本語）

### 中文（玩家向け）

#### 五目並べ（Gomoku）
- 目標：在 15×15 的棋盤上，先將自己的棋子（黑/白）連成五子直線（橫/直/斜）即為勝利。
- 先手與後手：系統會在配對成功時隨機或依規則分配黑／白與先後序。黑方通常先行。
- 操作：點擊棋盤上的格子以下子。每次下子會由伺服器驗證是否合法（是否輪到你、該格是否空位），成功後畫面會更新。
- 投降：按下「投降」按鈕，則視為放棄，對方獲勝，系統會記錄對戰結果並返回大廳。
- 再戰與資料：每次配對成功會建立新的對局；上一局的棋盤記錄在比賽結束時會被清除，只保留對戰歷史記錄於 `match_history`。

#### 三目並べ（TicTacToe / まるばつ）
- 目標：在 3×3 的棋盤上，連成三子直線（橫/直/斜）即為勝利。若所有格子填滿但沒人連成三子，為平手（和局）。
- 先手與符號：系統會在配對成功時隨機決定誰為 X（先手）或 O（後手）。
- 操作：點擊空格以下子。伺服器會驗證回合與合法性，成功後更新畫面。
- 投降：可按投降按鈕直接結束並視為敗北，系統會記錄對戰結果並返回大廳。
- 再戰與資料：同 Gomoku，每次配對為新局，結束後即時資料會被清除，結果記錄於對戰歷史中。

#### 共通注意事項（玩家）
- 對局流程：配對 → 進入遊戲畫面 → 等待系統分配先後與顏色/符號 → 輪流下子 → 決勝或投降 → 返回大廳。
- 操作介面：在遊戲頁面以滑鼠點擊棋盤格子下子，投降按鈕會立即送出請求。
- 若網頁卡住或無法下子：請確認已登入並且配對狀態為 "playing"，或重新載入頁面並重試配對。
- 對戰歷史：勝負結果會記錄於系統的對戰履歷，玩家可透過管理介面或後續功能查看（視系統實作）。

---

### 日本語（プレイヤー向け）

#### 五目並べ（Gomoku）
- 目的：15×15 の盤上で、自分の石（黒／白）を縦・横・斜めに5つ連続して並べた方が勝ちです。
- 先手／後手：マッチング成立時に先手・後手（黒／白）を割り当てます。通常は黒が先手です。
- 操作：盤面のマスをクリックして手を打ちます。サーバ側で手番や空きの確認を行い、承認後に画面が更新されます。
- 投了：『投降』ボタンを押すと降参扱いとなり、相手の勝ちとして記録されます。
- 対局データ：対局が終了すると一時的な対局データ（手の履歴や盤面）は消去され、結果は `match_history` に保存されます。

#### 三目並べ（TicTacToe / まるばつ）
- 目的：3×3 の盤で横・縦・斜めに3つ並べた方が勝ち。全て埋まって勝ちが無ければ引き分けです。
- 先手／記号：マッチング時に X（先手）と O（後手）を割り当てます。
- 操作：空きマスをクリックして手を打ちます。サーバで検証され、成立すれば画面が更新されます。
- 投了：投降ボタンで降参できます。結果は履歴に記録され、大会画面に戻ります。
- 対局データ：各対局は新しいセッションとして扱われ、終了時に一時データは削除されます。

#### 共通注意事項（プレイヤー向け）
- 対局の流れ：マッチング → ゲーム画面へ移動 → 先手・記号の割当 → 順番に手を打つ → 勝敗または投了 → ロビーへ戻る。
- 操作方法：マウスで盤のセルをクリックして手を打ちます。投降ボタンで即時に降参できます。
- トラブル時：ページが応答しないなどの問題があれば、ページをリロードして再度マッチングを行ってください。
- 履歴の保存：勝敗は `match_history` に保存されます。再生機能等を追加する場合は別途履歴を保持する仕組みを検討してください。
