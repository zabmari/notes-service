package com.marika.notesservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RateLimitingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void rateLimitingBlocksAfterFiveLoginAttempts() throws Exception {
        String loginJson = """
                {
                    "login": "test-user",
                    "password": "wrong-password"
                }
                """;

        // Wykonujemy 5 prób - powinny zwrócić 401 Unauthorized
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        // 6. próba powinna zostać zablokowana przez filtr Rate Limitera
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isTooManyRequests()) // Status 429
                .andExpect(header().exists("Retry-After"));
    }
}