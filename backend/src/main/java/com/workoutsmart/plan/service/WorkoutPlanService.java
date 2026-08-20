package com.workoutsmart.plan.service;

import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.common.SetType;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.dto.GoalSetupRequest;
import com.workoutsmart.plan.dto.GoalSetupResponse;
import com.workoutsmart.plan.dto.PlanDayResponse;
import com.workoutsmart.plan.dto.PlanExerciseItemRequest;
import com.workoutsmart.plan.dto.PlanExerciseResponse;
import com.workoutsmart.plan.dto.SetTargetRequest;
import com.workoutsmart.plan.dto.SetTargetResponse;
import com.workoutsmart.plan.dto.ReplaceDayExercisesRequest;
import com.workoutsmart.plan.dto.WorkoutPlanResponse;
import com.workoutsmart.plan.entity.WorkoutPlan;
import com.workoutsmart.plan.entity.WorkoutPlanDay;
import com.workoutsmart.plan.entity.WorkoutPlanExercise;
import com.workoutsmart.plan.entity.WorkoutPlanExerciseSet;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseSetRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ lộ trình tập — FR-001, FR-002, FR-005. */
@Service
public class WorkoutPlanService {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVED = "archived";
    public static final String STATUS_SCHEDULED = "scheduled";

    private final UserRepository userRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository dayRepository;
    private final WorkoutPlanExerciseRepository planExerciseRepository;
    private final WorkoutPlanExerciseSetRepository planExerciseSetRepository;
    private final ExerciseRepository exerciseRepository;
    private final RuleEngineService ruleEngineService;

