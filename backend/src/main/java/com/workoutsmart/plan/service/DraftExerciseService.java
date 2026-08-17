package com.workoutsmart.plan.service;

import com.workoutsmart.auth.exception.ApiException;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Draft queue khi bài tập trong plan active bị Admin ẩn — FR-011. */
@Service
public class DraftExerciseService {

    private final DraftExerciseRepository draftRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutPlanExerciseRepository planExerciseRepository;
    private final WorkoutPlanDayRepository dayRepository;
    private final WorkoutSessionRepository sessionRepository;

    public DraftExerciseService(DraftExerciseRepository draftRepository,
                                ExerciseRepository exerciseRepository,
                                WorkoutPlanExerciseRepository planExerciseRepository,
                                WorkoutPlanDayRepository dayRepository,
                                WorkoutSessionRepository sessionRepository) {
        this.draftRepository = draftRepository;
        this.exerciseRepository = exerciseRepository;
        this.planExerciseRepository = planExerciseRepository;
        this.dayRepository = dayRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Ghi nhận bài tập bị ẩn cho buổi tập hiện tại + gợi ý bài thay thế.
     * clonedExerciseId trỏ về chính originalExerciseId vì exercise chỉ bị soft-hide
     * (vẫn truy cập được theo id), không cần nhân bản bản ghi.
     */
    @Transactional
    public DraftExercise recordHiddenExercise(Long exerciseId, Long sessionId) {
        Exercise original = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
        Long replacementId = findReplacement(original).map(Exercise::getId).orElse(null);
        return draftRepository.save(DraftExercise.builder()
                .sessionId(sessionId)
                .originalExerciseId(exerciseId)
                .clonedExerciseId(exerciseId)
                .replacementExerciseId(replacementId)
                .build());
    }

    /**
     * Khi Admin ẩn exercise: tìm tất cả workout session active có plan chứa exercise này
     * và clone vào draft queue cho từng session. Trả về số session bị ảnh hưởng.
     */
    @Transactional
    public int handleExerciseHidden(Long exerciseId) {
        List<WorkoutPlanExercise> planExercises = planExerciseRepository.findByExerciseId(exerciseId);
        if (planExercises.isEmpty()) {
            return 0;
        }
        Set<Long> dayIds = planExercises.stream()
                .map(WorkoutPlanExercise::getDayId)
                .collect(Collectors.toSet());
        Set<Long> planIds = dayRepository.findAllById(dayIds).stream()
                .map(WorkoutPlanDay::getPlanId)
                .collect(Collectors.toSet());
        if (planIds.isEmpty()) {
            return 0;
        }

        List<WorkoutSession> affected = sessionRepository.findByStatus("active").stream()
                .filter(s -> s.getPlanId() != null && planIds.contains(s.getPlanId()))
                .toList();
        for (WorkoutSession session : affected) {
            recordHiddenExercise(exerciseId, session.getId());
        }
        return affected.size();
    }

    @Transactional
    public void cleanupBySession(Long sessionId) {
        draftRepository.deleteBySessionId(sessionId);
    }

    private Optional<Exercise> findReplacement(Exercise original) {
        Specification<Exercise> spec = Specification.where(
                (root, cq, cb) -> cb.equal(root.get("status"), "active"));
        if (original.getMuscleGroup() != null) {
            spec = spec.and((root, cq, cb) ->
                    cb.equal(root.get("muscleGroup"), original.getMuscleGroup()));
        }
        if (original.getBodyPart() != null) {
            spec = spec.and((root, cq, cb) ->
                    cb.equal(root.get("bodyPart"), original.getBodyPart()));
        }
        spec = spec.and((root, cq, cb) -> cb.notEqual(root.get("id"), original.getId()));
        return exerciseRepository.findAll(spec, Sort.by("id")).stream().findFirst();
    }
}
