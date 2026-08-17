package com.workoutsmart.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.dto.AuthResponse;
import com.workoutsmart.auth.entity.RefreshToken;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtProvider jwtProvider;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtProvider, repository, userRepository, 7L);
    }

    private User testUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hash")
                .role("user")
                .build();
    }

    @Test
    void issueReturnsTokenPairAndStoresHashedRefresh() {
        when(jwtProvider.getAccessExpirySeconds()).thenReturn(900L);
        when(jwtProvider.generateAccessToken(1L, "user@example.com", "user")).thenReturn("access.jwt");

        AuthResponse response = tokenService.issue(testUser());

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(900L, response.expiresInSeconds());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertEquals(64, captor.getValue().getTokenHash().length(), "Hash phải là SHA-256 hex 64 ký tự");
        assertTrue(captor.getValue().getExpiresAt().isAfter(Instant.now().plus(Duration.ofDays(6))),
                "Refresh token phải sống ~7 ngày");
    }

    @Test
    void rotateRevokesOldAndIssuesNew() {
        String oldRaw = "old-refresh-token";
        String hash = TokenService.sha256Hex(oldRaw);
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build();
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser()));
        when(jwtProvider.generateAccessToken(1L, "user@example.com", "user")).thenReturn("access.jwt");

        AuthResponse response = tokenService.rotate(oldRaw);

        assertNotNull(response.refreshToken());
        assertNotNull(stored.getRevokedAt(), "Token cũ phải bị revoke khi rotate");
    }

    @Test
    void rotateRejectsRevokedToken() {
        String hash = TokenService.sha256Hex("revoked-token");
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .revokedAt(Instant.now())
                .build();
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        ApiException ex = assertThrows(ApiException.class, () -> tokenService.rotate("revoked-token"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void rotateRejectsExpiredToken() {
        String hash = TokenService.sha256Hex("expired-token");
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        ApiException ex = assertThrows(ApiException.class, () -> tokenService.rotate("expired-token"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void rotateUnknownTokenThrows() {
        when(repository.findByTokenHash(TokenService.sha256Hex("unknown"))).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> tokenService.rotate("unknown"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void revokeAllCalledForBan() {
        tokenService.revokeAll(1L);
        verify(repository).revokeAllByUserId(eq(1L), any(Instant.class));
    }

    @Test
    void sha256HexIsDeterministic() {
        String a = TokenService.sha256Hex("same-input");
        String b = TokenService.sha256Hex("same-input");
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    @Test
    void logoutRevokesTokenIfActive() {
        String hash = TokenService.sha256Hex("active-token");
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build();
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        tokenService.revoke("active-token");
        org.junit.jupiter.api.Assertions.assertNotNull(stored.getRevokedAt());
    }

    @Test
    void logoutDoesNothingWhenTokenAlreadyRevoked() {
        String hash = TokenService.sha256Hex("revoked-token");
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .revokedAt(Instant.now())
                .build();
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        tokenService.revoke("revoked-token");
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void issueWithRealUserWorks() {
        when(jwtProvider.generateAccessToken(anyLong(), any(), any())).thenReturn("access.jwt");
        AuthResponse response = tokenService.issue(testUser());
        assertNotNull(response);
    }
}
