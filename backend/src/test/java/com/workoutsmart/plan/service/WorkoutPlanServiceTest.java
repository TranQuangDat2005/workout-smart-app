package com.workoutsmart.plan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.dto.GoalSetupRequest;
import com.workoutsmart.plan.dto.GoalSetupResponse;
import com.workoutsmart.plan.entity.WorkoutPlan;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutPlanServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkoutPlanRepository planRepository;
    @Mock
    private WorkoutPlanDayRepository dayRepository;
    @Mock
    private WorkoutPlanExerciseRepository planExerciseRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private RuleEngineService ruleEngineService;

    private WorkoutPlanService service;

    @BeforeEach
    void setUp() {
        service = new WorkoutPlanService(userRepository, planRepository, dayRepository,
                planExerciseRepository, exerciseRepository, ruleEngineService);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .email("u@example.com")
                .passwordHash("x")
                .role("user")
                .goalType("weight_loss")
                .fitnessLevel("beginner")
                .equipment("body_weight")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    private WorkoutPlan plan(Long id, String status) {
        return WorkoutPlan.builder()
                .id(id).userId(1L).name("Kế hoạch Giảm cân")
                .goalType("weight_loss").fitnessLevel("beginner")
                .status(status).build();
    }

    @Test
    void setupGoalSavesUserAndReturnsPlan() {
        User u = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(planRepository.findByUserIdAndStatus(1L, WorkoutPlanService.STATUS_ACTIVE))
                .thenReturn(Optional.empty());
        when(ruleEngineService.generatePlan(any(), any(), any(), any(), any()))
                .thenReturn(plan(10L, WorkoutPlanService.STATUS_ACTIVE));
        when(planRepository.save(any(WorkoutPlan.class))).thenAnswer(i -> i.getArgument(0));

        GoalSetupResponse res = service.setupGoal(1L, new GoalSetupRequest(
                "muscle_gain", "intermediate", List.of("dumbbell"), LocalDate.now()));

        assertEquals("muscle_gain", u.getGoalType());
        assertEquals("intermediate", u.getFitnessLevel());
        assertEquals("dumbbell", u.getEquipment());
        assertEquals(10L, res.planId());
        assertNull(res.warning());
        verify(userRepository).save(u);
    }

    @Test
    void generatePlanRequiresGoalLevelAndEquipment() {
        User u = user();
        u.setEquipment(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.generatePlan(1L, LocalDate.now()));

        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void generatePlanImmediateArchivesActivePlan() {
        User u = user();
        WorkoutPlan oldPlan = plan(9L, WorkoutPlanService.STATUS_ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(planRepository.findByUserIdAndStatus(1L, WorkoutPlanService.STATUS_ACTIVE))
                .thenReturn(Optional.of(oldPlan));
        when(ruleEngineService.generatePlan(any(), any(), any(), any(), any()))
                .thenReturn(plan(10L, WorkoutPlanService.STATUS_ACTIVE));
        when(planRepository.save(any(WorkoutPlan.class))).thenAnswer(i -> i.getArgument(0));

        WorkoutPlan result = service.generatePlan(1L, LocalDate.now());

        assertEquals(WorkoutPlanService.STATUS_ARCHIVED, oldPlan.getStatus());
        assertEquals(WorkoutPlanService.STATUS_ACTIVE, result.getStatus());
    }

    @Test
    void generatePlanFutureCreatesScheduledPlan() {
        User u = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(ruleEngineService.generatePlan(any(), any(), any(), any(), any()))
                .thenReturn(plan(10L, WorkoutPlanService.STATUS_ACTIVE));
        when(planRepository.save(any(WorkoutPlan.class))).thenAnswer(i -> i.getArgument(0));

        WorkoutPlan result = service.generatePlan(1L, LocalDate.now().plusDays(1));

        assertEquals(WorkoutPlanService.STATUS_SCHEDULED, result.getStatus());
        assertEquals(LocalDate.now().plusDays(1), result.getEffectiveDate());
    }

    @Test
    void getActivePlanReturns404WhenMissing() {
        when(planRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(planRepository.findByUserIdAndStatus(1L, WorkoutPlanService.STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getActivePlan(1L));

        assertEquals(404, ex.getStatus().value());
    }
}
