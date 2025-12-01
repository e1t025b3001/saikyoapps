# 鋤大D（大老二）実装仕様書（確定版）

## 概要
本仕様は本プロジェクトで実装する鋤大D（台湾ルール）について、ルール、通信方式、永続化、マルチゲーム設計、主要 API を確定したもの。

## ゲームルール（確定）
- ルールセット: 台湾ローカルルールを採用。
- プレイ人数: 3 人固定。
- 使用カード: 標準 52 枚デッキ（ジョーカー無し）。
- 配牌: 52 ÷ 3 = 17 余り 1。余った 1 枚は 3♣ を持つプレイヤーに渡す（そのプレイヤーは 18 枚、他は 17 枚）。
- 先手: 配牌後に 3♣ を持つプレイヤーが先手となる。
- ランク順: 3 < 4 < ... < A < 2（2 が最強）。
- スート順（同ランク比較）: ♠ > ♥ > ♦ > ♣。
- 許容出し役: Single, Pair, Triple, Five-card hands（Straight, Flush, Full House, Four of a Kind + Kicker, Straight Flush）。
- 終了条件: いずれかのプレイヤーの手札が 0 枚になった時点でゲーム終了（最初に 0 になったプレイヤーを勝者とする）。

## 比較と同値処理方針
- 基本方針: 役の強さ → 代表ランク → スート の順で比較。
- Single/Pair/Triple: ランクで比較、同ランクはスート優先度で比較（ペア/トリプルは各組で最も強いカードのスートを比較キーにする）。
- Five-card: 役の種類で比較、同役は代表ランク（例: フルハウスならトリプルのランク）で比較、同ランクは代表カードのスートで比較。

## アーキテクチャ（確定）
- 通信方式: SSE（Server-Sent Events）。クライアントは EventSource で `/api/game/{id}/events` に接続し、サーバからの状態更新を受信する。
- 永続化: MyBatis を使用。game 毎の状態を DB に保持し、再起動後も復元可能とする。テスト環境は H2 を使用。
- マルチゲーム: 同一サーバ上で複数の game（3 人対戦）が同時に稼働する。各ゲームはユニークな `gameId`（UUID）で識別。
- Deck: Deck は game ごとにインスタンスを持ち、GameState に含めて永続化/復元する。
- 排他制御: gameId ごとに ReentrantLock を用いて操作を排他制御。DB 更新はトランザクション化し、状態更新後に SSE で配信する。

## 主要コンポーネント（確定クラス名・役割）
- model
  - `Card`（ランク・スート・スート優先度、toCode/fromCode）
  - `Deck`（darourGame ごとのカード山、initialize/shuffle/dealThreePlayers/draw/remaining）
  - `DarourPlayer`（playerId, userId, name, hand, position）
  - `Hand` 抽象クラスと具象（Single/Pair/Triple/FiveCardHand）
  - `DarourGameState`（darourGameId, players, currentPlayerIndex, lastPlayedHand, passCount, status, deck）
- persistence
  - `team1.saikyoapps.model.DarourMapper` (@Mapper)：insert/select/update/delete for Darour (モデルクラス `team1.saikyoapps.model.Darour` を使用)
  - `team1.saikyoapps.model.DarourPlayerMapper` (@Mapper)
  - モデルクラス: `team1.saikyoapps.model.Darour` / `team1.saikyoapps.model.DarourPlayer`（state_json, hand_json を含む）

注: 本プロジェクトでは MyBatis の Mapper インタフェースおよびモデルクラスは `team1.saikyoapps.model` パッケージに配置します。エンティティ用の別パッケージは使わず、注釈ベース（@Select/@Insert/@Update/@Delete）で SQL を定義する方針です。

- service
  - `DarourGameService` (@Service, singleton)：darourGamesCache, emitters, darourGameLocks を管理。createDarourGame/deal/play/pass/persistState/registerEmitter/emitAll 等を提供。
- controller
  - `DarourGameController` (@RestController)：/api/darourgame, /api/darourgame/{id}/deal, /api/darourgame/{id}/state, /api/darourgame/{id}/play, /api/darourgame/{id}/pass, /api/darourgame/{id}/events (SSE)

## DB スキーマ（要旨）
- games
  - game_id VARCHAR(36) PRIMARY KEY, owner_user_id VARCHAR(...), state_json TEXT, created_at TIMESTAMP, updated_at TIMESTAMP
- players
  - player_id VARCHAR(36) PRIMARY KEY, game_id VARCHAR(36) REFERENCES games(game_id), user_id VARCHAR(...), name VARCHAR(...), hand_json TEXT, position INT

## API（確定した基本仕様）
- POST /api/game
  - body: { "userIds": ["u1","u2","u3"] }
  - returns: { "gameId": "..." }
- POST /api/game/{id}/deal
  - 配牌を行い、DB に保存し、SSE で状態を配信
- GET /api/game/{id}/state
  - 現在の GameState を返す
- POST /api/game/{id}/play
  - body: { "playerId": "...", "cards": ["3C","4D"] }
  - 検証: currentPlayer と一致すること、手が有効であること
- POST /api/game/{id}/pass
  - body: { "playerId": "..." }
- GET /api/game/{id}/events
  - SSE 接続。サーバは stateJson や差分イベントを送信する。

## 運用上の決定事項・注意
- gamesCache の肥大化対策として一定時間アクセスのない gameId をメモリから削除する仕組み（TTL/LRU）を導入することを検討する。
- SseEmitter のタイムアウト・切断処理を正しく実装すること。
- 5 枚役の同値ケース（代表ランク・代表カード決定ルール）はユニットテストで明確化する。
- 並列リクエスト（複数クライアントからの同時操作）に対するロックと DB の整合性を厳格に保つ。

## 次のアクション
1. `feat/sukadai-sse-mybatis` ブランチを作成して実装を開始する（推奨）。
2. タスク A（DB schema + MyBatis 設定）→ タスク B（Mapper/Entity）→ タスク C（GameService 基盤） の順で実装を進める。

---
作成者: GitHub Copilot
作成日: 2025-11-25
