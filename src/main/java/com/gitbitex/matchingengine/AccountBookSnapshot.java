package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.List;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountBookSnapshot {
    private List<Account> accounts=new ArrayList<>();
}
