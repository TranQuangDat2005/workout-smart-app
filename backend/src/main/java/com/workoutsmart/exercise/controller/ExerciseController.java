package com.workoutsmart.exercise.controller;

import com.workoutsmart.exercise.dto.CreateCustomExerciseRequest;
import com.workoutsmart.exercise.dto.ExerciseDetailResponse;
import com.workoutsmart.exercise.dto.ExerciseSearchResponse;
import com.workoutsmart.exercise.dto.UpdateCustomExerciseRequest;
import com.workoutsmart.exercise.service.ExerciseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    public ExerciseSearchResponse search(Authentication auth,
                                         @RequestParam(required = false) List<String> equipment,
                                         @RequestParam(required = false) List<String> category,
                                         @RequestParam(required = false) String bodyPart,
                                         @RequestParam(required = false) String muscleGroup,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Long viewerId = auth != null ? (Long) auth.getPrincipal() : null;
        return exerciseService.search(equipment, category, bodyPart, muscleGroup, q, page, size, viewerId);
    }

    @GetMapping("/{exerciseId}")
    public ExerciseDetailResponse detail(Authentication auth, @PathVariable Long exerciseId) {
        Long viewerId = auth != null ? (Long) auth.getPrincipal() : null;
        return exerciseService.findById(exerciseId, viewerId);
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseDetailResponse createCustom(Authentication auth,
                                               @Valid @RequestBody CreateCustomExerciseRequest request) {
        return exerciseService.createCustomExercise((Long) auth.getPrincipal(), request);
    }

    @PutMapping("/custom/{exerciseId}")
    public ExerciseDetailResponse updateCustom(Authentication auth,
                                               @PathVariable Long exerciseId,
                                               @Valid @RequestBody UpdateCustomExerciseRequest request) {
        return exerciseService.updateCustomExercise((Long) auth.getPrincipal(), exerciseId, request);
    }

    @DeleteMapping("/custom/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustom(Authentication auth, @PathVariable Long exerciseId) {
        exerciseService.deleteCustomExercise((Long) auth.getPrincipal(), exerciseId);
    }
}
