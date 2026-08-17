package com.workoutsmart.exercise.controller;

import com.workoutsmart.exercise.dto.ExerciseDetailResponse;
import com.workoutsmart.exercise.dto.ExerciseSearchResponse;
import com.workoutsmart.exercise.service.ExerciseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller mỏng — logic nằm ở ExerciseService (constitution §3). */
@RestController
@RequestMapping("/api/v1/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public ExerciseSearchResponse search(
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String bodyPart,
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return exerciseService.search(equipment, category, bodyPart, muscleGroup, q, page, size);
    }

    @GetMapping("/{exerciseId}")
    public ExerciseDetailResponse detail(@PathVariable Long exerciseId) {
        return exerciseService.findById(exerciseId);
    }
}
