package com.portfolio.helpdesk.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.portfolio.helpdesk.auth.application.JwtService;
import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;

class JwtServiceTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters-long";

    @Test
    void generatedTokenContainsUserAndIsValid() {
        JwtService jwtService = new JwtService(SECRET, 10);
        UserAccount user = new UserAccount("user@test.local", "ignored", "Test", "User");
        user.addRole(new Role(RoleName.USER));

        String token = jwtService.generateToken(user);

        assertEquals("user@test.local", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }
}
