# 認証情報（配布用ドキュメント）

本プロジェクトでは、事前に配布する ID/パスワードを使った in-memory 認証を採用します。

配布ユーザ（事前登録：ハッシュ値を使用、ロール: PLAYER）:

- ID: `foo`
  - 平文パスワード（配布用）: `qwert`
  - BCrypt ハッシュ: `$2y$05$m/bFb4oVM/SwkJmjRNJQr.M.c46/gdDu39kvXTWoGmxVwsdf0PVAy`
  - ロール: `PLAYER`

- ID: `bar`
  - 平文パスワード（配布用）: `qwert`
  - BCrypt ハッシュ: `$2y$05$4LwYSdqYDV3u9m3IwBWVFe.u52Uf8xLJlc1/tJfIorbnn.hLIJsaK`
  - ロール: `PLAYER`

- ID: `buz`
  - 平文パスワード（配布用）: `qwert`
  - BCrypt ハッシュ: `$2y$05$nUsb7ufJ83W9cIpc5KiM6e.JtFcl8Um0qkgUU1yKSENPSnUD3sB/a`
  - ロール: `PLAYER`

注意:
- リポジトリに平文パスワードを残さない運用を推奨します。ハッシュ値は repo に記載していますが、平文の配布は運用側で安全に行ってください。
- 起動時にアプリは上記ハッシュ値を使って in-memory ユーザを登録します。開発環境では上記の平文でログイン確認が可能です。
