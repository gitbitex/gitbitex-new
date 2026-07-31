package com.gitbitex.openapi.controller;

import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.entity.Wallet;
import com.gitbitex.marketdata.manager.WalletManager;
import com.gitbitex.openapi.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WalletController {
    private final WalletManager walletManager;

    /**
     * Получить все кошельки текущего пользователя
     */
    @GetMapping("/wallets")
    public List<WalletDto> getWallets(@RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        List<Wallet> wallets = walletManager.getWallets(currentUser.getId());
        return wallets.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить кошелек по ID
     */
    @GetMapping("/wallets/{walletId}")
    public WalletDto getWallet(@PathVariable String walletId, 
                               @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getWalletById(walletId);
        if (wallet == null || !wallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        return toDto(wallet);
    }

    /**
     * Создать новый кошелек для валюты
     */
    @PostMapping("/wallets")
    public WalletDto createWallet(@RequestBody CreateWalletRequest request,
                                  @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getOrCreateWallet(currentUser.getId(), request.getCurrency());
        logger.info("Created/accessed wallet for user={}, currency={}", currentUser.getId(), request.getCurrency());
        return toDto(wallet);
    }

    /**
     * Пополнить кошелек (депозит) - для тестирования/администрирования
     */
    @PostMapping("/wallets/deposit")
    public ResponseEntity<Response<Void>> deposit(@RequestBody DepositRequest request,
                                                  @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getWalletById(request.getWalletId());
        if (wallet == null || !wallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        walletManager.credit(request.getWalletId(), request.getAmount());
        return ResponseEntity.ok(Response.success());
    }

    /**
     * Вывод средств - для тестирования/администрирования
     * В production здесь должна быть интеграция с блокчейном
     */
    @PostMapping("/wallets/withdraw")
    public ResponseEntity<Response<Void>> withdraw(@RequestBody WithdrawRequest request,
                                                   @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getWalletById(request.getWalletId());
        if (wallet == null || !wallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        // Проверка достаточности средств
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }
        
        walletManager.debit(request.getWalletId(), request.getAmount());
        logger.info("Withdrawal requested: wallet={}, amount={}, address={}", 
                request.getWalletId(), request.getAmount(), request.getAddress());
        
        // В production: создать заявку на вывод и отправить в блокчейн
        return ResponseEntity.ok(Response.success());
    }

    /**
     * Перевод между кошельками пользователей
     */
    @PostMapping("/wallets/transfer")
    public ResponseEntity<Response<Void>> transfer(@RequestBody TransferRequest request,
                                                   @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet fromWallet = walletManager.getWalletById(request.getFromWalletId());
        Wallet toWallet = walletManager.getWalletById(request.getToWalletId());
        
        if (fromWallet == null || !fromWallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source wallet not found or access denied");
        }
        
        if (toWallet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination wallet not found");
        }
        
        // Проверка достаточности средств
        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }
        
        walletManager.transfer(request.getFromWalletId(), request.getToWalletId(), request.getAmount());
        return ResponseEntity.ok(Response.success());
    }

    /**
     * Получить адрес кошелька для депозита
     */
    @GetMapping("/wallets/{walletId}/address")
    public WalletAddressDto getDepositAddress(@PathVariable String walletId,
                                              @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getWalletById(walletId);
        if (wallet == null || !wallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        WalletAddressDto dto = new WalletAddressDto();
        // В production: сгенерировать уникальный адрес для депозита
        dto.setAddress(wallet.getAddress() != null ? wallet.getAddress() : "GENERATED_ADDRESS_" + walletId);
        return dto;
    }

    /**
     * Заблокировать средства на кошельке (для торговых операций)
     */
    @PostMapping("/wallets/{walletId}/lock")
    public ResponseEntity<Response<Void>> lockFunds(@PathVariable String walletId,
                                                    @RequestParam BigDecimal amount,
                                                    @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getWalletById(walletId);
        if (wallet == null || !wallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        walletManager.lockFunds(walletId, amount);
        return ResponseEntity.ok(Response.success());
    }

    /**
     * Разблокировать средства на кошельке
     */
    @PostMapping("/wallets/{walletId}/unlock")
    public ResponseEntity<Response<Void>> unlockFunds(@PathVariable String walletId,
                                                      @RequestParam BigDecimal amount,
                                                      @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Wallet wallet = walletManager.getWalletById(walletId);
        if (wallet == null || !wallet.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        walletManager.unlockFunds(walletId, amount);
        return ResponseEntity.ok(Response.success());
    }

    private WalletDto toDto(Wallet wallet) {
        WalletDto dto = new WalletDto();
        dto.setId(wallet.getId());
        dto.setUserId(wallet.getUserId());
        dto.setCurrency(wallet.getCurrency());
        dto.setAddress(wallet.getAddress());
        dto.setBalance(wallet.getBalance() != null ? wallet.getBalance().toPlainString() : "0");
        dto.setLocked(wallet.getLocked() != null ? wallet.getLocked().toPlainString() : "0");
        dto.setStatus(wallet.getStatus());
        dto.setCreatedAt(wallet.getCreatedAt());
        dto.setUpdatedAt(wallet.getUpdatedAt());
        return dto;
    }
}
