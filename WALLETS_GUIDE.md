# Руководство по использованию блокчейн-кошельков в GitBitEx

## Обзор

GitBitEx включает реализацию системы блокчейн-кошельков для управления криптовалютами пользователей. Система поддерживает:
- Создание кошельков для различных криптовалют (BTC, ETH, LTC, USDT)
- Генерацию адресов для депозитов
- Пополнение и вывод средств
- Переводы между кошельками
- Блокировку/разблокировку средств для торговых операций

## Архитектура

### Основные компоненты

1. **WalletManager** (`WalletManager.java`) - сервис для управления кошельками
2. **BlockchainAddressGenerator** (`BlockchainAddressGenerator.java`) - генерация адресов криптовалют
3. **WalletController** (`WalletController.java`) - REST API для работы с кошельками
4. **Wallet** (`Wallet.java`) - entity кошелька
5. **DepositCommand/WithdrawalCommand** - команды для движка сопоставления

## API Кошельков

### 1. Получить все кошельки пользователя

```bash
GET /api/wallets
Headers: Authorization (требуется аутентификация)
```

**Ответ:**
```json
[
  {
    "id": "wallet-id",
    "userId": "user-id",
    "currency": "BTC",
    "address": "bc1q...",
    "balance": "1.5",
    "locked": "0.5",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

### 2. Создать кошелек для валюты

```bash
POST /api/wallets
Content-Type: application/json

{
  "currency": "BTC"
}
```

**Ответ:** Объект WalletDto с сгенерированным адресом

### 3. Получить адрес для депозита

```bash
GET /api/wallets/{walletId}/address
```

**Ответ:**
```json
{
  "address": "bc1q..."
}
```

### 4. Пополнить кошелек (депозит)

```bash
POST /api/wallets/deposit
Content-Type: application/json

{
  "walletId": "wallet-id",
  "amount": "1.5"
}
```

### 5. Вывод средств

```bash
POST /api/wallets/withdraw
Content-Type: application/json

{
  "walletId": "wallet-id",
  "amount": "0.5",
  "address": "external-wallet-address"
}
```

⚠️ **Внимание:** В текущей реализации вывод только списывает средства. Для production нужна интеграция с реальным блокчейном.

### 6. Перевод между кошельками

```bash
POST /api/wallets/transfer
Content-Type: application/json

{
  "fromWalletId": "source-wallet-id",
  "toWalletId": "destination-wallet-id",
  "amount": "0.1"
}
```

### 7. Заблокировать средства (для ордеров)

```bash
POST /api/wallets/{walletId}/lock?amount=0.5
```

### 8. Разблокировать средства

```bash
POST /api/wallets/{walletId}/unlock?amount=0.5
```

## Поддерживаемые криптовалюты

| Валюта | Префикс адреса | Пример |
|--------|---------------|---------|
| BTC | `bc1q`, `1`, `3` | `bc1qxy...` |
| ETH | `0x` | `0x742d35Cc6634C0532925a3b844Bc454e4438f44e` |
| LTC | `L`, `M`, `3` | `LhK7Sd...` |
| USDT (TRC20) | `T` | `T9yD14Nj9j7xAB4dbGeiX9h8zzCu53LjWy` |

## Интеграция с Matching Engine

Для обработки депозитов и выводов в движке сопоставления используются команды:

### DepositCommand
```java
DepositCommand command = new DepositCommand();
command.setUserId("user-id");
command.setCurrency("BTC");
command.setAmount(new BigDecimal("1.5"));
command.setTransactionId("tx-hash");
// Отправить в Kafka через CommandDispatcher
```

### WithdrawalCommand
```java
WithdrawalCommand command = new WithdrawalCommand();
command.setUserId("user-id");
command.setCurrency("ETH");
command.setAmount(new BigDecimal("0.5"));
command.setTransactionId("tx-hash");
command.setAddress("external-address");
// Отправить в Kafka через CommandDispatcher
```

## Пример использования

### Шаг 1: Запуск приложения

```bash
# Запустить зависимости (MongoDB, Kafka, Redis)
docker compose up -d

