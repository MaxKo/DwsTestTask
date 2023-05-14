package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AccountBalanceTransferServiceTest {

    @MockBean
    private NotificationService notificationService;

    @Autowired
    AccountBalanceTransferService transferService;

    @Test
    public void transferTest() {
        Account account1 = new Account("Id-0001", new BigDecimal(1000));
        Account account2 = new Account("Id-0002", new BigDecimal(2000));

        // Create multiple threads to simulate concurrent transfers
        Thread thread1 = new Thread(() -> transferService.transferThreadSafe(account1, account2, new BigDecimal(1500)));
        Thread thread2 = new Thread(() -> transferService.transferThreadSafe(account2, account1, new BigDecimal(700)));

        thread1.start();
        thread2.start();

        // Wait for the threads to complete
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print the final balances
        System.out.println("Final balance of Account " + account1.getAccountId() + ": " + account1.getBalance());
        System.out.println("Final balance of Account " + account2.getAccountId() + ": " + account2.getBalance());

        System.out.println("---------   -------------------    ------------------------     ----------------    ---------------");
    }
}
