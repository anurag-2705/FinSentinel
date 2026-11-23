package com.finsentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterCustomerSuccessfully() throws Exception {

        String email = "customer-" + UUID.randomUUID() + "@example.com";

        String request = """
                {
                    "firstName": "Anurag",
                    "lastName": "Test",
                    "email": "%s",
                    "phoneNumber": null,
                    "password": "StrongPassword123!"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        String request = """
                {
                    "firstName": "Anurag",
                    "lastName": "Test",
                    "email": "invalid-email",
                    "phoneNumber": null,
                    "password": "StrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void shouldRejectShortPassword() throws Exception {

        String email = "short-" + UUID.randomUUID() + "@example.com";

        String request = """
                {
                    "firstName": "Anurag",
                    "lastName": "Test",
                    "email": "%s",
                    "phoneNumber": null,
                    "password": "123"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}