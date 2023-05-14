package com.dws.challenge;

import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ChallengeApplicationTests {
    @MockBean
    private NotificationService notificationService;
    @Test
    void contextLoads() {
    }

}
