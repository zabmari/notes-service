package com.marika.notesservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class OptimisticLockingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpSecurity() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test-user", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldReturnConflictWhenVersionIsOutdated() throws Exception {
        String createJson = """
                {
                    "title" : "First title",
                    "content" : "First content"
                }
                """;

        MvcResult mvcResult = mockMvc.perform(
                        post("/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        UUID itemID = UUID.fromString(JsonPath.read(response, "$.id"));

        String firstUpdate = """
                {
                    "title" : "Updated Title",
                    "content" : "Updated Content",
                    "version" : 0
                }
                """;

        mockMvc.perform(patch("/items/" + itemID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstUpdate))
                .andExpect(status().isOk());

        String secondUpdate = """
                {
                    "title" : "Outdated Title",
                    "content" : "Outdated Content",
                    "version" : 0
                }
                """;

        mockMvc.perform(patch("/items/" + itemID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondUpdate))
                .andExpect(status().isConflict());
    }
}