# Собрать проект
mvn clean package -Dmaven.test.skip=true

# Запустить приложение
cd target
java -jar gitbitex-0.0.1-SNAPSHOT.jar
```

### Шаг 2: Создать продукт (торговую пару)

```bash
curl -X PUT -H "Content-Type:application/json" \
  http://127.0.0.1/api/admin/products \
  -d '{"baseCurrency":"BTC","quoteCurrency":"USDT"}'
```

### Шаг 3: Создать кошелек для пользователя

```bash
curl -X POST -H "Content-Type:application/json" \
  http://127.0.0.1/api/wallets \
  -d '{"currency":"BTC"}'
```

### Шаг 4: Пополнить кошелек (тестирование)

```bash
curl -X POST -H "Content-Type:application/json" \
  http://127.0.0.1/api/wallets/deposit \
  -d '{"walletId":"wallet-id","amount":"1.5"}'
```

### Шаг 5: Проверить баланс

```bash
curl http://127.0.0.1/api/wallets
```

## Production considerations

### ⚠️ Важно для production

1. **Генерация адресов**: Текущая реализация использует упрощённую генерацию. 
   Для production используйте:
   - Bitcoin: библиотека `bitcoinj`
   - Ethereum: библиотека `web3j`
   - Litecoin: библиотека `litecoinj`

2. **Безопасность приватных ключей**: 
   - Храните ключи в аппаратных кошельках (HSM)
   - Используйте мультиподпись для крупных сумм
   - Никогда не храните ключи в базе данных

3. **Мониторинг блокчейна**:
   - Реализуйте слушатели транзакций для автоматического зачисления депозитов
   - Отслеживайте подтверждения транзакций
   - Обрабатывайте реорганизации блокчейна

4. **Вывод средств**:
   - Реализуйте очередь на вывод
   - Требуйте подтверждение email/2FA
   - Установите лимиты на вывод
   - Используйте холодные кошельки для хранения основных средств

5. **Соответствие регуляторным требованиям**:
   - KYC/AML проверки
   - Мониторинг подозрительных транзакций
   - Ведение аудиторского лога

## Структура базы данных

Коллекция `wallets` в MongoDB:

```javascript
{
  _id: "wallet-id",
  userId: "user-id",
  currency: "BTC",
  address: "bc1q...",
  balance: NumberDecimal("1.5"),
  locked: NumberDecimal("0.5"),
  status: "ACTIVE", // ACTIVE, FROZEN
  createdAt: ISODate("2024-01-01T00:00:00Z"),
  updatedAt: ISODate("2024-01-01T00:00:00Z")
}
```

## Расширение функциональности

### Добавление новой криптовалюты

1. Добавьте метод генерации адреса в `BlockchainAddressGenerator`:
```java
public String generateNewCoinAddress() {
    // Реализация генерации
}
```

2. Обновите метод `generateAddress()`:
```java
if ("NEWCOIN".equals(normalizedCurrency)) {
    return generateNewCoinAddress();
}
```

3. Добавьте проверку в `validateAddress()`

### Интеграция с внешними провайдерами

Для production рассмотрите интеграцию с:
- BitGo (корпоративные кошельки)
- Coinbase Custody
- Fireblocks
- Собственные ноды криптовалют

## Тестирование

```bash
# Запустить тесты
mvn test

# Тестирование API
curl http://127.0.0.1/api/wallets
```

## Мониторинг

Используйте Prometheus метрики:
- `gbe_wallet_operations_total` - количество операций с кошельками
- `gbe_wallet_balance_total` - общие балансы по валютам

Доступно на: `http://127.0.0.1:7002/actuator/prometheus`

## Поддержка

Для вопросов и предложений обращайтесь к документации проекта или создавайте issues на GitHub.
