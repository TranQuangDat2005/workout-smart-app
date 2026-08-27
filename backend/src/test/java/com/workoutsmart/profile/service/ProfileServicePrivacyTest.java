package com.workoutsmart.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import com.workoutsmart.auth.service.TokenService;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.service.DraftExerciseService;
import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.UpdateProfileRequest;
import com.workoutsmart.profile.dto.UpdateProfileResponse;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.social.service.ActivityFeedService;
import com.workoutsmart.social.service.LeaderboardSyncService;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServicePrivacyTest {

    @Mock private UserRepository userRepository;
    @Mock private WorkoutSessionRepository sessionRepository;
    @Mock private WorkoutSetRepository setRepository;
    @Mock private WorkoutSessionExerciseRepository sessionExerciseRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private TokenService tokenService;
    @Mock private JwtAuthFilter jwtAuthFilter;
    @Mock private DraftExerciseService draftExerciseService;
    @Mock private ActivityFeedService activityFeedService;
    @Mock private LeaderboardSyncService leaderboardSyncService;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userRepository, sessionRepository, setRepository,
                sessionExerciseRepository, exerciseRepository, tokenService, jwtAuthFilter,
                draftExerciseService, activityFeedService, leaderboardSyncService);
    }

    private User user(boolean isPrivate) {
        User u = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("x")
                .role("user")
                .displayName("Anh Tập")
                .age(25)
                .heightCm(new BigDecimal("170.00"))
                .goalType("weight_loss")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .isPrivate(isPrivate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .build();
        return u;
    }

    @Test
    void getProfile_includesIsPrivate() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user(true)));

        ProfileResponse res = service.getProfile(1L);

        assertTrue(res.isPrivate());
    }

    @Test
    void getProfile_publicUser_returnsFalse() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user(false)));

        ProfileResponse res = service.getProfile(1L);

        assertFalse(res.isPrivate());
    }

    @Test
    void updateProfile_togglesPrivacy() {
        User u = user(true);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(u));

        UpdateProfileResponse res = service.updateProfile(1L,
                new UpdateProfileRequest(null, null, null, null, null,
                        null, null, null, null, null, false));

        assertFalse(u.isPrivate());
        verify(userRepository).save(u);
    }

    @Test
    void updateProfile_rejectsToggleWithin24h() {
        User u = user(true);
        u.setUpdatedAt(Instant.now().minus(12, ChronoUnit.HOURS)); // changed 12h ago
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(u));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateProfile(1L,
                        new UpdateProfileRequest(null, null, null, null, null,
                                null, null, null, null, null, false)));

        assertEquals(429, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("24h"));
    }

    @Test
    void updateProfile_allowsToggleAfter24h() {
        User u = user(true);
        u.setUpdatedAt(Instant.now().minus(25, ChronoUnit.HOURS)); // changed 25h ago
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(u));

        service.updateProfile(1L,
                new UpdateProfileRequest(null, null, null, null, null,
                        null, null, null, null, null, false));

        assertFalse(u.isPrivate());
    }

    @Test
    void updateProfile_sameValueDoesNotTriggerCooldown() {
        User u = user(true);
        u.setUpdatedAt(Instant.now().minus(1, ChronoUnit.HOURS)); // changed 1h ago
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(u));

        // Sending same value (true == true) — should NOT throw
        service.updateProfile(1L,
                new UpdateProfileRequest(null, null, null, null, null,
                        null, null, null, null, null, true));

        assertTrue(u.isPrivate()); // still true, no change
    }
}
