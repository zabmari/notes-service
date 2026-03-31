package com.marika.notesservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class ItemErrorHandlingIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("notes")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldReturn404WhenItemNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/items/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenTitleMissing() throws Exception {
        UUID id = UUID.randomUUID();

        String invalidJson = """
                {
                  "content" : "Some content",
                  "version" : 0
                }
               """;

        mockMvc.perform(
                patch("/items/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenUserIsNotOwner() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/items/" + id))
                .andExpect(status().isForbidden());
    }
}
