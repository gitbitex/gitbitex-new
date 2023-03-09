package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account implements Cloneable {
    private String userId;
    private String currency;
    private BigDecimal available;
    private BigDecimal hold;

    @Override
    public Account clone() {
        try {
            return (Account)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Account)) {
            return false;
        }
        Account other = (Account)obj;
        return Objects.equals(this.userId, other.userId)
            && Objects.equals(this.currency, other.currency);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (userId == null ? 0 : userId.hashCode());
        result = 31 * result + (currency == null ? 0 : currency.hashCode());
        return result;
    }
}
