package com.gitbitex.openapi.controller;

import com.gitbitex.marketdata.entity.Account;
import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.openapi.model.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {
    private final AccountManager accountManager;

    @GetMapping("/accounts")
    public List<AccountDto> getAccounts(@RequestParam(name = "currency") List<String> currencies,
                                        @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<Account> accounts = accountManager.getAccounts(currentUser.getId());
        Map<String, Account> accountByCurrency = accounts.stream()
                .collect(Collectors.toMap(Account::getCurrency, x -> x));

        List<AccountDto> accountDtoList = new ArrayList<>();
        for (String currency : currencies) {
            Account account = accountByCurrency.get(currency);
            if (account != null) {
                accountDtoList.add(accountDto(account));
            } else {
                AccountDto accountDto = new AccountDto();
                accountDto.setCurrency(currency);
                accountDto.setAvailable("0");
                accountDto.setHold("0");
                accountDtoList.add(accountDto);
            }
        }
        return accountDtoList;
    }

    private AccountDto accountDto(Account account) {
        AccountDto accountDto = new AccountDto();
        accountDto.setId(account.getId());
        accountDto.setCurrency(account.getCurrency());
        accountDto.setAvailable(account.getAvailable() != null ? account.getAvailable().toPlainString() : "0");
        accountDto.setHold(account.getHold() != null ? account.getHold().toPlainString() : "0");
        return accountDto;
    }

}
