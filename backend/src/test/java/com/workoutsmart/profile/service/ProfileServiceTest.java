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
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.service.DraftExerciseService;
import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.UpdateProfileRequest;
import com.workoutsmart.profile.dto.UpdateProfileResponse;
import com.workoutsmart.profile.dto.WorkoutSessionDetailResponse;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseRepository;
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
    private WorkoutSessionExerciseRepository sessionExerciseRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtAuthFilter jwtAuthFilter;
    @Mock
    private DraftExerciseService draftExerciseService;
    @Mock
    private com.workoutsmart.social.service.ActivityFeedService activityFeedService;
    @Mock
    private com.workoutsmart.social.service.LeaderboardSyncService leaderboardSyncService;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userRepository, sessionRepository, setRepository,
                sessionExerciseRepository, exerciseRepository, tokenService, jwtAuthFilter,
                draftExerciseService, activityFeedService, leaderboardSyncService);
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
    void updateProfileChangesFieldsIncludingWeightSexActivity() {
        User u = user();
        u.setWeightKg(new BigDecimal("70.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateProfileResponse res = service.updateProfile(1L,
                new UpdateProfileRequest("Tên Mới", "http://img/x.png", 30, new BigDecimal("175.00"),
                        new BigDecimal("71.50"), "female", "active", "custom", -350, "muscle_gain", null));

        assertTrue(res.goalChanged());
        assertEquals("Tên Mới", u.getDisplayName());
        assertEquals(30, u.getAge());
        assertEquals("muscle_gain", u.getGoalType());
        // 019: TDEE đồng bộ hồ sơ — cân nặng/giới tính/mức vận động/mức điều chỉnh calo sửa được.
        assertEquals(0, new BigDecimal("71.50").compareTo(u.getWeightKg()));
        assertEquals("female", u.getSex());
        assertEquals("active", u.getActivityLevel());
        assertEquals("custom", u.getCalorieGoal());
        assertEquals(-350, u.getCustomCalorieOffset());
        verify(userRepository).save(u);
    }

    @Test
    void updateProfileSameGoalDoesNotFlagChange() {
        User u = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateProfileResponse res = service.updateProfile(1L,
                new UpdateProfileRequest("Tên Mới", null, null, null, null, null, null, null, null, "weight_loss", null));

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

    @Test
    void getExerciseProgressComputesDeltas() {
        Instant early = Instant.now().minus(20, java.time.temporal.ChronoUnit.DAYS);
        Instant late = Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS);
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of(
                        WorkoutSession.builder().id(10L).userId(1L).status("completed").startTime(early).build(),
                        WorkoutSession.builder().id(11L).userId(1L).status("completed").startTime(late).build()));
        when(setRepository.findBySessionIdIn(List.of(10L, 11L))).thenReturn(List.of(
                WorkoutSet.builder().id(1L).sessionId(10L).exerciseId(100L).setNumber(1)
                        .repsCompleted(8).weightUsed(new BigDecimal("50.00")).setType("normal").build(),
                WorkoutSet.builder().id(2L).sessionId(11L).exerciseId(100L).setNumber(1)
                        .repsCompleted(10).weightUsed(new BigDecimal("60.00")).setType("normal").build()));
        when(exerciseRepository.findById(100L)).thenReturn(Optional.of(
                Exercise.builder().id(100L).name("Bench Press").build()));

        var result = service.getExerciseProgress(1L, 30);

        assertEquals(1, result.size());
        assertEquals("Bench Press", result.get(0).exerciseName());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.get(0).weightDelta()));
        assertEquals(2, result.get(0).repsDelta());
    }

    @Test
    void completeSessionMarksCompletedAndCleansDraft() {
        WorkoutSession session = WorkoutSession.builder()
                .id(10L).userId(1L).status("active").startTime(Instant.now()).build();
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(i -> i.getArgument(0));

        service.completeSession(1L, 10L);

        assertEquals("completed", session.getStatus());
        assertNotNull(session.getEndTime());
        verify(draftExerciseService).cleanupBySession(10L);
    }

    @Test
    void clearHistoryDeletesNonActiveInOrder() {
        WorkoutSession completed = WorkoutSession.builder().id(1L).userId(1L).status("completed").build();
        when(sessionRepository.findByUserIdAndStatusNot(1L, "active")).thenReturn(List.of(completed));
        when(sessionRepository.deleteByUserIdAndStatusNot(1L, "active")).thenReturn(1L);

        var res = service.clearHistory(1L);

        verify(setRepository).deleteBySessionIdIn(List.of(1L));
        verify(draftExerciseService).cleanupBySessions(List.of(1L));
        verify(sessionExerciseRepository).deleteBySessionIdIn(List.of(1L));
        verify(sessionRepository).deleteByUserIdAndStatusNot(1L, "active");
        assertTrue(res.message().contains("1"));
    }

    @Test
    void clearHistoryEmptyReturnsMessageWithoutDeleting() {
        when(sessionRepository.findByUserIdAndStatusNot(1L, "active")).thenReturn(List.of());

        var res = service.clearHistory(1L);

        assertEquals("Không có lịch sử để xóa", res.message());
        verify(setRepository, org.mockito.Mockito.never()).deleteBySessionIdIn(org.mockito.ArgumentMatchers.anyList());
    }
}
