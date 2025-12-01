# 鋤大D（大老二）実装計画（SSE 非同期通信・MyBatis 永続化・3 人対戦）

## 概要
本計画は以下の条件で鋤大D を実装するための具体的タスク一覧とクラス・メソッド設計を示す。
- ルール: 台湾ルール（3 人固定、スート順 ♠>♥>♦>♣、3♣ 所有者が先手、余り 1 枚を 3♣ 所有者に付与、勝者は手札 0 枚）
- 通信方式: SSE（Server-Sent Events）を用いた非同期の一斉配信（クライアントはサブスクライブして状態更新を受信）
- 永続化: MyBatis を使用。@Mapper インタフェース + XML/注釈で SQL を記述し、@Autowired で注入される singleton サービス（@Service）で DB 操作を行う。実装参考: igakilab/springboot_samples
- ボット: 使用しない。人間 3 人での対戦を前提（テスト・ローカルは複数タブで代用）
- マルチゲーム対応: 同一サーバ上で複数の DarourGame（3 人対戦）が同時に稼働することを前提とする。各 DarourGame はユニークな `darourGameId`（UUID など）で識別され、DarourGameService と DB、SSE、ロックは darourGameId 毎に管理される。

---

## マルチゲーム対応の設計ポイント（重要）
- darourGameId（String）を全ての API と内部メソッドの第一引数として利用する。
- DarourGameService は下記のような内部構造を持つ（擬似フィールド記載）:
  - Map<String, DarourGameState> darourGames  // メモリキャッシュ（起動中の DarourGame）
  - Map<String, List<SseEmitter>> emitters  // darourGameId ごとの SSE 接続リスト
  - （明示的な JVM レベルの排他ロックは使用しない。代わりに DB トランザクションと更新前後の整合性チェック、必要に応じて version カラムによる楽観ロックを導入する設計とする）
- DB 側は darour_games テーブルを darour_game_id PK で管理し、state_json は各 darourGameId に紐付く。
- 各変更操作（play/pass/deal）は以下の順で実行することを推奨する（シンプル実装）:
  1. リクエストを受け取る。まずリクエスト内容のバリデーションを行う（currentPlayer の照合等）。
  2. @Transactional を用いて DB を更新（persistState）。更新処理内で最新の state を再取得し、期待する状態と異なる場合はロールバックしてクライアントにエラーを返す（楽観的検証）。
  3. トランザクション終了後（コミット後）に emitAll(darourGameId, stateJson) を呼んで SSE 経由でクライアントに配信（トランザクション外で行う）。
- 注意点: 明示的な JVM レベルのロックを使わない設計のため race condition の可能性は残る。高負荷・高同時性を想定する場合は version カラムによる楽観ロックの導入や、必要に応じて分散ロックの採用を検討することを推奨する。
- SseEmitter は darourGameId ごとに管理し、クライアント切断時にリストから除去する。

---

## 主要クラス / インタフェース（具体名・主要メソッド）
- team1.saikyoapps.model.Card
  - フィールド: Rank rank; Suit suit; int suitPriority;
  - メソッド: static Card fromCode(String code), String toCode(), int compareTo(Card o)
- team1.saikyoapps.model.Deck
  - フィールド: List<Card> cards  // Deck は DarourGame ごとのインスタンスとして DarourGameState に保持する
  - メソッド: void initialize(), void shuffle(), DealResult dealThreePlayers(), Card draw(), int remaining()
  - 備考: Deck は DarourGameState.deck として各 darourGameId ごとに存在し、配牌/ドロー等の操作はトランザクション内で一貫して行うこと。
- team1.saikyoapps.model.DarourPlayer
  - フィールド: String playerId; String userId; String name; List<Card> hand; int position
  - メソッド: void receiveCards(List<Card> cards), void removeCards(List<Card> cards)
- team1.saikyoapps.model.Hand（抽象）
  - メソッド: HandRank getHandRank(), boolean isValid(), int compareTo(Hand other)
- team1.saikyoapps.model.hands.* (Single, Pair, Triple, FiveCardHand)
- team1.saikyoapps.model.DarourGameState
  - フィールド: String darourGameId; List<DarourPlayer> players; int currentPlayerIndex; Hand lastPlayedHand; int passCount; GameStatus status; Deck deck;
  - メソッド: DarourPlayer getCurrentPlayer(), void advanceTurn(), void clearRound(), boolean isGameOver()
  - 備考: DarourGameState は永続化時に deck の状態（残り枚数・残カード）も含めて state_json に保存・復元する。

## 永続化関連
- `team1.saikyoapps.model.DarourMapper` (MyBatis @Mapper)
  - void insertDarour(Darour darour)
  - Darour selectDarourById(String darourGameId)
  - void updateDarourState(@Param("darourGameId") String darourGameId, @Param("stateJson") String stateJson)
  - void deleteDarour(String darourGameId)
- `team1.saikyoapps.model.DarourPlayerMapper` (MyBatis @Mapper)
  - void insertDarourPlayer(DarourPlayer p)
  - List<DarourPlayer> selectDarourPlayersByDarourGameId(String darourGameId)

注: 本プロジェクトでは MyBatis の Mapper インタフェースおよびモデルクラスは `team1.saikyoapps.model` パッケージに配置します。エンティティ用の別パッケージは使わず、注釈ベース（@Select/@Insert/@Update/@Delete）で SQL を定義する方針です。

- service
  - `DarourGameService` (@Service, singleton)：darourGamesCache, emitters, darourGameLocks を管理。createDarourGame/deal/play/pass/persistState/registerEmitter/emitAll 等を提供。
