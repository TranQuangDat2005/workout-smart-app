package com.workoutsmart.plan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.service.ExerciseService;
import com.workoutsmart.plan.entity.WorkoutPlan;
import com.workoutsmart.plan.entity.WorkoutPlanDay;
import com.workoutsmart.plan.entity.WorkoutPlanExercise;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private ExerciseService exerciseService;
    @Mock
    private WorkoutPlanRepository planRepository;
    @Mock
    private WorkoutPlanDayRepository dayRepository;
    @Mock
    private WorkoutPlanExerciseRepository planExerciseRepository;

    private RuleEngineService service;

    @BeforeEach
    void setUp() {
        service = new RuleEngineService(exerciseService, planRepository, dayRepository, planExerciseRepository);
    }

    private List<Exercise> mixedExercises() {
        List<Exercise> list = new ArrayList<>();
        list.add(exercise(1L, "Jumping Jacks", "cardio", "core"));
        list.add(exercise(2L, "Mountain Climber", "cardio", "core"));
        list.add(exercise(3L, "Push Up", "strength", "chest"));
        list.add(exercise(4L, "Squat", "strength", "legs"));
        list.add(exercise(5L, "Plank", "strength", "core"));
        list.add(exercise(6L, "Burpee", "cardio", "legs"));
        list.add(exercise(7L, "Lunge", "strength", "legs"));
        return list;
    }

    private List<Exercise> splitExercises() {
        List<Exercise> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(exercise(100L + i, "Chest " + i, "strength", "chest"));
            list.add(exercise(200L + i, "Back " + i, "strength", "back"));
            list.add(exercise(300L + i, "Legs " + i, "strength", "legs"));
        }
        return list;
    }

    private Exercise exercise(Long id, String name, String category, String muscleGroup) {
        return Exercise.builder()
                .id(id).name(name).category(category).equipment("body_weight")
                .muscleGroup(muscleGroup).status("active").build();
    }

    private void stubSave() {
        when(planRepository.save(any(WorkoutPlan.class))).thenAnswer(i -> i.getArgument(0));
        when(dayRepository.save(any(WorkoutPlanDay.class))).thenAnswer(i -> i.getArgument(0));
        when(planExerciseRepository.save(any(WorkoutPlanExercise.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void weightLossBeginnerCreatesFiveDaysWithCorrectVolume() {
        when(exerciseService.findActiveByEquipment(anyList())).thenReturn(mixedExercises());
        stubSave();

        WorkoutPlan plan = service.generatePlan("weight_loss", "beginner",
                List.of("body_weight"), 1L, "Kế hoạch Giảm cân");

        assertNotNull(plan);
        verify(dayRepository, times(5)).save(any(WorkoutPlanDay.class));
        ArgumentCaptor<WorkoutPlanExercise> captor = ArgumentCaptor.forClass(WorkoutPlanExercise.class);
        verify(planExerciseRepository, times(15)).save(captor.capture());
        assertEquals(3, captor.getValue().getTargetSets());
        assertEquals(12, captor.getValue().getTargetReps());
        assertEquals(45, captor.getValue().getRestTimeSeconds());
    }

    @Test
    void muscleGainIntermediateCreatesSplitWithCorrectVolume() {
        when(exerciseService.findActiveByEquipment(anyList())).thenReturn(splitExercises());
        stubSave();

        service.generatePlan("muscle_gain", "intermediate", List.of("body_weight"), 1L, "Kế hoạch Tăng cơ");

        verify(dayRepository, times(4)).save(any(WorkoutPlanDay.class));
        ArgumentCaptor<WorkoutPlanExercise> captor = ArgumentCaptor.forClass(WorkoutPlanExercise.class);
        verify(planExerciseRepository, times(16)).save(captor.capture());
        assertEquals(4, captor.getValue().getTargetSets());
        assertEquals(8, captor.getValue().getTargetReps());
        assertEquals(60, captor.getValue().getRestTimeSeconds());
    }

    @Test
    void emptyEligibleExercisesThrows422() {
        when(exerciseService.findActiveByEquipment(anyList())).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.generatePlan("endurance", "beginner", List.of("machine"), 1L, "x"));

        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void invalidGoalTypeThrows400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.generatePlan("powerlifting", "beginner", List.of("body_weight"), 1L, "x"));

        assertEquals(400, ex.getStatus().value());
    }
}
