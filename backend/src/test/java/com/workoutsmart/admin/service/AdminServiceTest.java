package com.workoutsmart.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.admin.dto.AdminUserResponse;
import com.workoutsmart.admin.dto.CreateExerciseRequest;
import com.workoutsmart.admin.dto.ExerciseImportResponse;
import com.workoutsmart.admin.dto.UpdateExerciseRequest;
import com.workoutsmart.admin.entity.AuditLog;
import com.workoutsmart.admin.repository.AuditLogRepository;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import com.workoutsmart.auth.service.TokenService;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.service.DraftExerciseService;
import com.workoutsmart.profile.service.ProfileService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private DraftExerciseService draftExerciseService;
    @Mock
    private ProfileService profileService;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtAuthFilter jwtAuthFilter;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(userRepository, exerciseRepository, auditLogRepository,
                draftExerciseService, profileService, tokenService, jwtAuthFilter);
    }

    private User user() {
        return User.builder().id(1L).email("u@example.com").role("user")
                .accountStatus(AccountStatus.ACTIVE).emailVerified(true).build();
    }

    @Test
    void banUserRevokesTokensAndAudits() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.banUser(1L, "spam", 9L);

        assertEquals(AccountStatus.BANNED, userRepository.findById(1L).orElseThrow().getAccountStatus());
        verify(tokenService).revokeAll(1L);
        verify(jwtAuthFilter).invalidate(1L);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void unbanUserRestoresActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.unbanUser(1L, 9L);

        verify(jwtAuthFilter).invalidate(1L);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void createExerciseSavesActiveAndAudits() {
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.createExercise(new CreateExerciseRequest("Push Up", "strength", "chest",
                "body_weight", "chest", "chest", null, null, null), 9L);

        ArgumentCaptor<Exercise> captor = ArgumentCaptor.forClass(Exercise.class);
        verify(exerciseRepository).save(captor.capture());
        assertEquals("active", captor.getValue().getStatus());
    }

    @Test
    void importExercisesUpsertsAndReports() {
        when(exerciseRepository.findByNameIgnoreCaseAndEquipmentIgnoreCase("Push Up", "body_weight"))
                .thenReturn(Optional.of(Exercise.builder().id(1L).name("Push Up").equipment("body_weight").status("active").build()));
        when(exerciseRepository.findByNameIgnoreCaseAndEquipmentIgnoreCase("Squat", "body_weight"))
                .thenReturn(Optional.empty());
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ExerciseImportResponse res = service.importExercises(List.of(
                new CreateExerciseRequest("Push Up", "strength", "chest", "body_weight", "chest", "chest", null, null, null),
                new CreateExerciseRequest("Squat", "strength", "legs", "body_weight", "legs", "legs", null, null, null)), 9L);

        assertEquals(1, res.updated());
        assertEquals(1, res.inserted());
    }

    @Test
    void unknownUserThrows404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.banUser(99L, "x", 9L));

        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void hideExerciseTriggersDraftQueue() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(
                Exercise.builder().id(1L).name("Push Up").status("active").build()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.setExerciseStatus(1L, "inactive", "lý do", 9L);

        verify(draftExerciseService).handleExerciseHidden(1L);
    }

    @Test
    void searchUsersReturnsMappedResults() {
        when(userRepository.findByEmailContainingIgnoreCase("u")).thenReturn(List.of(user()));
        when(userRepository.findByDisplayNameContainingIgnoreCase("u")).thenReturn(List.of());

        List<AdminUserResponse> res = service.searchUsers("u");

        assertEquals(1, res.size());
        assertEquals("u@example.com", res.get(0).email());
    }
}
