# ゲーム選択画面実装計画（ログイン後のゲーム選択画面表示）

## 概要
本計画は、`docs/reports/investigate/2025-11-18_ゲーム選択画面実装調査.md` の調査結果に基づき、ログイン後にゲーム選択画面を表示するための実装作業を小さなタスクに分割したものです。画面要件は以下の通り。

- 画面上部に「ようこそ {username} さん」を表示する（既存の `username` モデルを利用）
- ゲーム選択ボタン（今回は「マルバツゲーム」のみ）
- マッチング開始ボタン（ダミーで良い。押下時は JavaScript のアラート等で良い）
- 現状のログアウトボタンはそのまま残す

## 実装方針（推奨）
- 最小変更で実現するため、既存のトップページテンプレート `index.html` を拡張してゲーム選択画面を実装する。
- 既存の `HomeController#index` はすでに `username` を model にセットしているため、コントローラ側の変更は不要とする（必要になれば別途タスク化）。

## 変更対象ファイル（相対パス）
- `saikyoapps/src/main/resources/templates/index.html`  ← 推奨: ここにゲーム選択 UI を追加
- （オプション）`saikyoapps/src/main/java/team1/saikyoapps/controller/HomeController.java`  ← 新しいページに分離する場合のみ
- （オプション）`saikyoapps/src/main/java/team1/saikyoapps/config/SecurityConfig.java`  ← リダイレクト先を変更する場合のみ

## タスク一覧（優先度順・最小単位）

1) ブランチ準備（既存ブランチ利用）
- 内容: 作業用ブランチは既に作成済みのため、新規ブランチ作成は不要です。現在の作業ブランチ上で作業を続行します。
- 確認事項: 現在のブランチ名を確認し、作業内容を記録してください（例: `git branch --show-current`）。
- DoD: 作業は現在のブランチ上で行われ、使用したブランチ名が done レポートに記載されていること。

2) テンプレート編集: `index.html` にゲーム選択 UI を追加（必須）
- 内容: 既存の `index.html` に以下を追加する。
  - ヘッダのユーザ表示（既存の `username` を使用）
  - 「マルバツゲーム」選択ボタン（見た目は button）
  - 「マッチング開始」ボタン（押下でダミーアクション）。
  - 必要なら簡易な JavaScript を追加し、選択状態の表示とマッチング開始時の alert を実装する。
- 関連ファイル:
  - `saikyoapps/src/main/resources/templates/index.html`
- DoD:
  - `./gradlew bootRun` でアプリを起動し、ブラウザで `http://localhost:8080/` にアクセスするとログイン画面に遷移する。
  - 既存ユーザ（`foo` / `bar` / `buz` のいずれか）でログイン後、トップページに「ようこそ {username} さん」が表示される。
  - 「マルバツゲーム」ボタンが見えること。
  - 「マッチング開始」ボタンを押すとダミー動作（例: アラート）が動作すること。

3) コントローラ確認（任意）
- 内容: `HomeController#index` が `username` を model に渡していることを確認する。もし別ページに分離する場合は `/select` 用の GET ハンドラを追加する。
- 関連ファイル:
  - `saikyoapps/src/main/java/team1/saikyoapps/controller/HomeController.java`
- DoD: テンプレートが `username` を受け取り表示できること。

4) セキュリティ設定確認（任意）
- 内容: `SecurityConfig` の `defaultSuccessUrl` は現在 `"/"` に設定されている。別テンプレート（例: `/select`）に分離する場合のみ `SecurityConfig` を修正する。
- 関連ファイル: `saikyoapps/src/main/java/team1/saikyoapps/config/SecurityConfig.java`
- DoD: ログイン成功後に期待する URL に遷移することを確認する。

5) ドキュメントと完了報告の作成（必須）
- 内容: 実装完了後に以下を作成する。
  - `docs/reports/done/done_YYYY-MM-DD_ゲーム選択実装.md` に実装内容、確認手順、使用ブランチ名を記載する。
  - 必要に応じて `docs/specs.md` を更新する。
- DoD: done レポートが作成され、実装手順と確認方法が記載されていること。

## 実行手順（開発者向け）
1. 作業中のブランチにいることを確認する: `git branch --show-current`（既に作業用ブランチがある前提）
2. `saikyoapps/src/main/resources/templates/index.html` を編集して UI を追加
3. `./gradlew bootRun` を実行
4. ブラウザで `http://localhost:8080/` にアクセスし、ログイン後にゲーム選択画面が表示されることを確認する
5. 変更をコミットし、作業が完了したら `docs/reports/done/done_YYYY-MM-DD_ゲーム選択実装.md` を作成する

## 確認手順（DoD のまとめ）
- 起動: `./gradlew bootRun` でアプリが起動すること
- ログイン: `http://localhost:8080/` にアクセスし、`foo` / `bar` / `buz` のいずれかでログインできること
- 表示: ログイン後に「ようこそ {username} さん」が表示されること
- UI: 「マルバツゲーム」ボタンが表示され、「マッチング開始」ボタンを押すとダミー動作が発生すること

## 補足・注意点
- 本計画では機能は最小限（マルバツゲームのみ、マッチングはダミー）に留める。将来的な拡張（複数ゲーム、実際のマッチング実装）は別タスクで対応する。
- 既存の in-memory ユーザはハッシュ済みパスワードで登録されているため、開発/検証用の平文パスワードは `docs/credentials.md` 等で管理すること。

---
作業者: GitHub Copilot