    public WorkoutPlanService(UserRepository userRepository,
                              WorkoutPlanRepository planRepository,
                              WorkoutPlanDayRepository dayRepository,
                              WorkoutPlanExerciseRepository planExerciseRepository,
                              WorkoutPlanExerciseSetRepository planExerciseSetRepository,
                              ExerciseRepository exerciseRepository,
                              RuleEngineService ruleEngineService) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.dayRepository = dayRepository;
        this.planExerciseRepository = planExerciseRepository;
        this.planExerciseSetRepository = planExerciseSetRepository;
        this.exerciseRepository = exerciseRepository;
        this.ruleEngineService = ruleEngineService;
    }

    @Transactional
    public GoalSetupResponse setupGoal(Long userId, GoalSetupRequest request) {
        User user = requireUser(userId);
        boolean hadActivePlan = planRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE).isPresent();
        String warning = hadActivePlan
                ? "Lộ trình cũ sẽ được lưu trữ. Tiến trình tập hôm nay có thể bị ảnh hưởng."
                : null;

        user.setGoalType(request.goalType());
        user.setFitnessLevel(request.fitnessLevel());
        user.setEquipment(String.join(",", request.equipment()));
        userRepository.save(user);

        WorkoutPlan plan = generatePlan(userId, request.effectiveDate());
        return new GoalSetupResponse(plan.getGoalType(), plan.getFitnessLevel(),
                parseEquipment(user.getEquipment()), plan.getId(), warning);
    }

    @Transactional
    public WorkoutPlan generatePlan(Long userId, LocalDate effectiveDate) {
        User user = requireUser(userId);
        if (isBlank(user.getGoalType()) || isBlank(user.getFitnessLevel()) || isBlank(user.getEquipment())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Vui lòng thiết lập mục tiêu, trình độ và dụng cụ trước khi tạo lộ trình");
        }
        List<String> equipment = parseEquipment(user.getEquipment());
        LocalDate today = LocalDate.now();
        LocalDate startDate = effectiveDate != null ? effectiveDate : today;

        if (startDate.isAfter(today)) {
            WorkoutPlan plan = ruleEngineService.generatePlan(user.getGoalType(), user.getFitnessLevel(),
                    equipment, user.getId(), planName(user.getGoalType()));
            plan.setStatus(STATUS_SCHEDULED);
            plan.setEffectiveDate(startDate);
            return planRepository.save(plan);
        }

        archiveActivePlan(userId);
        WorkoutPlan plan = ruleEngineService.generatePlan(user.getGoalType(), user.getFitnessLevel(),
                equipment, user.getId(), planName(user.getGoalType()));
        plan.setStatus(STATUS_ACTIVE);
        plan.setEffectiveDate(today);
        return planRepository.save(plan);
    }

    @Transactional
    public WorkoutPlanResponse getActivePlan(Long userId) {
        promoteDueScheduledPlans(userId);
        WorkoutPlan plan = planRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chưa có lộ trình tập"));
        return toResponse(plan);
    }

    @Transactional
    public void archivePlan(Long planId) {
        WorkoutPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lộ trình không tồn tại"));
        plan.setStatus(STATUS_ARCHIVED);
        planRepository.save(plan);
    }

    public WorkoutPlanResponse toResponse(WorkoutPlan plan) {
        List<PlanDayResponse> days = dayRepository.findByPlanIdOrderByDayOfWeekAsc(plan.getId()).stream()
                .map(this::toDayResponse)
                .toList();
        return new WorkoutPlanResponse(plan.getId(), plan.getName(), plan.getGoalType(),
                plan.getFitnessLevel(), plan.getStatus(), days);
    }

    private PlanDayResponse toDayResponse(WorkoutPlanDay day) {
        List<PlanExerciseResponse> exercises = planExerciseRepository
                .findByDayIdOrderBySortOrderAscIdAsc(day.getId()).stream()
                .map(pe -> {
                    Exercise exercise = exerciseRepository.findById(pe.getExerciseId()).orElse(null);
                    List<SetTargetResponse> sets = planExerciseSetRepository
                            .findByPlanExerciseIdOrderBySetNumberAsc(pe.getId()).stream()
                            .map(s -> new SetTargetResponse(s.getSetNumber(), s.getTargetReps(),
                                    s.getTargetWeight(), s.getSetType(), s.getTargetDurationSeconds()))
                                    .toList();
                            return new PlanExerciseResponse(pe.getId(), pe.getExerciseId(),
                                    exercise != null ? exercise.getName() : null,
                                    pe.getTargetSets(), pe.getTargetReps(), pe.getRestTimeSeconds(),
                                    exercise != null ? exercise.getImage() : null,
                                    exercise != null ? exercise.getGifUrl() : null,
                                    sets,
                                    pe.getTargetDurationSeconds(),
                                    pe.getMeasureType() != null && !pe.getMeasureType().isBlank()
                                            ? pe.getMeasureType()
                                            : exercise != null ? exercise.getMeasureType() : "reps_weight");
                }).toList();
        return new PlanDayResponse(day.getId(), day.getDayOfWeek(), exercises);
    }

    @Transactional
    public WorkoutPlanResponse replaceDayExercises(Long userId, Long dayId, ReplaceDayExercisesRequest request) {
        WorkoutPlanDay day = dayRepository.findById(dayId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ngày tập không tồn tại"));
        WorkoutPlan plan = planRepository.findById(day.getPlanId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lộ trình không tồn tại"));
        if (!plan.getUserId().equals(userId) || !STATUS_ACTIVE.equals(plan.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Ngày tập không tồn tại");
        }
        planExerciseRepository.deleteByDayId(dayId);
        int order = 0;
        for (PlanExerciseItemRequest item : request.exercises()) {
            Exercise exercise = exerciseRepository.findById(item.exerciseId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
            if ("user_custom".equals(exercise.getSource())
                    && (exercise.getCreatedBy() == null || !exercise.getCreatedBy().equals(userId))) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại");
            }
            if (!"active".equals(exercise.getStatus()) || exercise.getDeletedAt() != null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Bài tập không còn dùng được");
            }
            WorkoutPlanExercise saved = planExerciseRepository.save(WorkoutPlanExercise.builder()
                    .dayId(dayId)
                    .exerciseId(exercise.getId())
                    .targetSets(item.targetSets())
                    .targetReps(item.targetReps())
                    .targetDurationSeconds(item.targetDurationSeconds())
                    .measureType(item.measureType())
                    .restTimeSeconds(item.restTimeSeconds())
                    .sortOrder(order++)
                    .build());
            saveSetTargets(saved.getId(), item.sets());
        }
        return toResponse(plan);
    }

    private void saveSetTargets(Long planExerciseId, List<SetTargetRequest> sets) {
        if (sets == null || sets.isEmpty()) {
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (SetTargetRequest s : sets) {
            if (!seen.add(s.setNumber())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Số hiệp target trùng lặp");
            }
            planExerciseSetRepository.save(WorkoutPlanExerciseSet.builder()
                    .planExerciseId(planExerciseId)
                    .setNumber(s.setNumber())
                    .targetReps(s.targetReps() != null ? s.targetReps() : 0)
                    .targetWeight(s.targetWeight())
                    .setType(SetType.normalize(s.setType()))
                    .targetDurationSeconds(s.targetDurationSeconds())
                    .build());
        }
    }

    @Transactional
    public PlanDayResponse createDay(Long userId, int dayOfWeek) {
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thứ trong tuần không hợp lệ");
        }
        WorkoutPlan plan = planRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chưa có lộ trình tập"));
        WorkoutPlanDay day = dayRepository.findByPlanIdAndDayOfWeek(plan.getId(), dayOfWeek)
                .orElseGet(() -> dayRepository.save(WorkoutPlanDay.builder()
                        .planId(plan.getId())
                        .dayOfWeek(dayOfWeek)
                        .build()));
        return toDayResponse(day);
    }

    private void promoteDueScheduledPlans(Long userId) {
        LocalDate today = LocalDate.now();
        planRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(p -> STATUS_SCHEDULED.equals(p.getStatus()))
                .filter(p -> p.getEffectiveDate() != null && !p.getEffectiveDate().isAfter(today))
                .forEach(p -> {
                    archiveActivePlan(userId);
                    p.setStatus(STATUS_ACTIVE);
                    p.setEffectiveDate(null);
                    planRepository.save(p);
                });
    }

    private void archiveActivePlan(Long userId) {
        planRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE).ifPresent(plan -> {
            plan.setStatus(STATUS_ARCHIVED);
            planRepository.save(plan);
        });
    }

    private String planName(String goalType) {
        String label = switch (goalType) {
            case "weight_loss" -> "Giảm cân";
            case "muscle_gain" -> "Tăng cơ";
            case "endurance" -> "Sức bền";
            default -> "Tập luyện";
        };
        return "Kế hoạch " + label;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
    }

    private List<String> parseEquipment(String equipment) {
        if (isBlank(equipment)) {
            return List.of();
        }
        return Arrays.stream(equipment.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
