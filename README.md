<p align="center"><img width="40%" src="https://getbitex.oss-cn-beijing.aliyuncs.com/projects/image/logo.svg" /></p>

GitBitEx is an open source cryptocurrency exchange.

![微信图片_20220417184255](https://user-images.githubusercontent.com/4486680/163711067-8543457a-5b13-4131-bbd7-254860a580dc.png)

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

## Build
The project uses **jib**(https://cloud.google.com/java/getting-started/jib?hl=en) for docker image construction, so there is no dockerfile file. You can use the following command to generate an image locally:
```shell
mvn clean compile jib:dockerBuild
```

If you don't want to use docker, you can directly build a jar package with the following command, and then start it with Java command
```shell
mvn clean package -Dmaven.test.skip=true
```


## Demo

You can view the demo website

http://gitbitex.cloud/trade/BTC-USDT

