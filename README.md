# social-login-with-scala-tidb-discord

## 概要

このアプリケーションは、DiscordのOAuth2.0を使用してユーザー認証を行い、認証後にメモを投稿・閲覧できる機能を提供します。データベースにはMySQLを使用しています。

## セットアップ手順

1. Docker ComposeでTiDBを起動
```shell
docker compose up -d
```

2. DBにテーブルを作成
```shell
docker run -it --rm --network host -v $(pwd)/tidb/mytable.sql:/mytable.sql mysql mysql -h 127.0.0.1 -P 4000 -u root -e "source /mytable.sql"

# テーブルが作成されたことを確認
docker run -it --rm --network host mysql mysql -h 127.0.0.1 -P 4000 -u root -D memo_app
> desc User;
> desc Note;
> exit
```

3. Discord Developer Portalでアプリケーションを作成

- [Discord Developer Portal](https://discord.com/developers/applications)

4. 変数を設定

```shell
cd sample-social-login
cp conf/application.example.conf conf/application.conf
vi conf/application.conf
```
(TODO: .envファイルを利用したかったですが、Play Frameworkの読み込みがうまくいかなかったため、application.confに直接記述しています。)


5. アプリケーションを起動

```shell
cd sample-social-login
sbt run
```

## 参考

[New Application - 3.0.x](https://www.playframework.com/documentation/3.0.x/NewApplication)
