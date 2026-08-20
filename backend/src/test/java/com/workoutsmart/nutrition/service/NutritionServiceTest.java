package com.workoutsmart.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.nutrition.dto.CreateFoodRequest;
import com.workoutsmart.nutrition.dto.CreateMealRequest;
import com.workoutsmart.nutrition.dto.FoodResponse;
import com.workoutsmart.nutrition.dto.ImportFoodRequest;
import com.workoutsmart.nutrition.dto.ImportFoodRow;
import com.workoutsmart.nutrition.dto.MealEntryRequest;
import com.workoutsmart.nutrition.dto.MealResponse;
import com.workoutsmart.nutrition.dto.NutritionSummaryResponse;
import com.workoutsmart.nutrition.entity.FoodItem;
import com.workoutsmart.nutrition.entity.MealLog;
import com.workoutsmart.nutrition.repository.BodyMetricRepository;
import com.workoutsmart.nutrition.repository.FoodItemRepository;
import com.workoutsmart.nutrition.repository.MealEntryRepository;
import com.workoutsmart.nutrition.repository.MealLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NutritionServiceTest {

    @Mock
    private FoodItemRepository foodRepository;
    @Mock
    private MealLogRepository mealLogRepository;
    @Mock
    private MealEntryRepository mealEntryRepository;
    @Mock
    private BodyMetricRepository bodyMetricRepository;
    @Mock
    private UserRepository userRepository;

    private NutritionService service;

    @BeforeEach
    void setUp() {
        service = new NutritionService(foodRepository, mealLogRepository, mealEntryRepository,
                bodyMetricRepository, userRepository);
    }

    private FoodItem rice() {
        return FoodItem.builder()
                .id(1L).name("Cơm trắng").source("system")
                .caloriesPer100g(new BigDecimal("130"))
                .proteinPer100g(new BigDecimal("2.7"))
                .carbPer100g(new BigDecimal("28"))
                .fatPer100g(new BigDecimal("0.3"))
                .build();
    }

    @Test
    void createFoodSavesCustomFood() {
        when(foodRepository.save(any(FoodItem.class))).thenAnswer(inv -> {
            FoodItem f = inv.getArgument(0);
            f.setId(5L);
            return f;
        });

        FoodResponse res = service.createFood(1L, new CreateFoodRequest(
                "Cơm tấm", new BigDecimal("200"), new BigDecimal("5"),
                new BigDecimal("30"), new BigDecimal("6")));

        assertEquals("Cơm tấm", res.name());
        assertEquals("user_custom", res.source());
    }

    @Test
    void createMealComputesTotalsFromEntries() {
        when(mealLogRepository.findByUserIdAndLogDateAndMealNumber(1L, LocalDate.of(2026, 8, 17), 1))
                .thenReturn(Optional.empty());
        when(mealLogRepository.save(any(MealLog.class))).thenAnswer(inv -> {
            MealLog ml = inv.getArgument(0);
            ml.setId(10L);
            return ml;
        });
        when(foodRepository.findById(1L)).thenReturn(Optional.of(rice()));
        when(mealEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MealResponse res = service.createMeal(1L, new CreateMealRequest(1,
                LocalDate.of(2026, 8, 17), List.of(new MealEntryRequest(1L, new BigDecimal("200")))));

        // 200g cơm = 2 × 130 calo = 260
        assertEquals(0, new BigDecimal("260.0").compareTo(res.totalCalories()));
        assertEquals(0, new BigDecimal("5.4").compareTo(res.totalProtein()));
        assertEquals(0, new BigDecimal("56.0").compareTo(res.totalCarb()));
    }

    @Test
    void createMealDuplicateMealNumberReturns409() {
        when(mealLogRepository.findByUserIdAndLogDateAndMealNumber(1L, LocalDate.of(2026, 8, 17), 1))
                .thenReturn(Optional.of(MealLog.builder().id(9L).userId(1L)
                        .mealNumber(1).logDate(LocalDate.of(2026, 8, 17)).build()));

        ApiException ex = assertThrows(ApiException.class, () -> service.createMeal(1L,
                new CreateMealRequest(1, LocalDate.of(2026, 8, 17),
                        List.of(new MealEntryRequest(1L, new BigDecimal("100"))))));
        assertEquals(409, ex.getStatus().value());
        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    void createMealWithDeletedFoodThrows404() {
        when(mealLogRepository.findByUserIdAndLogDateAndMealNumber(1L, LocalDate.of(2026, 8, 17), 1))
                .thenReturn(Optional.empty());
        when(mealLogRepository.save(any(MealLog.class))).thenAnswer(inv -> {
            MealLog ml = inv.getArgument(0);
            ml.setId(10L);
            return ml;
        });
        FoodItem deleted = rice();
        deleted.setDeletedAt(Instant.now());
        when(foodRepository.findById(1L)).thenReturn(Optional.of(deleted));

        ApiException ex = assertThrows(ApiException.class, () -> service.createMeal(1L,
                new CreateMealRequest(1, LocalDate.of(2026, 8, 17),
                        List.of(new MealEntryRequest(1L, new BigDecimal("100"))))));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void deleteFoodSoftDeletesOwnFood() {
        FoodItem food = FoodItem.builder()
                .id(3L).name("Tự làm").source("user_custom").createdBy(1L)
                .caloriesPer100g(new BigDecimal("100"))
                .build();
        when(foodRepository.findById(3L)).thenReturn(Optional.of(food));

        service.deleteFood(1L, 3L);

        org.junit.jupiter.api.Assertions.assertNotNull(food.getDeletedAt());
    }

    @Test
    void deleteSystemFoodReturns403() {
        when(foodRepository.findById(1L)).thenReturn(Optional.of(rice()));

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteFood(1L, 1L));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void deleteOthersFoodReturns403() {
        FoodItem other = FoodItem.builder()
                .id(4L).name("Của người khác").source("user_custom").createdBy(99L)
                .caloriesPer100g(new BigDecimal("100"))
                .build();
        when(foodRepository.findById(4L)).thenReturn(Optional.of(other));

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteFood(1L, 4L));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void importFoodsNormalizesTo100gAndSkipsInvalid() {
        when(foodRepository.save(any(FoodItem.class))).thenAnswer(i -> i.getArgument(0));

        var res = service.importFoods(1L, new ImportFoodRequest(List.of(
                // 250g có 500 kcal, 50g protein → 100g có 200 kcal, 20g protein
                new ImportFoodRow("Ức gà luộc", new BigDecimal("250"), new BigDecimal("50"),
                        new BigDecimal("0"), new BigDecimal("9"), new BigDecimal("500")),
                new ImportFoodRow("", new BigDecimal("100"), new BigDecimal("1"),
                        new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("10")))));

        assertEquals(1, res.imported());
        assertEquals(1, res.errors().size());
        assertEquals("Dòng 2: thiếu hoặc sai tên món", res.errors().get(0));
    }

    @Test
    void summaryRequiresBodyData() {
        User incomplete = User.builder().id(1L).email("u@e.c")
                .accountStatus(AccountStatus.ACTIVE).sex("male").age(25)
                .heightCm(new BigDecimal("170")).weightKg(null)
                .activityLevel("moderate").goalType("weight_loss").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(incomplete));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getSummary(1L, LocalDate.of(2026, 8, 17)));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void summaryComputesStatusThua() {
        User u = User.builder().id(1L).email("u@e.c")
                .accountStatus(AccountStatus.ACTIVE).sex("male").age(35)
                .heightCm(new BigDecimal("170")).weightKg(new BigDecimal("68"))
                .activityLevel("moderate").goalType("endurance").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        MealLog ml = MealLog.builder().id(1L).userId(1L).mealNumber(1)
                .logDate(LocalDate.of(2026, 8, 17)).build();
        when(mealLogRepository.findByUserIdAndLogDateOrderByMealNumberAsc(1L, LocalDate.of(2026, 8, 17)))
                .thenReturn(List.of(ml));
        when(mealEntryRepository.findByMealLogId(1L)).thenReturn(List.of());

        NutritionSummaryResponse res = service.getSummary(1L, LocalDate.of(2026, 8, 17));
        assertEquals("thiếu", res.status());
        // BMR = 1572.5 × 1.55 = 2437.375 → 2437
        assertEquals(0, new BigDecimal("2437").compareTo(res.targetCalories()));
    }

    @Test
    void needsComputesTdeeMacrosPerMeal() {
        User u = User.builder().id(1L).email("u@e.c")
                .accountStatus(AccountStatus.ACTIVE).sex("male").age(35)
                .heightCm(new BigDecimal("170")).weightKg(new BigDecimal("68"))
                .activityLevel("moderate").goalType("weight_loss")
                .calorieGoal("cut_light").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        var res = service.getNeeds(1L);

        // BMR ≈ 1573; TDEE ≈ 2437; cut_light = −300 → 2137
        assertEquals(0, new BigDecimal("1573").compareTo(res.bmr()));
        assertEquals(0, new BigDecimal("2437").compareTo(res.tdee()));
        assertEquals(0, new BigDecimal("2137").compareTo(res.targetCalories()));
        assertEquals("cut_light", res.calorieGoal());
        // Protein 2g/kg = 136g
        assertEquals(0, new BigDecimal("136.0").compareTo(res.proteinG()));
        // 3 bữa → mỗi bữa ≈ 712 kcal
        assertEquals(3, res.mealsPerDay());
        assertEquals(0, new BigDecimal("712").compareTo(res.perMealCalories()));
        // macro không âm
        assertTrue(res.carbG().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(res.fatG().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void needsRequiresBodyData() {
        User incomplete = User.builder().id(1L).email("u@e.c")
                .accountStatus(AccountStatus.ACTIVE).sex("male").age(25)
                .heightCm(new BigDecimal("170")).weightKg(null)
                .activityLevel("moderate").goalType("weight_loss").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(incomplete));

        ApiException ex = assertThrows(ApiException.class, () -> service.getNeeds(1L));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void updateMealDeletesAndRecreatesEntries() {
        MealLog ml = MealLog.builder().id(7L).userId(1L).mealNumber(2)
                .logDate(LocalDate.of(2026, 8, 17)).build();
        when(mealLogRepository.findById(7L)).thenReturn(Optional.of(ml));
        when(foodRepository.findById(1L)).thenReturn(Optional.of(rice()));
        when(mealEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MealResponse res = service.updateMeal(1L, 7L, new CreateMealRequest(2,
                LocalDate.of(2026, 8, 17), List.of(new MealEntryRequest(1L, new BigDecimal("100")))));

        verify(mealEntryRepository).deleteByMealLogId(7L);
        assertEquals(0, new BigDecimal("130.0").compareTo(res.totalCalories()));
    }

    @Test
    void updateOthersMealReturns404() {
        MealLog ml = MealLog.builder().id(7L).userId(2L).mealNumber(1)
                .logDate(LocalDate.of(2026, 8, 17)).build();
        when(mealLogRepository.findById(7L)).thenReturn(Optional.of(ml));

        ApiException ex = assertThrows(ApiException.class, () -> service.updateMeal(1L, 7L,
                new CreateMealRequest(1, LocalDate.of(2026, 8, 17),
                        List.of(new MealEntryRequest(1L, new BigDecimal("100"))))));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void deleteMealRemovesEntriesAndLog() {
        MealLog ml = MealLog.builder().id(7L).userId(1L).mealNumber(2)
                .logDate(LocalDate.of(2026, 8, 17)).build();
        when(mealLogRepository.findById(7L)).thenReturn(Optional.of(ml));

        var res = service.deleteMeal(1L, 7L);

        verify(mealEntryRepository).deleteByMealLogId(7L);
        verify(mealLogRepository).delete(ml);
        assertEquals("Đã xóa bữa ăn", res.message());
    }

    @Test
    void deleteOthersMealReturns404() {
        MealLog ml = MealLog.builder().id(7L).userId(2L).mealNumber(1)
                .logDate(LocalDate.of(2026, 8, 17)).build();
        when(mealLogRepository.findById(7L)).thenReturn(Optional.of(ml));

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteMeal(1L, 7L));
        assertEquals(404, ex.getStatus().value());
    }
}
