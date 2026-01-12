# セットアップマニュアル

このマニュアルは、JDK がインストールされた仮想サーバ上で本 Web アプリケーションを動作させるための手順を具体的に記述しています。スタッフが実際にセットアップし、動作確認できるように手順に従ってください。



前提
- 仮想サーバに JDK (Java 11+ またはプロジェクトで要求されるバージョン) がインストール済みであること。
- ネットワークから GitHub にアクセスできること（proxy/VPN 要確認）。

1. サーバの基本パッケージ準備

例（Ubuntu 系）:

- パッケージ更新・インストール
  - sudo apt update && sudo apt install -y git curl unzip nginx

2. リポジトリの取得

- 作業ディレクトリへ移動してクローン
  - git clone git@github.com:e1t025b3001/saikyoapps.git
  - cd saikyoapps

3. アプリケーションのビルドと起動

- Gradle ラッパーで起動:
  - bash ./gradlew bootRun
  - ブラウザで http://150.89.233.201/ にアクセスして動作を確認

4. 環境変数・設定ファイル

- 環境に合わせて `application.properties` または環境変数（SPRING_*）で以下を設定します:
  - サーバポート（server.port）
  - データベース接続情報（spring.datasource.*）
  - ログ出力先等

5. 動作確認手順

- ブラウザで http://150.89.233.201 にアクセスし、トップページが表示される。
- ログに起動成功メッセージが出力されている（/var/log/saikyoapps.log や systemd journal）。
- ゲーム一覧やログイン画面など、主要な画面に遷移できること。

付録: よく使うコマンドまとめ
- git pull
- bash ./gradlew bootRun
- sudo systemctl status saikyoapps
