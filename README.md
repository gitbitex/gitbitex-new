<p align="center"><img width="40%" src="https://getbitex.oss-cn-beijing.aliyuncs.com/projects/image/logo.svg" /></p>

GitBitEx is an open source cryptocurrency exchange.

## Quick Start

```shell
docker run -d --name zookeeper-server \
  --network host \
  -e ALLOW_ANONYMOUS_LOGIN=yes \
  bitnami/zookeeper:latest

docker run -d --name kafka-server \
  --network host \
  -e ALLOW_PLAINTEXT_LISTENER=yes \
  -e KAFKA_CFG_ZOOKEEPER_CONNECT=127.0.0.1:2181 \
  bitnami/kafka:latest

docker run -d --name redis-server \
  --network=host \
  redis

docker run -d --name mysql \
  --network=host \
  -e MYSQL_ROOT_PASSWORD=123456 \
  mysql:5.7

# Please wait for MySQL to start before executing the following command
docker exec -it mysql mysql -uroot -p123456 -e "create database gitbitex;"

docker run -d --name gitbitex \
  --network=host \
  greensheng/gitbitex

```

http://127.0.0.1:4567/trade/BTC-USDT

## Demo

You can view the demo website (deployed on the spot instance and may be released at any time)

http://47.243.120.30:4567/trade/BTC-USDT

