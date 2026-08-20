package com.workoutsmart.plan.controller;

import com.workoutsmart.plan.dto.GeneratePlanRequest;
import com.workoutsmart.plan.dto.GoalSetupRequest;
import com.workoutsmart.plan.dto.GoalSetupResponse;
import com.workoutsmart.plan.dto.PlanDayResponse;
import com.workoutsmart.plan.dto.ReplaceDayExercisesRequest;
import com.workoutsmart.plan.dto.WorkoutPlanResponse;
import com.workoutsmart.plan.service.WorkoutPlanService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller mỏng cho thiết lập mục tiêu và lộ trình tập — FR-001, FR-002, FR-005. */
@RestController
@RequestMapping("/api/v1")
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;

    public WorkoutPlanController(WorkoutPlanService workoutPlanService) {
        this.workoutPlanService = workoutPlanService;
    }

    @PutMapping("/users/me/goals")
    public GoalSetupResponse setupGoal(Authentication auth,
                                       @Valid @RequestBody GoalSetupRequest request) {
        return workoutPlanService.setupGoal(currentUserId(auth), request);
    }

    @GetMapping("/workout-plans/active")
    public WorkoutPlanResponse getActivePlan(Authentication auth) {
        return workoutPlanService.getActivePlan(currentUserId(auth));
    }

    @PutMapping("/workout-plans/active/days/{dayId}/exercises")
    public WorkoutPlanResponse replaceDayExercises(Authentication auth,
                                                   @PathVariable Long dayId,
                                                   @Valid @RequestBody ReplaceDayExercisesRequest request) {
        return workoutPlanService.replaceDayExercises(currentUserId(auth), dayId, request);
    }

    @PostMapping("/workout-plans/active/days")
    public PlanDayResponse createDay(Authentication auth, @RequestParam int dayOfWeek) {
        return workoutPlanService.createDay(currentUserId(auth), dayOfWeek);
    }

    @PostMapping("/workout-plans/generate")
    public WorkoutPlanResponse generate(Authentication auth,
                                        @RequestBody(required = false) GeneratePlanRequest request) {
        LocalDate effectiveDate = request != null ? request.effectiveDate() : null;
        return workoutPlanService.toResponse(
                workoutPlanService.generatePlan(currentUserId(auth), effectiveDate));
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
