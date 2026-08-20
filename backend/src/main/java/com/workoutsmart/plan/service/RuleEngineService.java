package com.workoutsmart.plan.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rule Engine v1 — sinh lộ trình theo goal_type × fitness_level × equipment (FR-008/009/010). */
@Service
public class RuleEngineService {

    private static final List<List<String>> MUSCLE_GAIN_SPLIT = List.of(
            List.of("chest", "shoulders", "arms"), // Push
            List.of("back", "arms"),               // Pull
            List.of("legs")                        // Legs
    );

    private final ExerciseService exerciseService;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository dayRepository;
    private final WorkoutPlanExerciseRepository planExerciseRepository;

    public RuleEngineService(ExerciseService exerciseService,
                             WorkoutPlanRepository planRepository,
                             WorkoutPlanDayRepository dayRepository,
                             WorkoutPlanExerciseRepository planExerciseRepository) {
        this.exerciseService = exerciseService;
        this.planRepository = planRepository;
        this.dayRepository = dayRepository;
        this.planExerciseRepository = planExerciseRepository;
    }

    @Transactional
    public WorkoutPlan generatePlan(String goalType, String fitnessLevel, List<String> equipment,
                                    Long userId, String planName) {
        RuleConfig config = ruleConfig(goalType, fitnessLevel);
        List<Exercise> eligible = exerciseService.findActiveByEquipmentForUser(equipment, userId);
        if (eligible.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không có bài tập nào khớp với dụng cụ đã chọn");
        }

        WorkoutPlan plan = planRepository.save(WorkoutPlan.builder()
                .userId(userId)
                .name(planName)
                .goalType(goalType)
                .fitnessLevel(fitnessLevel)
                .status("active")
                .build());

        List<Integer> weekdays = weekdaysFor(config.daysPerWeek());
        for (int i = 0; i < config.daysPerWeek(); i++) {
            WorkoutPlanDay day = dayRepository.save(WorkoutPlanDay.builder()
                    .planId(plan.getId())
                    .dayOfWeek(weekdays.get(i))
                    .build());

            List<Exercise> selected = selectExercises(eligible, goalType, i, config.exercisesPerDay());
            for (int exIndex = 0; exIndex < selected.size(); exIndex++) {
                Exercise exercise = selected.get(exIndex);
                boolean duration = "duration".equals(exercise.getMeasureType());
                planExerciseRepository.save(WorkoutPlanExercise.builder()
                        .dayId(day.getId())
                        .exerciseId(exercise.getId())
                        .targetSets(duration && "cardio".equalsIgnoreCase(exercise.getCategory()) ? 1 : config.sets())
                        .targetReps(duration ? 0 : config.reps())
                        .targetDurationSeconds(duration ? durationSecondsFor(exercise, goalType) : null)
                        .restTimeSeconds(config.restSeconds())
                        .sortOrder(exIndex)
                        .build());
            }
        }
        return plan;
    }

    private RuleConfig ruleConfig(String goalType, String fitnessLevel) {
        int days;
        int reps;
        int restSeconds;
        switch (goalType) {
            case "weight_loss" -> {
                days = 5;
                reps = 12;
                restSeconds = 45;
            }
            case "muscle_gain" -> {
                days = 4;
                reps = 8;
                restSeconds = 60;
            }
            case "endurance" -> {
                days = 4;
                reps = 15;
                restSeconds = 30;
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "goal_type không hợp lệ");
        }

        int exercisesPerDay;
        int sets;
        switch (fitnessLevel) {
            case "beginner" -> {
                exercisesPerDay = 3;
                sets = 3;
            }
            case "intermediate" -> {
                exercisesPerDay = 4;
                sets = 4;
            }
            case "advanced" -> {
                exercisesPerDay = 5;
                sets = 5;
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "fitness_level không hợp lệ");
        }
        return new RuleConfig(days, exercisesPerDay, sets, reps, restSeconds);
    }

    private Integer durationSecondsFor(Exercise exercise, String goalType) {
        if ("cardio".equalsIgnoreCase(exercise.getCategory())) {
            return "endurance".equals(goalType) ? 900 : 1200;
        }
        return 60;
    }

    private List<Integer> weekdaysFor(int count) {
        return switch (count) {
            case 3 -> List.of(1, 3, 5);
            case 5 -> List.of(1, 2, 3, 4, 5);
            default -> List.of(1, 2, 4, 5);
        };
    }

    private List<Exercise> selectExercises(List<Exercise> eligible, String goalType, int dayIndex, int count) {
        if ("muscle_gain".equals(goalType)) {
            List<String> targetGroups = MUSCLE_GAIN_SPLIT.get(dayIndex % MUSCLE_GAIN_SPLIT.size());
            List<Exercise> filtered = eligible.stream()
                    .filter(e -> e.getMuscleGroup() != null && targetGroups.contains(e.getMuscleGroup()))
                    .toList();
            return pickDistinct(filtered.isEmpty() ? eligible : filtered, count);
        }
        if ("weight_loss".equals(goalType)) {
            List<Exercise> cardio = eligible.stream()
                    .filter(e -> "cardio".equalsIgnoreCase(e.getCategory()))
                    .toList();
            List<Exercise> others = eligible.stream()
                    .filter(e -> !"cardio".equalsIgnoreCase(e.getCategory()))
                    .toList();
            List<Exercise> result = new ArrayList<>(pickDistinct(cardio, Math.min(cardio.size(), 2)));
            result.addAll(pickDistinct(others, count - result.size()));
            return result;
        }
        // endurance = circuit toàn thân: không lọc theo nhóm cơ
        return pickDistinct(eligible, count);
    }

    private List<Exercise> pickDistinct(List<Exercise> source, int count) {
        return source.stream().distinct().limit(Math.max(0, count)).toList();
    }

    private record RuleConfig(int daysPerWeek, int exercisesPerDay, int sets, int reps, int restSeconds) {
    }
}
