package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsBalanceTransferControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Captor
    ArgumentCaptor<String> message;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";

    @BeforeEach
    void prepareMockMvc () throws Exception {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-0001\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-0002\",\"balance\":2000}")).andExpect(status().isCreated());
    }

    @Test
    void transferNegativeAmount() throws Exception {
        this.mockMvc.perform(put("/v1/accounts/balanceTransfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":\"Id-0001\", \"receiverAccountId\":\"Id-0002\",\"amount\":-10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferOneSuccess() throws Exception {
        this.mockMvc.perform(put("/v1/accounts/balanceTransfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":\"Id-0001\", \"receiverAccountId\":\"Id-0002\",\"amount\":500}"))
                .andExpect(status().isOk());

        assertEquals(new BigDecimal(500), accountsService.getAccount("Id-0001").getBalance());
        assertEquals(new BigDecimal(2500), accountsService.getAccount("Id-0002").getBalance());

        verify(notificationService, times(2)).notifyAboutTransfer(any(Account.class), message.capture());

        String[] expected = {"Transfer of 500 amount completed from Id-0001",
                "Transfer of 500 amount completed to Id-0002"};
        assertArrayEquals(expected, message.getAllValues().toArray());
    }

    @Test
    void transferOneNotSuccess() throws Exception {
        this.mockMvc.perform(put("/v1/accounts/balanceTransfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderAccountId\":\"Id-0001\", \"receiverAccountId\":\"Id-0002\",\"amount\":1500}"))
                .andExpect(status().isBadRequest());

        assertEquals(new BigDecimal(1000), accountsService.getAccount("Id-0001").getBalance());
        assertEquals(new BigDecimal(2000), accountsService.getAccount("Id-0002").getBalance());

        verify(notificationService, times(2)).notifyAboutTransfer(any(Account.class), message.capture());

        String[] expected = {"Transfer of 1500 amount cannot be done from Id-0001 due to insufficient balance amount",
                "Transfer of 1500 amount cannot be done to Id-0002 due to insufficient balance amount"};
        assertArrayEquals(expected, message.getAllValues().toArray());
    }

    ExecutorService executorService = new ForkJoinPool(3);

    @RepeatedTest(100)
    void transferExperimentalRaceConditional_totalAmountShouldBeTheSame() throws Exception {
        // Create multiple threads to simulate concurrent transfers
        var t1 = CompletableFuture
                .supplyAsync(() -> transferThread("Id-0001", "Id-0002", "800"), executorService);
        var t2 = CompletableFuture
                .supplyAsync(() -> transferThread("Id-0002", "Id-0001", "1800"), executorService);
        var t3 = CompletableFuture
                .supplyAsync(() -> transferThread("Id-0001", "Id-0002", "200"), executorService);
        var t4 = CompletableFuture
                .supplyAsync(() -> transferThread("Id-0002", "Id-0001", "500"), executorService);
        var t5 = CompletableFuture
                .supplyAsync(() -> transferThread("Id-0001", "Id-0002", "2200"), executorService);
        var t6 = CompletableFuture
                .supplyAsync(() -> transferThread("Id-0002", "Id-0001", "700"), executorService);

        // Wait for the threads to complete
        CompletableFuture.allOf(t1, t2, t3, t4, t5, t6).get();

        System.out.println(ANSI_RED + "------------ ID-0001 = " + accountsService.getAccount("Id-0001").getBalance()
                + ANSI_BLUE + "------------------- ID-0002 = " + accountsService.getAccount("Id-0002").getBalance() +
                "------------------ " + ANSI_RESET + " ---------------- --------------");

        assertEquals(new BigDecimal(3000),
                accountsService.getAccount("Id-0001").getBalance()
                    .add(accountsService.getAccount("Id-0002").getBalance()),
                "Total balance amount for both accounts should always be the same " +
                        "despite race condition transfer issues");
    }

    @SneakyThrows
    boolean transferThread(String sender, String receiver, String amount) {
        try {
            this.mockMvc.perform(put("/v1/accounts/balanceTransfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"senderAccountId\":\"" + sender
                                    + "\", \"receiverAccountId\":\"" + receiver
                                    + "\",\"amount\":" + amount + "}"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        return true;
    }

}
