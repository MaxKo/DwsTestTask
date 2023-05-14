package com.dws.challenge.exception;

import com.dws.challenge.domain.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
public class InsufficientBalanceAmountException extends RuntimeException {
    private Account senderAccount;
    private Account receiverAccount;
    private BigDecimal requestedAmount;


    public String getReceiverMessage() {
        return  "Transfer of " + getRequestedAmount()
                + " amount cannot be done from " + getSenderAccount().getAccountId()
                + " due to insufficient balance amount";

    }

    public String getSenderMessage() {
        return  "Transfer of " + getRequestedAmount()
                + " amount cannot be done to " + getReceiverAccount().getAccountId()
                + " due to insufficient balance amount";

    }
}
