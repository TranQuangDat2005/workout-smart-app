package com.workoutsmart.plan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.entity.DraftExercise;
import com.workoutsmart.plan.repository.DraftExerciseRepository;
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

    private DraftExerciseService service;

    @BeforeEach
    void setUp() {
        service = new DraftExerciseService(draftRepository, exerciseRepository);
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
    void cleanupBySessionDeletesDrafts() {
        service.cleanupBySession(10L);

        verify(draftRepository).deleteBySessionId(10L);
    }
}
