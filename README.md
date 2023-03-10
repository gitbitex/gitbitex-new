<p align="center"><img width="40%" src="https://getbitex.oss-cn-beijing.aliyuncs.com/projects/image/logo.svg" /></p>

GitBitEx is an open source cryptocurrency exchange.

![微信图片_20220417184255](https://user-images.githubusercontent.com/4486680/163711067-8543457a-5b13-4131-bbd7-254860a580dc.png)


## Features
- All in memory matching engine
- Support distributed deployment (standby mode：only one matching engine is running at the same time)
- Support 100000 orders per second
- Support the replay of matching engine commands and ensure that each result is completely consistent


## Quick Start
### Prerequisites
- install docker
- install jdk
- update your **/etc/hosts** file. (required for mongodb-replica-set：https://github.com/UpSync-Dev/docker-compose-mongo-replica-set)
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
(**If you want to build an exchange, you can hire me. I'm looking for a part-time job remotely 
because I lost a lot of money on the damn digital currency.**)
