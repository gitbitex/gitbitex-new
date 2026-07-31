package com.gitbitex.marketdata.manager;

import com.gitbitex.marketdata.entity.Wallet;
import com.gitbitex.marketdata.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class WalletManager {
    private final WalletRepository walletRepository;
    private final BlockchainAddressGenerator blockchainAddressGenerator;

    public WalletManager(WalletRepository walletRepository, 
                        BlockchainAddressGenerator blockchainAddressGenerator) {
        this.walletRepository = walletRepository;
        this.blockchainAddressGenerator = blockchainAddressGenerator;
    }

    /**
     * Получить все кошельки пользователя
     */
    public List<Wallet> getWallets(String userId) {
        return walletRepository.findWalletsByUserId(userId);
    }

    /**
     * Получить кошелек по ID
     */
    public Wallet getWalletById(String walletId) {
        return walletRepository.findWalletById(walletId);
    }

    /**
     * Получить или создать кошелек для пользователя и валюты
     */
    public Wallet getOrCreateWallet(String userId, String currency) {
        Wallet wallet = walletRepository.findWalletByUserIdAndCurrency(userId, currency);
        if (wallet == null) {
            wallet = createWallet(userId, currency);
        }
        return wallet;
    }

    /**
     * Создать новый кошелек
     */
    public Wallet createWallet(String userId, String currency) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID().toString());
        wallet.setUserId(userId);
        wallet.setCurrency(currency);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setLocked(BigDecimal.ZERO);
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(new Date());
        wallet.setUpdatedAt(new Date());
        
        // Генерируем адрес кошелька для депозита
        String address = blockchainAddressGenerator.generateAddress(currency);
        wallet.setAddress(address);
        
        walletRepository.save(wallet);
        logger.info("Created wallet for user={}, currency={}, address={}", userId, currency, address);
        return wallet;
    }

    /**
     * Обновить баланс кошелька (пополнение)
     */
    public void credit(String walletId, BigDecimal amount) {
        walletRepository.updateBalance(walletId, amount, false);
        logger.info("Credited wallet={} with amount={}", walletId, amount);
    }

    /**
     * Обновить баланс кошелька (списание)
     */
    public void debit(String walletId, BigDecimal amount) {
        walletRepository.updateBalance(walletId, amount, true);
        logger.info("Debited wallet={} with amount={}", walletId, amount);
    }

    /**
     * Заблокировать средства на кошельке (для ордеров)
     */
    public void lockFunds(String walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findWalletById(walletId);
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found: " + walletId);
        }
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLocked(wallet.getLocked().add(amount));
        wallet.setUpdatedAt(new Date());
        
        walletRepository.save(wallet);
        logger.info("Locked funds in wallet={}, amount={}", walletId, amount);
    }

    /**
     * Разблокировать средства на кошельке
     */
    public void unlockFunds(String walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findWalletById(walletId);
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found: " + walletId);
        }
        
        if (wallet.getLocked().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient locked funds");
        }
        
        wallet.setLocked(wallet.getLocked().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(new Date());
        
        walletRepository.save(wallet);
        logger.info("Unlocked funds in wallet={}, amount={}", walletId, amount);
    }

    /**
     * Перевод средств между кошельками
     */
    public void transfer(String fromWalletId, String toWalletId, BigDecimal amount) {
        debit(fromWalletId, amount);
        credit(toWalletId, amount);
        logger.info("Transferred from={} to={} amount={}", fromWalletId, toWalletId, amount);
    }

    /**
     * Сохранить все изменения в кошельках
     */
    public void saveAll(List<Wallet> wallets) {
        wallets.forEach(wallet -> wallet.setUpdatedAt(new Date()));
        walletRepository.saveAll(wallets);
    }
}
