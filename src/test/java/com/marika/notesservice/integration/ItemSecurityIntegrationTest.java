package com.marika.notesservice.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.marika.notesservice.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
public class ItemSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnForbiddenWhenAccessingForeignItemActions() throws Exception {
        User hacker = new User();
        hacker.setLogin("hacker-user");
        hacker.setPassword("password");
        userRepository.save(hacker);

        String createJson = """
                {
                    "title" : "First title",
                    "content" : "First content"
                }
                """;

        MvcResult mvcResult = mockMvc.perform(
                        post("/items")
                                .with(user("test-user"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        UUID itemID = UUID.fromString(JsonPath.read(response, "$.id"));

        mockMvc.perform(patch("/items/" + itemID)
                        .with(user(hacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Hacked\", \"version\": 0 }"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/items/" + itemID)
                        .with(user(hacker)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/items/" + itemID + "/history")
                        .with(user(hacker)))
                .andExpect(status().isForbidden());

        UUID randomUser = UUID.randomUUID();
        String shareJson = """
                {
                    "userId" : "%s",
                    "role" : "VIEWER"
                }
                """.formatted(randomUser);

        mockMvc.perform(post("/items/" + itemID + "/share")
                        .with(user(hacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/items/" + itemID + "/share/" + randomUser)
                        .with(user(hacker)))
                .andExpect(status().isForbidden());
    }
}
