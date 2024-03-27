package com.gitbitex.matchingengine.message;

import com.gitbitex.matchingengine.Account;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountMessage extends Message {
    private Account account;

    public AccountMessage() {
        this.setMessageType(MessageType.ACCOUNT);
    }
}
