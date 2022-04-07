<p align="center"><img width="40%" src="https://getbitex.oss-cn-beijing.aliyuncs.com/projects/image/logo.svg" /></p>

GitBitEx is an open source cryptocurrency exchange.

## Quick Start

```shell
# start zookeeper
docker run -d --name zookeeper-server \
  --network host \
  -e ALLOW_ANONYMOUS_LOGIN=yes \
  bitnami/zookeeper:latest

# start kafka
docker run -d --name kafka-server \
  --network host \
  -e ALLOW_PLAINTEXT_LISTENER=yes \
  -e KAFKA_CFG_ZOOKEEPER_CONNECT=127.0.0.1:2181 \
  bitnami/kafka:latest
  

docker exec -it kafka-server  /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic account-command --partitions=8  --bootstrap-server localhost:9092
docker exec -it kafka-server  /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic order-command --partitions=8  --bootstrap-server localhost:9092
# Note that the order book command must be ordered, so the partitions of topic must be 1
docker exec -it kafka-server  /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic BTC-USDT-order-book-command --partitions=1  --bootstrap-server localhost:9092
# Note that the order book log must be ordered, so the partitions of topic must be 1
docker exec -it kafka-server  /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic BTC-USDT-order-book-log --partitions=1  --bootstrap-server localhost:9092

# start redis
docker run -d --name redis-server \
  --network=host \
  redis

# start mysql
docker run -d --name mysql \
  --network=host \
  -e MYSQL_ROOT_PASSWORD=123456 \
  mysql:5.7

# Please wait for MySQL to start before executing the following command
docker exec -it mysql mysql -uroot -p123456 -e "create database gitbitex;"

# start gitbitex
# open the browser and visit http://127.0.0.1:4567/trade/BTC-USDT
docker run -d --name gitbitex \
  --network=host \
  greensheng/gitbitex

```

## Demo

You can view the demo website

http://gitbitex.cloud/trade/BTC-USDT

