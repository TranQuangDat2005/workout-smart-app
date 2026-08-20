package com.workoutsmart.exercise.service;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.ExerciseTaxonomy;
import com.workoutsmart.exercise.dto.CreateCustomExerciseRequest;
import com.workoutsmart.exercise.dto.ExerciseDetailResponse;
import com.workoutsmart.exercise.dto.ExerciseSearchResponse;
import com.workoutsmart.exercise.dto.UpdateCustomExerciseRequest;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ tra cứu bài tập + bài tập tự tạo cá nhân — FR-003, FR-004, FR-006, FR-007 + 011. */
@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public ExerciseDetailResponse findById(Long id) {
        return findById(id, null);
    }

    public ExerciseDetailResponse findById(Long id, Long viewerId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
        if (isCustomOfOtherUser(exercise, viewerId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại");
        }
        return toDetail(exercise);
    }

    public ExerciseSearchResponse search(String equipment, String category, String bodyPart,
                                         String muscleGroup, String q, int page, int size) {
        return search(singleton(equipment), singleton(category), bodyPart, muscleGroup, q, page, size, null);
    }

    public ExerciseSearchResponse search(String equipment, String category, String bodyPart,
                                         String muscleGroup, String q, int page, int size, Long viewerId) {
        return search(singleton(equipment), singleton(category), bodyPart, muscleGroup, q, page, size, viewerId);
    }

    public ExerciseSearchResponse search(List<String> equipment, List<String> category, String bodyPart,
                                         String muscleGroup, String q, int page, int size, Long viewerId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Specification<Exercise> spec = visibleSpec(viewerId);
        List<String> equipmentValues = nonBlank(equipment);
        if (!equipmentValues.isEmpty()) {
            spec = spec.and((root, cq, cb) -> root.get("equipment").in(equipmentValues));
        }
        List<String> categoryValues = nonBlank(category);
        if (!categoryValues.isEmpty()) {
            spec = spec.and((root, cq, cb) -> root.get("category").in(categoryValues));
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

    /** Bao gồm bài tập hệ thống + bài tập cá nhân của User (dùng cho Rule Engine — FR-005 011). */
    public List<Exercise> findActiveByEquipmentForUser(List<String> equipment, Long userId) {
        List<Exercise> system = exerciseRepository.findByEquipmentInAndStatus(equipment, "active").stream()
                .filter(e -> !"user_custom".equals(e.getSource()))
                .toList();
        List<Exercise> custom = exerciseRepository
                .findByCreatedByAndEquipmentInAndStatusAndDeletedAtIsNull(userId, equipment, "active");
        List<Exercise> merged = new ArrayList<>(system);
        merged.addAll(custom);
        return merged;
    }

    @Transactional
    public ExerciseDetailResponse createCustomExercise(Long userId, CreateCustomExerciseRequest req) {
        requireTaxonomy(req.category(), req.equipment().trim(), req.muscleGroup());
        String category = req.category().trim();
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .name(req.name().trim())
                .muscleGroup(req.muscleGroup())
                .equipment(req.equipment().trim())
                .category(category)
                .bodyPart(hasText(req.bodyPart()) ? req.bodyPart().trim() : category)
                .target(req.target())
                .image(req.image())
                .gifUrl(req.gifUrl())
                .instructions(req.instructions())
                .status("active")
                .source("user_custom")
                .createdBy(userId)
                .build());
        return toDetail(exercise);
    }

    @Transactional
    public ExerciseDetailResponse updateCustomExercise(Long userId, Long exerciseId, UpdateCustomExerciseRequest req) {
        Exercise exercise = requireOwnCustom(userId, exerciseId);
        if (req.name() != null) exercise.setName(req.name().trim());
        if (req.muscleGroup() != null) {
            requireMuscleGroup(req.muscleGroup());
            exercise.setMuscleGroup(req.muscleGroup());
        }
        if (req.equipment() != null) {
            requireEquipment(req.equipment().trim());
            exercise.setEquipment(req.equipment().trim());
        }
        if (req.category() != null) {
            requireCategory(req.category().trim());
            exercise.setCategory(req.category().trim());
            if (req.bodyPart() == null) {
                exercise.setBodyPart(req.category().trim());
            }
        }
        if (req.bodyPart() != null) exercise.setBodyPart(req.bodyPart());
        if (req.target() != null) exercise.setTarget(req.target());
        if (req.image() != null) exercise.setImage(req.image());
        if (req.gifUrl() != null) exercise.setGifUrl(req.gifUrl());
        if (req.instructions() != null) exercise.setInstructions(req.instructions());
        return toDetail(exerciseRepository.save(exercise));
    }

    @Transactional
    public void deleteCustomExercise(Long userId, Long exerciseId) {
        Exercise exercise = requireOwnCustom(userId, exerciseId);
        exercise.setDeletedAt(Instant.now());
        exerciseRepository.save(exercise);
    }

    /** Xóa cứng bài tập đã soft-delete quá 1 tuần (retention). */
    @Transactional
    public void purgeExpiredCustomExercises() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        exerciseRepository.deleteAll(exerciseRepository.findByDeletedAtBefore(threshold));
    }

    private Exercise requireOwnCustom(Long userId, Long exerciseId) {
        Exercise exercise = exerciseRepository.findByIdAndCreatedBy(exerciseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
        if (!"user_custom".equals(exercise.getSource())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ được sửa/xóa bài tập do bạn tạo");
        }
        return exercise;
    }

    private boolean isCustomOfOtherUser(Exercise exercise, Long viewerId) {
        return "user_custom".equals(exercise.getSource())
                && (viewerId == null || !viewerId.equals(exercise.getCreatedBy()));
    }

    private Specification<Exercise> visibleSpec(Long viewerId) {
        return (root, cq, cb) -> {
            var active = cb.equal(root.get("status"), "active");
            var notDeleted = cb.isNull(root.get("deletedAt"));
            var isSystem = cb.equal(root.get("source"), "system");
            var isOwnCustom = cb.and(
                    cb.equal(root.get("source"), "user_custom"),
                    cb.equal(root.get("createdBy"), viewerId == null ? -1L : viewerId));
            return cb.and(active, notDeleted, cb.or(isSystem, isOwnCustom));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> singleton(String value) {
        return hasText(value) ? List.of(value) : List.of();
    }

    private List<String> nonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(this::hasText).distinct().toList();
    }

    private void requireTaxonomy(String category, String equipment, String muscleGroup) {
        requireCategory(category);
        requireEquipment(equipment);
        requireMuscleGroup(muscleGroup);
    }

    private void requireCategory(String category) {
        if (!ExerciseTaxonomy.isCategory(category)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category không hợp lệ");
        }
    }

    private void requireEquipment(String equipment) {
        if (!ExerciseTaxonomy.isEquipment(equipment)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dụng cụ không hợp lệ");
        }
    }

    private void requireMuscleGroup(String muscleGroup) {
        if (!ExerciseTaxonomy.isMuscleGroup(muscleGroup)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nhóm cơ không hợp lệ");
        }
    }

    private ExerciseDetailResponse toDetail(Exercise e) {
        return new ExerciseDetailResponse(e.getId(), e.getName(), e.getCategory(), e.getBodyPart(),
                e.getEquipment(), e.getTarget(), e.getMuscleGroup(), e.getImage(), e.getGifUrl(),
                e.getInstructions(), e.getSource(), e.getMeasureType());
    }
}
