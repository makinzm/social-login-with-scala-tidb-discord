# social-login-with-scala-tidb-discord

[New Application - 3.0.x](https://www.playframework.com/documentation/3.0.x/NewApplication)

```shell
docker compose up -d

docker run -it --rm --network host -v $(pwd)/tidb/mytable.sql:/mytable.sql mysql mysql -h 127.0.0.1 -P 4000 -u root -e "source /mytable.sql"
docker run -it --rm --network host mysql mysql -h 127.0.0.1 -P 4000 -u root
```

Edit "conf/application.conf" (not .env)

```shell
cd sample-social-login
bash run.sh
```
