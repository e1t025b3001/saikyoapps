# 三目並べ（まるばつ）実装計画

## 概要
- 目的: 同一端末で先手・後手を交互に操作して遊べる「三目並べ」機能を実装する（まずはクライアント（JS）で完結する簡易版を優先する）。
- 範囲: UI（テンプレート + 静的JS）、サーバーはテンプレート配信のみ。セッション永続化やネットワーク対戦は含まない。
- 前提: 本計画は調査レポート `docs/reports/investigate/2025-11-30_三目並べ実装調査.md` を基に作成。

## 関連ファイル（必ず確認するもの）
- `saikyoapps/src/main/resources/templates/matching.html`
- `saikyoapps/src/main/resources/templates/login.html`
- `saikyoapps/src/main/resources/templates/index.html`
- `saikyoapps/src/main/resources/templates/`（新規 `tictactoe.html` を作成予定）
- `saikyoapps/src/main/resources/static/js/`（新規 `tictactoe.js` を作成予定）
- `saikyoapps/src/main/java/team1/saikyoapps/controller/`（必要であれば `TicTacToeController.java` を追加）
- `docs/specs.md`（実装後に更新すること）

## タスク一覧（最小単位で分割）

1) テンプレート追加: ゲーム画面作成
- 内容: 3x3 グリッド、現在手番表示、勝敗表示、リセットボタンを持つ `tictactoe.html` を作成
- 関連ファイル: `saikyoapps/src/main/resources/templates/tictactoe.html`
- 実行手順:
  1. `tictactoe.html` を作成し、Thymeleaf ベースで必要な要素を配置する
  2. `tictactoe.html` から `static/js/tictactoe.js` を読み込むように設定する
- 入力/出力: ブラウザでの視覚的要素（HTML）を追加
- DoD (Definition of Done):
  - `gradle bootRun` 実行後、`http://localhost:8080/tictactoe` にアクセスするとゲーム画面が表示される（ログインが必要な場合は既存の認証を使用）。
  - 画面に 3x3 マス、手番表示、リセットボタンが存在する。

2) 静的JS実装: クライアント側ゲームロジック
- 内容: `tictactoe.js` に盤面管理、手番管理、勝敗判定（横・縦・斜め）、引き分け判定、UI更新を実装
- 関連ファイル: `saikyoapps/src/main/resources/static/js/tictactoe.js`
- 実行手順:
  1. 盤面を 2 次元配列で管理（0=空, 1=先手, 2=後手）
  2. クリックイベントでセルに手を置き、手番を切り替え
  3. 各手後に勝敗判定・引き分け判定を行い、結果を表示
  4. リセットボタンで盤面を初期化
- 入力/出力: ユーザーのクリック操作 -> UI 更新
- DoD:
  - 先手→後手でクリック操作が可能で、正しいマークがセルに表示される
  - 勝利・引き分けが正しく判定され、画面に通知される
  - リセットで初期状態に戻る

3) サーバーのルーティング（テンプレート配信のみ、必要なら）
- 内容: GET `/tictactoe` を提供するコントローラを追加（既存コントローラに追記しても可）
- 関連ファイル: `saikyoapps/src/main/java/team1/saikyoapps/controller/TicTacToeController.java`（または既存の `MatchingController.java` に追加）
- 実行手順:
  1. コントローラの作成/編集で GET `/tictactoe` を実装し `tictactoe.html` を返す
  2. 必要ならセキュリティ設定でアクセス制御を確認
- 入力/出力: ブラウザの GET リクエスト -> テンプレートを返す
- DoD:
  - `http://localhost:8080/tictactoe` にアクセスして `tictactoe.html` が表示される

4) ドキュメント更新
- 内容: 実装完了後、以下を更新
  - `docs/reports/done/done_YYYY-MM-DD_三目並べ実装.md` に作業内容、確認手順、ブランチ名を記載
  - `docs/specs.md` に機能追加を反映
- 関連ファイル: `docs/reports/done/`, `docs/specs.md`
- DoD:
  - 上記ファイルが作成/更新され、手順に従って動作確認ができる

5) テスト（任意だが推奨）
- 内容: サーバー側ロジックを追加する場合はユニットテストを作成。今回はクライアント実装のため手動テストを推奨
- 関連ファイル: `src/test/java/...`（該当テストファイルを作成）
- DoD:
  - 勝敗判定ロジックに関するユニットテスト（サーバー側実装時）が成功する
  - 手動テスト手順が `docs/reports/done/` に記載される

## 優先度と見積もり（粗）
- 高: タスク1（テンプレート追加）, タスク2（静的JS実装） — それぞれ 1〜2 時間程度（合計 2〜4 時間）
- 中: タスク3（ルーティング） — 30 分〜1 時間
- 低: タスク5（ユニットテスト）/ ドキュメント整備 — 1 時間

## 実施手順（順序）
1. `main` ブランチで作業することを確認する
2. 新しいブランチを作成: 例 `feat/tictactoe-client`
3. タスク1 → タスク2 → タスク3（必要なら）→ タスク4 の順で実装
4. 動作確認（DoD）を満たしたら `docs/reports/done/` に完了レポートを作成

## 動作確認手順（簡潔）
1. ターミナルでプロジェクトルートに移動
2. `./gradlew bootRun` を実行
3. ブラウザで `http://localhost:8080/tictactoe` にアクセス（ログインが必要な場合は既存の認証を使用）
4. 先手→後手でクリックして、勝敗・引き分けが正しく表示されることを確認

## 備考 / ユーザー確認事項
- 本計画は「クライアント（JS）完結」前提です。サーバー側で状態管理を希望する場合はその旨を通知してください。サーバー実装の場合はタスク分割とDoDを再作成します。

---
作業報告: このファイルは `docs/tasks.md` を上書きしました。次に実装フェーズに進めてよいか確認してください。
