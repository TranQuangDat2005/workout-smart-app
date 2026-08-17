package com.workoutsmart.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.admin.entity.AuditLog;
import com.workoutsmart.admin.repository.AuditLogRepository;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.service.DraftExerciseService;
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
    private ExerciseRepository exerciseRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private DraftExerciseService draftExerciseService;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(exerciseRepository, auditLogRepository, draftExerciseService);
    }

    private Exercise exercise() {
        return Exercise.builder().id(1L).name("Push Up").status("active").build();
    }

    @Test
    void hideExerciseTriggersDraftQueueAndAudit() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.setExerciseStatus(1L, "inactive", "lý do", 9L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("deactivate_exercise", captor.getValue().getActionType());
        assertEquals(9L, captor.getValue().getAdminId());
        verify(draftExerciseService).handleExerciseHidden(1L);
    }

    @Test
    void activateExerciseDoesNotTriggerDraftQueue() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise()));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.setExerciseStatus(1L, "active", null, 9L);

        verify(draftExerciseService, never()).handleExerciseHidden(any());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void unknownExerciseThrows404() {
        when(exerciseRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.setExerciseStatus(99L, "inactive", null, 9L));

        assertEquals(404, ex.getStatus().value());
    }
}
