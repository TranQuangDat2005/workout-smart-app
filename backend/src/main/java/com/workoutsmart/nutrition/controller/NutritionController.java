package com.workoutsmart.nutrition.controller;

import com.workoutsmart.nutrition.dto.BodyMetricResponse;
import com.workoutsmart.nutrition.dto.CreateBodyMetricRequest;
import com.workoutsmart.nutrition.dto.CreateFoodRequest;
import com.workoutsmart.nutrition.dto.CreateMealRequest;
import com.workoutsmart.nutrition.dto.FoodResponse;
import com.workoutsmart.nutrition.dto.MealResponse;
import com.workoutsmart.nutrition.dto.MessageResponse;
import com.workoutsmart.nutrition.dto.NutritionSummaryResponse;
import com.workoutsmart.nutrition.service.NutritionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Controller mỏng — logic nằm ở NutritionService. */
@RestController
@RequestMapping("/api/v1")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping("/foods")
    public Page<FoodResponse> searchFoods(Authentication auth,
                                          @RequestParam(defaultValue = "") String query,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return nutritionService.searchFoods(currentUserId(auth), query, page, size);
    }

    @PostMapping("/foods")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodResponse createFood(Authentication auth, @Valid @RequestBody CreateFoodRequest request) {
        return nutritionService.createFood(currentUserId(auth), request);
    }

    @PutMapping("/foods/{id}")
    public FoodResponse updateFood(Authentication auth, @PathVariable Long id,
                                   @Valid @RequestBody CreateFoodRequest request) {
        return nutritionService.updateFood(currentUserId(auth), id, request);
    }

    @DeleteMapping("/foods/{id}")
    public MessageResponse deleteFood(Authentication auth, @PathVariable Long id) {
        return nutritionService.deleteFood(currentUserId(auth), id);
    }

    @PostMapping("/meals")
    @ResponseStatus(HttpStatus.CREATED)
    public MealResponse createMeal(Authentication auth, @Valid @RequestBody CreateMealRequest request) {
        return nutritionService.createMeal(currentUserId(auth), request);
    }

    @PutMapping("/meals/{id}")
    public MealResponse updateMeal(Authentication auth, @PathVariable Long id,
                                   @Valid @RequestBody CreateMealRequest request) {
        return nutritionService.updateMeal(currentUserId(auth), id, request);
    }

    @GetMapping("/meals")
    public List<MealResponse> getMeals(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return nutritionService.getMeals(currentUserId(auth), date);
    }

    @GetMapping("/nutrition/summary")
    public NutritionSummaryResponse getSummary(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return nutritionService.getSummary(currentUserId(auth), date);
    }

    @PostMapping("/body-metrics")
    @ResponseStatus(HttpStatus.CREATED)
    public BodyMetricResponse createBodyMetric(Authentication auth,
                                               @Valid @RequestBody CreateBodyMetricRequest request) {
        return nutritionService.createBodyMetric(currentUserId(auth), request);
    }

    @GetMapping("/body-metrics")
    public List<BodyMetricResponse> getBodyMetrics(Authentication auth) {
        return nutritionService.getBodyMetrics(currentUserId(auth));
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
