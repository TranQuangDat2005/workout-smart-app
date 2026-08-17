package com.workoutsmart.exercise.service;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.dto.ExerciseDetailResponse;
import com.workoutsmart.exercise.dto.ExerciseSearchResponse;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Nghiệp vụ tra cứu bài tập — FR-003, FR-004, FR-006, FR-007. */
@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public ExerciseDetailResponse findById(Long id) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
        return toDetail(exercise);
    }

    public ExerciseSearchResponse search(String equipment, String category, String bodyPart,
                                         String muscleGroup, String q, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Specification<Exercise> spec = Specification.where(
                (root, cq, cb) -> cb.equal(root.get("status"), "active"));
        if (hasText(equipment)) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("equipment"), equipment));
        }
        if (hasText(category)) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("category"), category));
        }
        if (hasText(bodyPart)) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("bodyPart"), bodyPart));
        }
        if (hasText(muscleGroup)) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("muscleGroup"), muscleGroup));
        }
        if (hasText(q)) {
            spec = spec.and((root, cq, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"));
        }

        Page<Exercise> result = exerciseRepository.findAll(
                spec, PageRequest.of(safePage, safeSize, Sort.by("id")));
        List<ExerciseDetailResponse> content = result.getContent().stream()
                .map(this::toDetail)
                .toList();
        return new ExerciseSearchResponse(content, result.getTotalElements(), result.getTotalPages(),
                safePage, safeSize);
    }

    public List<Exercise> findActiveByEquipment(List<String> equipment) {
        return exerciseRepository.findByEquipmentInAndStatus(equipment, "active");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ExerciseDetailResponse toDetail(Exercise e) {
        return new ExerciseDetailResponse(e.getId(), e.getName(), e.getCategory(), e.getBodyPart(),
                e.getEquipment(), e.getTarget(), e.getMuscleGroup(), e.getImage(), e.getGifUrl(),
                e.getInstructions());
    }
}
