package com.portfolio.helpdesk.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import com.portfolio.helpdesk.auth.application.JwtService;
import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters-long";
    private static final String OTHER_SECRET = "different-test-secret-with-at-least-32-bytes";

    @Test
    void generatedTokenContainsUserAndIsValid() {
        JwtService jwtService = new JwtService(SECRET, 10);
        UserAccount user = new UserAccount("user@test.local", "ignored", "Test", "User");
        user.addRole(new Role(RoleName.USER));

        String token = jwtService.generateToken(user);

        assertEquals("user@test.local", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService jwtService = new JwtService(SECRET, 10);
        String token = Jwts.builder()
            .subject("admin@test.local")
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThrows(JwtException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 10);
        UserAccount user = new UserAccount("user@test.local", "ignored", "Test", "User");
        String token = jwtService.generateToken(user);
        String[] parts = token.split("\\.");
        char firstSignatureCharacter = parts[2].charAt(0);
        parts[2] = (firstSignatureCharacter == 'A' ? "B" : "A") + parts[2].substring(1);
        String tampered = String.join(".", parts);

        assertThrows(JwtException.class, () -> jwtService.extractUsername(tampered));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, 10);
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
            .subject("user@test.local")
            .issuedAt(Date.from(now.minusSeconds(120)))
            .expiration(Date.from(now.minusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    void rejectsShortSecretAndNonPositiveExpiration() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 30));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, 0));
    }
}
