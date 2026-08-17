package com.workoutsmart.plan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.entity.DraftExercise;
import com.workoutsmart.plan.entity.WorkoutPlanDay;
import com.workoutsmart.plan.entity.WorkoutPlanExercise;
import com.workoutsmart.plan.repository.DraftExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DraftExerciseServiceTest {

    @Mock
    private DraftExerciseRepository draftRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private WorkoutPlanExerciseRepository planExerciseRepository;
    @Mock
    private WorkoutPlanDayRepository dayRepository;
    @Mock
    private WorkoutSessionRepository sessionRepository;

    private DraftExerciseService service;

    @BeforeEach
    void setUp() {
        service = new DraftExerciseService(draftRepository, exerciseRepository,
                planExerciseRepository, dayRepository, sessionRepository);
    }

    @Test
    void recordHiddenExerciseCreatesDraftWithReplacement() {
        Exercise original = Exercise.builder()
                .id(1L).name("Push Up").muscleGroup("chest").bodyPart("chest")
                .status("inactive").build();
        Exercise replacement = Exercise.builder()
                .id(2L).name("Bench Press").muscleGroup("chest").bodyPart("chest")
                .status("active").build();
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(original));
        when(exerciseRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(replacement));
        when(draftRepository.save(any(DraftExercise.class))).thenAnswer(i -> i.getArgument(0));

        DraftExercise draft = service.recordHiddenExercise(1L, 10L);

        assertNotNull(draft);
        assertEquals(1L, draft.getOriginalExerciseId());
        assertEquals(10L, draft.getSessionId());
        assertEquals(2L, draft.getReplacementExerciseId());
    }

    @Test
    void handleExerciseHiddenClonesForAffectedActiveSessions() {
        when(planExerciseRepository.findByExerciseId(1L)).thenReturn(List.of(
                WorkoutPlanExercise.builder().id(1L).dayId(100L).exerciseId(1L).build()));
        when(dayRepository.findAllById(any())).thenReturn(List.of(
                WorkoutPlanDay.builder().id(100L).planId(50L).dayOfWeek(1).build()));
        when(sessionRepository.findByStatus("active")).thenReturn(List.of(
                WorkoutSession.builder().id(10L).userId(7L).planId(50L).status("active").build()));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(Exercise.builder()
                .id(1L).name("Push Up").muscleGroup("chest").bodyPart("chest").status("inactive").build()));
        when(exerciseRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        when(draftRepository.save(any(DraftExercise.class))).thenAnswer(i -> i.getArgument(0));

        int affected = service.handleExerciseHidden(1L);

        assertEquals(1, affected);
        verify(draftRepository).save(any(DraftExercise.class));
    }

    @Test
    void cleanupBySessionDeletesDrafts() {
        service.cleanupBySession(10L);

        verify(draftRepository).deleteBySessionId(10L);
    }
}
