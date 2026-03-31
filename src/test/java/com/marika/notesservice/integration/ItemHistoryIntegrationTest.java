package com.marika.notesservice.integration;

import com.jayway.jsonpath.JsonPath;
import com.marika.notesservice.model.User;
import com.marika.notesservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class ItemHistoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("notes").withUsername("root").withPassword("root");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setupSecurity() {
        userRepository.deleteAll();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("test_user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setLogin("test_user");
        user.setPassword("123456789");
        userRepository.save(user);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void historyContainsAllRevisions() throws Exception {

        String createJson = """
                        {
                        "title" : "First title",
                        "content" : "First content"
                       }
                """;

        MvcResult mvcResult = mockMvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content(createJson)).andExpect(status().isCreated()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        UUID itemID = UUID.fromString(JsonPath.read(response, "$.id"));

        int version = JsonPath.read(response, "$.version");

        String update1 = """
                        {
                        "title" : "Second title",
                        "content" : "Second content",
                        "version" : %d
                       }
                """.formatted(version);

        mockMvc.perform(patch("/items/" + itemID).contentType(MediaType.APPLICATION_JSON).content(update1)).andExpect(status().isOk());

        String update2 = """
                        {
                        "title" : "Third title",
                        "content" : "Third content",
                        "version" : %d
                       }
                """.formatted(version + 1);

        mockMvc.perform(patch("/items/" + itemID).contentType(MediaType.APPLICATION_JSON).content(update2)).andExpect(status().isOk());

        MvcResult historyResult = mockMvc.perform(get("/items/" + itemID + "/history")).andExpect(status().isOk()).andReturn();

        String historyJson = historyResult.getResponse().getContentAsString();

        List<?> revisions = JsonPath.read(historyJson, "$");
        assertEquals(3, revisions.size());
    }
}
