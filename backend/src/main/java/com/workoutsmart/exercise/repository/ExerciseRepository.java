package com.workoutsmart.exercise.repository;

import com.workoutsmart.exercise.entity.Exercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>,
        JpaSpecificationExecutor<Exercise> {

    List<Exercise> findByStatus(String status);

    List<Exercise> findByEquipmentAndStatus(String equipment, String status);

    List<Exercise> findByEquipmentInAndStatus(List<String> equipment, String status);

    List<Exercise> findByNameContainingIgnoreCaseAndStatus(String keyword, String status);

    boolean existsByNameIgnoreCase(String name);
}
