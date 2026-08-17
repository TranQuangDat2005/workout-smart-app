package com.workoutsmart.exercise.repository;

import com.workoutsmart.exercise.entity.Exercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByStatus(String status);

    List<Exercise> findByEquipmentAndStatus(String equipment, String status);

    List<Exercise> findByNameContainingIgnoreCaseAndStatus(String keyword, String status);

    boolean existsByNameIgnoreCase(String name);
}
