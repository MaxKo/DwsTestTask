package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Log4j2
public class Account {
    /*** in order to make balance manipulation threadsafe and avoid block whole application should be
     * lock object per each account for individual monitor
     * this solution allows to manipulate balance thread safe and able processed simultaneously
     * for different accounts
      */
    @JsonIgnore
    private final Lock lock = new ReentrantLock();

    @NotNull
    @NotEmpty
    private final String accountId;

    //added to fix existing tests
    @Min(0)
    @NotNull
    private BigDecimal balance;

    public Account(String accountId) {
        this.accountId = accountId;
        this.balance = BigDecimal.ZERO;
    }

    @JsonCreator
    public Account(@JsonProperty("accountId") String accountId,
                   @JsonProperty("balance") BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public void deposit(BigDecimal amount) {
        lock.lock();
        try {
            balance = balance.add(amount);
            log.info("Deposited " + amount + " to the " + accountId + " account. New balance: " + balance);
        } finally {
            lock.unlock();
        }
    }

    public boolean withdraw(BigDecimal amount) {
        lock.lock();
        try {
            if (balance.compareTo(amount) > 0) {
                balance = balance.subtract(amount);
                log.info("Withdrawn " + amount + " from the " + accountId + " account. New balance: " + balance);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public void setBalance(BigDecimal balance) {
        lock.lock();
        try {
            this.balance = balance;
        } finally {
            lock.unlock();
        }
    }

}