- controller
  - `DarourGameController` (@RestController)：/api/darourgame, /api/darourgame/{id}/deal, /api/darourgame/{id}/state, /api/darourgame/{id}/play, /api/darourgame/{id}/pass, /api/darourgame/{id}/events (SSE)

データエンティティ:
- GameEntity: game_id (PK), owner_user_id, state_json (TEXT), created_at, updated_at
- PlayerEntity: player_id (PK), game_id (FK), user_id, name, hand_json (TEXT), position

---

## 追加タスク（DB・SSE マルチ DarourGame 対応）
タスク A: DB スキーマ作成 & MyBatis 設定
- 作業内容:
  - `src/main/resources/schema.sql` 作成（darour_games テーブルは darour_game_id PK で管理）
  - `build.gradle` に MyBatis 依存と H2 ドライバを追加（テストは H2 推奨）
  - `src/main/resources/mybatis/mapper/DarourMapper.xml`（必要なら）を追加
- 追記事項: darour_games.darour_game_id はユニーク制約を必須とする
- DoD: `./gradlew test` の起動時に H2 で schema が適用できること
- 見積: 0.5–1 日

タスク B: Mapper とモデル作成
- 作業内容:
  - `team1/saikyoapps/model/DarourMapper.java` (@Mapper)
  - `team1/saikyoapps/model/DarourPlayerMapper.java` (@Mapper)
  - `team1/saikyoapps/model/Darour.java`, `team1/saikyoapps/model/DarourPlayer.java` (モデルクラス)
- DoD: MyBatis コンテキストで mapper がロードされること（アプリ起動確認）
- 見積: 0.5 日

タスク C: DarourGameService の実装（マルチゲーム対応: キャッシュ・SSE）
- 作業内容:
  - `DarourGameService` に darourGamesCache, emitters を実装（明示的な JVM ロックは使用しない）
  - createDarourGame で UUID を発行し DB に insert、darourGamesCache に GameState を格納
  - deal/play/pass の内部処理はトランザクション内で最新 state を取得 → 検証 → 更新（persistState）を行い、コミット後に emitAll を実行
  - emitAll は emitters.get(darourGameId) の全 SseEmitter に送信
- DoD: 同時に複数の darourGameId に対して play リクエストを送り、それぞれが独立して更新されること（統合テスト）。競合が発生した場合は適切にエラー/リトライされることを確認する。
- 見積: 1–2 日

タスク D: Controller の実装と SSE 登録
- 作業内容:
  - `/api/game/{id}/events` で SseEmitter を生成し GameService.registerEmitter を呼ぶ
  - クライアント切断時に unregister するハンドリング
- DoD: 複数の darourGameId に対して EventSource 接続を行い、各 game のイベントのみ受信できること
- 見積: 0.5–1 日

タスク E: Model/Logic 実装（GameState の JSON 化を考慮）
- 作業内容:
  - GameState を JSON に変換するユーティリティ（Jackson）を用意し、persistState で state_json を更新
  - GameState の復元（selectGameById -> GameEntity -> GameState）を実装
  - Deck クラス実装（ゲームごとの Deck を GameState.deck として保持する設計）
    - dealThreePlayers() は GameState.deck を操作して配牌を行い、余り 1 枚を 3♣ 所有者に渡す
    - Deck の状態（cards 配列）を JSON として保存・復元する
- DoD: DB に保存した state_json を復元して同一の GameState（含む Deck）が得られること
- 見積: 1 日

タスク F: API 実装（create/deal/play/pass/state）
- 作業内容:
  - Controller 側で gameId を path から受け取り処理する
  - 不正な gameId に対して 404 を返す
  - play はトランザクション内で最新 state を検証し、順序違反は 409 を返す
- DoD: curl で複数 game を同時に操作し、それぞれが独立して動作すること。競合発生時のハンドリング（409 返却やリトライ動作）が確認できること。
- 見積: 0.5–1 日

タスク G: UI（EventSource を用いた game.html）
- 作業内容:
  - `game.html` は query parameter または path で gameId を受け取り `/api/game/{id}/events` に接続
  - 3 つのブラウザ/タブで異なる gameId を開き、それぞれの UI が独立して更新されることを確認
  - クライアントはサーバから受信した stateJson を用いて表示を更新する。stateJson 内に deck 情報（残り枚数や場に残るカード）を含めるため、UI は deck の情報にも対応すること
- DoD: 異なる gameId 間でイベントが混在しないこと
- 見積: 1–2 日

タスク H: テスト・ドキュメント更新
- 作業内容:
  - 同時に n 組のゲームが作成・進行できる統合テストを追加
  - `docs/specs.md` にマルチゲームの挙動と制約（最大接続数等）を記載
- DoD: `./gradlew test` が成功、手動で複数ゲームを同時に動かし競合がないことを確認
- 見積: 1–2 日

---

## 実装上の注意点（マルチゲーム観点）
- メモリ上の gamesCache が増加するとメモリ圧迫につながるため、一定時間アクセスのない gameId は自動的にメモリから除去し永続化された state のみ残す運用を検討する（LRU や TTL）。
- 明示的な JVM ロック（ReentrantLock 等）は使用せず、DB トランザクション内での最新 state の検証と、version カラム等による楽観ロックを基本戦略とする。
- emitters リストは gameId ごとに管理し、切断時に emitter を削除するロジックを正しく実装する。

---

## ブランチ運用と次アクション
- 実装は `feat/sukadai-sse-mybatis` ブランチで行うことを推奨（`git switch -c feat/sukadai-sse-mybatis`）。

---
作成者: GitHub Copilot
作成日: 2025-11-25
