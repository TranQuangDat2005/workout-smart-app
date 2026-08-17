package com.workoutsmart.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private JwtProvider provider() {
        return new JwtProvider("test-secret-key-for-jwt-provider-tests-0123456789", 15L);
    }

    @Test
    void generateAndParseRoundTrip() {
        JwtProvider jwt = provider();
        String token = jwt.generateAccessToken(42L, "user@example.com", "user");

        Claims claims = jwt.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals("user@example.com", claims.get("email"));
        assertEquals("user", claims.get("role"));
    }

    @Test
    void tokenHasExpiryInFuture() {
        JwtProvider jwt = provider();
        String token = jwt.generateAccessToken(1L, "a@b.c", "user");

        Claims claims = jwt.parse(token);
        assertTrue(claims.getExpiration().toInstant().isAfter(Instant.now()));
        assertTrue(claims.getExpiration().toInstant().isBefore(Instant.now().plusSeconds(16 * 60)));
    }

    @Test
    void getAccessExpirySecondsReturnsConfigured() {
        assertEquals(900L, provider().getAccessExpirySeconds());
    }

    @Test
    void blankSecretThrowsOnConstruction() {
        assertThrows(IllegalStateException.class, () -> new JwtProvider("", 15L));
        assertThrows(IllegalStateException.class, () -> new JwtProvider(null, 15L));
    }

    @Test
    void parseInvalidTokenThrows() {
        JwtProvider jwt = provider();
        assertThrows(Exception.class, () -> jwt.parse("not-a-jwt"));
        assertNotNull(jwt);
    }
}
