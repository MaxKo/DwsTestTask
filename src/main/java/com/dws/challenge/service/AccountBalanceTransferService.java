package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AccountBalanceTransferDto;
import com.dws.challenge.exception.InsufficientBalanceAmountException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
@Log4j2
public class AccountBalanceTransferService {

    private AccountsService accountsService;

    private NotificationService notificationService;

    protected void transferThreadSafe(Account sender, Account receiver, BigDecimal amount) {
        Account firstLock = sender;
        Account secondLock = receiver;

        /* in order to avoid deadlock object locks should be always enabled in the same order.
         //order is defining by object accountId field
         //note: when "if" section commented tests may catch deadlock
         */
        if (sender.getAccountId().compareTo(receiver.getAccountId()) < 0) {
            firstLock = receiver;
            secondLock = sender;
        }

        firstLock.getLock().lock();
        try {
            secondLock.getLock().lock();
            try {
                if (sender.withdraw(amount)) {
                    receiver.deposit(amount);
                    log.info("Transferred " + amount + " from Account " + sender.getAccountId() + " to Account " + receiver.getAccountId());
                } else {
                    log.info("Insufficient balance in Account " + sender.getAccountId() + " Requested: " + amount + " but only exists: " + sender.getBalance());
                    throw new InsufficientBalanceAmountException(sender, receiver, amount);
                }
            } finally {
                secondLock.getLock().unlock();
            }
        } finally {
            firstLock.getLock().unlock();
        }
    }

    public void transferThreadSafe(AccountBalanceTransferDto amountDto) {
        Account sender = accountsService.getAccount(amountDto.getSenderAccountId());
        Account receiver = accountsService.getAccount(amountDto.getReceiverAccountId());

        transferThreadSafe(sender, receiver, amountDto.getAmount());

        notifyAccounts(sender, receiver, amountDto.getAmount());
    }

    //ToDo: this logic probably should be done fully by notificationService, so interface should be changed
    private void notifyAccounts(Account sender, Account receiver, BigDecimal amount) {
        String receiverMessage = "Transfer of " + amount
                + " amount completed from " + sender.getAccountId();
        notificationService.notifyAboutTransfer(receiver, receiverMessage);

        String senderMessage = "Transfer of " + amount
                + " amount completed to " + receiver.getAccountId();
        notificationService.notifyAboutTransfer(sender, senderMessage);
    }
}
