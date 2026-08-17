package com.workoutsmart.plan.service;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.entity.DraftExercise;
import com.workoutsmart.plan.repository.DraftExerciseRepository;
import java.util.Optional;
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

    public DraftExerciseService(DraftExerciseRepository draftRepository,
                                ExerciseRepository exerciseRepository) {
        this.draftRepository = draftRepository;
        this.exerciseRepository = exerciseRepository;
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
