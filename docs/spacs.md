概要

このドキュメントはリポジトリ `saikyoapps` の現状の全体仕様（スペック）をまとめたものです。

プロジェクト概要

- プロジェクト名: saikyoapps
- 言語/実行環境: Java 21
- ビルド: Gradle
- フレームワーク: Spring Boot（Thymeleaf、MyBatis を利用）
- 開発用DB: H2（メモリ）

実行方法（開発）

- Windows: `gradlew.bat bootRun`
- 標準ポート: ログによりポート 80 で起動している場合あり（設定で変更可能）

主要ディレクトリ/ファイル

- テンプレート: `saikyoapps/src/main/resources/templates/`
  - `index.html` - ゲーム選択画面（ゲーム選択・マッチング開始・観戦ボタン等のクライアント処理を含む）
  - `matching.html` - マッチング待機画面（待機人数の表示、ポーリングによるマッチング状態確認）
  - 各ゲームの画面テンプレート（`marubatsu.html`, `gomoku.html`, `gomoku_spectate.html`, `uno.html` など）

- 静的資産: `saikyoapps/src/main/resources/static/`（CSS/JS/画像/音声等）
- 多言語 (i18n): `saikyoapps/src/main/resources/i18n/messages_*.properties`（ja/en/zh_TW）
- スキーマ: `saikyoapps/src/main/resources/schema.sql`

API / エンドポイント（主なもの）

- GET `/` - ホーム（ゲーム選択）
- GET `/matching?game={game}` - マッチング処理の開始（選択画面から遷移）
- GET `/matching/status?game={game}` - マッチング待機中ポーリング用 API（JSON を返す）
- POST `/match/leave` - マッチングキャンセル
- GET `/{game}` - 各ゲーム画面へ遷移（`matchId` クエリなどで参照）
- GET `/{game}/spectate` - 観戦ページ（対応するゲームのみ）
- POST `/i18n` - 言語設定保存
- POST `/logout` - ログアウト

クライアント挙動（重要点）

- `index.html` のクライアントJS がゲーム選択を制御し、選択後に「マッチング開始」または「観戦」へ遷移。
  - 観戦可能ゲームは配列 `spectateSupportedGames`（現状 `gomoku`）で判定。
- `matching.html` は `selectedGame` を Thymeleaf で埋め込み、`/matching/status` を定期ポーリングしてマッチング状況を確認する。
- 待機人数表示は DOM の id 名と JS 側の参照名が一致している必要がある（例: `waiting-count` を参照）。

データベース / マイグレーション

- `schema.sql` に初期テーブル定義がある。アプリ起動時にスクリプトで初期化される。
- 既知の問題: `i18n_config` テーブルで `user` をカラム名として使うと H2 の構文エラーとなるため、実際は予約語回避のためカラム名を `login_user` 等に変更する必要があった。

依存関係 / 技術スタック詳細

- Spring Boot
- Thymeleaf テンプレートエンジン
- MyBatis（Mapper を使用）
- HikariCP（コネクションプール）
- H2 データベース（開発用）

既知の問題と注意点

- `schema.sql` の `user` カラムによる H2 の構文エラーが起点でアプリ起動に失敗するケースがある（ログに ScriptStatementFailedException）。対処: カラム名を変更または引用符で囲む。
- テンプレートのメッセージ埋め込み（`th:text`）を使う場合、モデルに該当属性が入っていないと空表示になる。
- `matching.html` の待機人数表示など DOM id と JS 参照名の不一致に注意する。

今後の推奨アクション（短く）

1. `schema.sql` のカラム名（`user`）を修正して起動確認する。
2. `matching.html` の DOM id と JS の参照名を合わせる（`waiting-count` 等）。
3. 必要なら `matching.html` で Thymeleaf のメッセージ表示からモデル直出力へ変更し、表示確認を行う。

参照ファイル

- `saikyoapps/src/main/resources/templates/index.html`
- `saikyoapps/src/main/resources/templates/matching.html`
- `saikyoapps/src/main/resources/schema.sql`
- `saikyoapps/src/main/resources/i18n/messages_ja.properties`

作成日: 2026-01-06
