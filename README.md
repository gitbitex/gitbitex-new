<p align="center"><img width="40%" src="https://getbitex.oss-cn-beijing.aliyuncs.com/projects/image/logo.svg" /></p>

GitBitEX is an open source cryptocurrency exchange.

![微信图片_20220417184255](https://user-images.githubusercontent.com/4486680/163711067-8543457a-5b13-4131-bbd7-254860a580dc.png)


## Features
- All in memory matching engine
- Support distributed deployment (standby mode：only one matching engine is running at the same time)
- Support 100000 orders per second (Intel(R) Core(TM) i7-10700K CPU @ 3.80GHz   3.79 GHz  32.0 GB RAM 1T SSD)
- Support the replay of matching engine commands and ensure that each result is completely consistent


## Quick Start
### Prerequisites
- Install docker
- Install jdk
- Install maven
- Update your **/etc/hosts** file. (required for mongodb-replica-set：https://github.com/UpSync-Dev/docker-compose-mongo-replica-set)
```text
127.0.0.1       mongo1
127.0.0.1       mongo2
127.0.0.1       mongo3
```

### Run

```shell
git clone https://github.com/gitbitex/gitbitex-new.git
cd gitbitex-new
docker compose up -d
mvn clean package -Dmaven.test.skip=true
cd target
java -jar gitbitex-0.0.1-SNAPSHOT.jar
#visit http://127.0.0.1/
```

## FAQ
### How do i add new product (currency pair)?
```shell
curl -X PUT -H "Content-Type:application/json" http://127.0.0.1/api/admin/products -d '{"baseCurrency":"BTC","quoteCurrency":"USDT"}'
```
### Does the project include blockchain wallets?
No, you need to implement it yourself, and then connect to gitbiex.
For example, after users recharge/withdraw, send **_DepositCommand_**/**_WithdrawalCommand_** to the matching engine
### Can I use this project for production?
Probably not. Some customers I know use this project in production, but they all have professional technicians. 
If you want to use it in production, you need professional technicians to help you. Like me . 

### How can I monitor the performance of the matching engine?
Some prometheus metrics for measuring performance have been built into the project. 
You can visit http://127.0.0.1:7002/actuator/prometheus see.You can use Prometheus and grafana to monitor.
The metrics are as follows:
- **gbe_matching_engine_command_processed_total** : The number of commands processed by the matching engine. The greater the value change, the faster the processing.
- **gbe_matching_engine_modified_object_created_total** : This value represents the number of objects that have modified,Wait to save to database.
- **gbe_matching_engine_modified_object_saved_total** : The number of modified objects that have been saved. If the difference between this value and _gbe_matching_engine_modified_object_created_total_ is too large, it means that saving to disk is too slow.
- **gbe_matching_engine_snapshot_taker_modified_objects_queue_size** : Objects that have not been written to the snapshot. This value reflects the performance of the snapshot thread.


### Where is the API document?
http://127.0.0.1/swagger-ui/index.html#/
