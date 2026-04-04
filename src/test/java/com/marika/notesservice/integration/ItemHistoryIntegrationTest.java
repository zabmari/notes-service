package com.marika.notesservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
public class ItemHistoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpSecurity() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("test-user", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser(username = "test-user")
    void historyContainsAllRevisions() throws Exception {
        String createJson = """
                {
                    "title" : "First title",
                    "content" : "First content"
                }
                """;

        MvcResult mvcResult = mockMvc.perform(
                post("/items").with(user("test-user")).contentType(MediaType.APPLICATION_JSON)
                        .content(createJson)).andExpect(status().isCreated()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        UUID itemID = UUID.fromString(JsonPath.read(response, "$.id"));
        int version = JsonPath.read(response, "$.version");

        String update1 = """
                { "title" : "Second title", "content" : "Second content", "version" : %d }
                """.formatted(version);

        mockMvc.perform(
                        patch("/items/" + itemID).contentType(MediaType.APPLICATION_JSON).content(update1))
                .andExpect(status().isOk());

        String update2 = """
                { "title" : "Third title", "content" : "Third content", "version" : %d }
                """.formatted(version + 1);

        mockMvc.perform(
                        patch("/items/" + itemID).contentType(MediaType.APPLICATION_JSON).content(update2))
                .andExpect(status().isOk());

        MvcResult historyResult =
                mockMvc.perform(get("/items/" + itemID + "/history")).andExpect(status().isOk())
                        .andReturn();

        String historyResponse = historyResult.getResponse().getContentAsString();
        String lastTitle = JsonPath.read(historyResponse, "$[2].title");

        assertEquals("Third title", lastTitle);
    }

    @Test
    @WithMockUser(username = "test-user")
    void shouldReturnNotFoundForHistoryOfDeletedItem() throws Exception {
        String createJson = """
                {
                    "title" : "Title",
                    "content" : "Content"
                }
                """;

        MvcResult mvcResult = mockMvc.perform(
                post("/items").with(user("test-user")).contentType(MediaType.APPLICATION_JSON)
                        .content(createJson)).andExpect(status().isCreated()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        UUID itemId = UUID.fromString(JsonPath.read(response, "$.id"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/items/" + itemId).with(user("test-user"))).andExpect(status().isNoContent());

        mockMvc.perform(get("/items/" + itemId + "/history").with(user("test-user")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test-user")
    void shouldNotReturnDeletedItemsInList() throws Exception {
        String createJson = """
                {
                    "title" : "Title",
                    "content" : "Content"
                }
                """;

        MvcResult mvcResult = mockMvc.perform(
                post("/items").with(user("test-user")).contentType(MediaType.APPLICATION_JSON)
                        .content(createJson)).andExpect(status().isCreated()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        UUID itemId = UUID.fromString(JsonPath.read(response, "$.id"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/items/" + itemId).with(user("test-user"))).andExpect(status().isNoContent());

        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("DELETE FROM item_permissions").executeUpdate();
            return null;
        });
        mockMvc.perform(get("/items").with(user("test-user"))).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
    }
}