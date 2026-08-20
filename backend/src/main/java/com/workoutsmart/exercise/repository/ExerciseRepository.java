package com.workoutsmart.exercise.repository;

import com.workoutsmart.exercise.entity.Exercise;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>,
        JpaSpecificationExecutor<Exercise> {

    List<Exercise> findByStatus(String status);

    List<Exercise> findByEquipmentAndStatus(String equipment, String status);

    List<Exercise> findByEquipmentInAndStatus(List<String> equipment, String status);

    List<Exercise> findByCreatedByAndEquipmentInAndStatusAndDeletedAtIsNull(
            Long createdBy, List<String> equipment, String status);

    Optional<Exercise> findByIdAndCreatedBy(Long id, Long createdBy);

    List<Exercise> findByDeletedAtBefore(Instant deletedAtBefore);

    List<Exercise> findByNameContainingIgnoreCaseAndStatus(String keyword, String status);

    boolean existsByNameIgnoreCase(String name);

    Optional<Exercise> findByNameIgnoreCaseAndEquipmentIgnoreCase(String name, String equipment);
}
