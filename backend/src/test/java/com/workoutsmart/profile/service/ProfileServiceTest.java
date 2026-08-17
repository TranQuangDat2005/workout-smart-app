package com.workoutsmart.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.UpdateProfileRequest;
import com.workoutsmart.profile.dto.UpdateProfileResponse;
import com.workoutsmart.profile.dto.WorkoutSessionDetailResponse;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkoutSessionRepository sessionRepository;
    @Mock
    private WorkoutSetRepository setRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtAuthFilter jwtAuthFilter;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userRepository, sessionRepository, setRepository,
                tokenService, jwtAuthFilter);
    }

    private User user() {
        return User.builder()
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
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getProfileReturnsUserData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        ProfileResponse res = service.getProfile(1L);

        assertEquals("user@example.com", res.email());
        assertEquals("Anh Tập", res.displayName());
        assertEquals("weight_loss", res.goalType());
    }

    @Test
    void getProfileUnknownUserThrows404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getProfile(99L));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void updateProfileChangesFieldsButNotWeight() {
        User u = user();
        u.setWeightKg(new BigDecimal("70.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateProfileResponse res = service.updateProfile(1L,
                new UpdateProfileRequest("Tên Mới", "http://img/x.png", 30, new BigDecimal("175.00"), "muscle_gain"));

        assertTrue(res.goalChanged());
        assertEquals("Tên Mới", u.getDisplayName());
        assertEquals(30, u.getAge());
        assertEquals("muscle_gain", u.getGoalType());
        // Cân nặng KHÔNG đổi (không nằm trong request)
        assertEquals(new BigDecimal("70.00"), u.getWeightKg());
        verify(userRepository).save(u);
    }

    @Test
    void updateProfileSameGoalDoesNotFlagChange() {
        User u = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateProfileResponse res = service.updateProfile(1L,
                new UpdateProfileRequest("Tên Mới", null, null, null, "weight_loss"));

        org.junit.jupiter.api.Assertions.assertFalse(res.goalChanged());
    }

    @Test
    void deleteAccountSoftDeletesAndRevokesTokens() {
        User u = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        service.deleteAccount(1L);

        assertEquals(AccountStatus.DELETED, u.getAccountStatus());
        assertNotNull(u.getDeletedAt());
        verify(tokenService).revokeAll(1L);
        verify(jwtAuthFilter).invalidate(1L);
    }

    @Test
    void getSessionsComputesVolume() {
        WorkoutSession session = WorkoutSession.builder()
                .id(10L).userId(1L).status("completed").startTime(Instant.now()).build();
        when(sessionRepository.findByUserIdOrderByStartTimeDesc(any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(session)));
        when(setRepository.findBySessionIdOrderBySetNumberAsc(10L)).thenReturn(List.of(
                WorkoutSet.builder().id(1L).sessionId(10L).setNumber(1)
                        .repsCompleted(10).weightUsed(new BigDecimal("50.00")).build(),
                WorkoutSet.builder().id(2L).sessionId(10L).setNumber(2)
                        .repsCompleted(8).weightUsed(new BigDecimal("60.00")).build()));

        var page = service.getSessions(1L, 0, 20);

        assertEquals(1, page.getTotalElements());
        assertEquals(2, page.getContent().get(0).totalSets());
        assertEquals(0, new BigDecimal("980.00").compareTo(page.getContent().get(0).totalVolumeKg()));
    }

    @Test
    void getSessionDetailRejectsOtherUsersSession() {
        WorkoutSession session = WorkoutSession.builder()
                .id(10L).userId(2L).status("active").startTime(Instant.now()).build();
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        ApiException ex = assertThrows(ApiException.class, () -> service.getSessionDetail(1L, 10L));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void getSessionDetailReturnsSets() {
        WorkoutSession session = WorkoutSession.builder()
                .id(10L).userId(1L).status("completed").startTime(Instant.now()).build();
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(setRepository.findBySessionIdOrderBySetNumberAsc(10L)).thenReturn(List.of(
                WorkoutSet.builder().id(1L).sessionId(10L).setNumber(1)
                        .repsCompleted(12).weightUsed(new BigDecimal("40.00")).restTimeSeconds(90).build()));

        WorkoutSessionDetailResponse res = service.getSessionDetail(1L, 10L);

        assertEquals(1, res.sets().size());
        assertEquals(12, res.sets().get(0).repsCompleted());
    }

    @Test
    void getSessionDetailUnknownSessionThrows404() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getSessionDetail(1L, 10L));
        assertEquals(404, ex.getStatus().value());
    }
}
