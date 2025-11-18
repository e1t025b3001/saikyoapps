# ログイン機能実装計画（in-memory 認証：事前配布ユーザ）

## 概要
本計画は、調査レポート（`docs/reports/investigate/2025-11-18_ログイン機能調査.md`）を元に、事前に配布するID/パスワードを使った in-memory 認証を Spring Security で実装するためのタスクを小さな単位に分割したものです。各タスクに必要ファイル、実行手順、DoD（完了定義）を記載します。

## 実装方針の要点
- 認証方式: in-memory（`InMemoryUserDetailsManager` を利用）
- テンプレート: Thymeleaf（`thymeleaf-extras-springsecurity6` を利用）
- パスワード: 演習用でも可能な限り `BCrypt` ハッシュを使用する。今回の計画では事前にハッシュ値を用意し、アプリ起動時に in-memory ユーザとして登録する方式とする。
- 認証情報配布: 配布用ドキュメント `docs/credentials.md` にユーザ一覧（ハッシュ値を含む）を記載し、運用側でプレイヤーへ配布する。

## タスク一覧（優先度順）

1) ブランチ作成と初期準備（必須）
- 内容: `main` から新ブランチを作成（例: `feat/login`）。
- 関連ファイル: なし（運用ルール）
- DoD: 新規ブランチが作成され、以降のコミットはそのブランチ上で行う旨を README に記載できる。

2) Security 設定クラス作成
- 内容: `SecurityConfig` を作成し、`SecurityFilterChain` と `InMemoryUserDetailsManager`、`PasswordEncoder` を定義する。起動時に配布ドキュメントで指定されたハッシュ値でユーザを登録する。
- 関連ファイル:
  - `saikyoapps/src/main/java/team1/saikyoapps/config/SecurityConfig.java`
- 実装上の注意点:
  - `PasswordEncoder` は `BCryptPasswordEncoder` を使用すること。
  - 起動時に `User.withUsername("foo").password("<hash>").roles("PLAYER").build()` のように、事前ハッシュを登録する（`PasswordEncoder` を注入した上で、ハッシュ済みパスワードをそのまま設定する）。
- DoD:
  - アプリ起動時に事前定義したユーザ（下記参照）が認識され、ログイン可能である。
  - 保護対象URLへ未認証でアクセスすると `/login` にリダイレクトされることを手動で確認できる。
  - 実行コマンド: `./gradlew bootRun` → ブラウザで `http://localhost:8080/` にアクセスしログインページが表示される。

3) ログイン画面の実装（Thymeleaf）
- 内容: ログインフォームを `templates/login.html` に実装（CSRF トークン、username/password フィールド、エラーメッセージ）。
- 関連ファイル:
  - `saikyoapps/src/main/resources/templates/login.html`
- DoD:
  - `/login` でフォームが表示され、既定ユーザでログインできることを確認する。

4) 配布用認証情報ドキュメント作成
- 内容: 配布するユーザ一覧を `docs/credentials.md` に記載する。今回の計画では以下のユーザを事前登録する。
- 配布ユーザ（事前登録：ハッシュ値を使用、ロール: PLAYER）:
  - ユーザ: `foo`
    - 平文パスワード（開発時確認用）: `qwert`
    - ハッシュ（BCrypt）: `$2y$05$m/bFb4oVM/SwkJmjRNJQr.M.c46/gdDu39kvXTWoGmxVwsdf0PVAy`
    - ロール: `PLAYER`
  - ユーザ: `bar`
    - 平文パスワード（開発時確認用）: `qwert`
    - ハッシュ（BCrypt）: `$2y$05$4LwYSdqYDV3u9m3IwBWVFe.u52Uf8xLJlc1/tJfIorbnn.hLIJsaK`
    - ロール: `PLAYER`
  - ユーザ: `buz`
    - 平文パスワード（開発時確認用）: `qwert`
    - ハッシュ（BCrypt）: `$2y$05$nUsb7ufJ83W9cIpc5KiM6e.JtFcl8Um0qkgUU1yKSENPSnUD3sB/a`
    - ロール: `PLAYER`
- 関連ファイル: `docs/credentials.md`
- DoD: `docs/credentials.md` が存在し、記載のハッシュ値で起動時に in-memory ユーザが登録され、平文 `qwert` でログインできることを確認できる。

5) テスト作成
- 内容: `spring-security-test` を使い認証の単体/統合テストを追加する。テストでは `qwert` を使ってログインできることを確認する（ハッシュは事前登録済み）。
- 関連ファイル:
  - `saikyoapps/src/test/java/team1/saikyoapps/SecurityIntegrationTests.java`
- DoD:
  - 主要ケース（ログイン成功、ログイン失敗、未認証からのリダイレクト）が CI 上で通ること（`./gradlew test`）。

6) ドキュメント更新
- 内容: `docs/specs.md` に実装概要を反映し、`docs/reports/done/done_YYYY-MM-DD_ログイン実装.md` を作成する。
- 関連ファイル: `docs/specs.md`, `docs/reports/done/` に done レポート
- DoD: specs.md に新機能が記載され、done レポートが作成されていること。

## テスト手順（開発者向け）
1. `./gradlew bootRun` でアプリを起動
2. ブラウザで `http://localhost:8080/` にアクセス -> `/login` が表示される
3. `docs/credentials.md` に記載のユーザでログイン可能であることを確認
4. `./gradlew test` でテストが成功すること

## 注意事項
- リポジトリに平文パスワードを残さない運用が望ましい。docs へはダミー例を記載するか、配布は別手段を検討する。
- 実装は計画で定めた範囲のみ行う（追加機能は都度確認）。

---
作業者: GitHub Copilot
