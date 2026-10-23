package com.portfolio.helpdesk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.helpdesk.auth.application.LoginAttemptService;
import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.RoleRepository;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void validCredentialsReturnABearerToken() throws Exception {
        String email = "security-test-" + UUID.randomUUID() + "@example.test";
        String password = "Correct-Pass-123!";
        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();
        UserAccount user = new UserAccount(email, passwordEncoder.encode(password), "Security", "Test");
        user.addRole(userRole);
        userRepository.saveAndFlush(user);

        try {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
        } finally {
            userRepository.delete(user);
        }
    }

    @Test
    void currentUserEndpointRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectCredentialsReturnGenericUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"never-existing-account@example.test\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("No se pudo iniciar sesión con esas credenciales."));
    }

    @Test
    void malformedBearerTokenCannotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this-is-not-a-valid-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailedLoginsAreRateLimitedWithRetryAfter() throws Exception {
        String body = "{\"email\":\"rate-limit-user@example.test\",\"password\":\"wrong-password\"}";
        for (int attempt = 0; attempt < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
            .andExpect(jsonPath("$.message").value("Demasiados intentos. Inténtalo de nuevo más tarde."));
    }

    @Test
    void corsAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void corsRejectsUnlistedOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isForbidden());
    }

    @Test
    void technicianDirectoryRequiresRoleAndReturnsTechnicians() throws Exception {
        String technicianEmail = "directory-tech-" + UUID.randomUUID() + "@example.test";
        String userEmail = "directory-user-" + UUID.randomUUID() + "@example.test";
        String password = "Directory-Pass-123!";
        Role technicianRole = roleRepository.findByName(RoleName.TECHNICIAN).orElseThrow();
        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();
        UserAccount technician = new UserAccount(technicianEmail, passwordEncoder.encode(password), "Directory", "Technician");
        technician.addRole(technicianRole);
        UserAccount user = new UserAccount(userEmail, passwordEncoder.encode(password), "Directory", "User");
        user.addRole(userRole);
        userRepository.saveAndFlush(technician);
        userRepository.saveAndFlush(user);
        try {
            String technicianToken = loginAndReadToken(technicianEmail, password);
            mockMvc.perform(get("/api/users/technicians").header(HttpHeaders.AUTHORIZATION, "Bearer " + technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '%s')]".formatted(technicianEmail)).exists());

            String userToken = loginAndReadToken(userEmail, password);
            mockMvc.perform(get("/api/users/technicians").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
        } finally {
            userRepository.delete(user);
            userRepository.delete(technician);
        }
    }

    private String loginAndReadToken(String email, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        return response.get("token").asText();
    }
}
