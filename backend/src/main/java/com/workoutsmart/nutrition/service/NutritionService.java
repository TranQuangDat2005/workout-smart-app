package com.workoutsmart.nutrition.service;

import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.nutrition.dto.BodyMetricResponse;
import com.workoutsmart.nutrition.dto.CreateBodyMetricRequest;
import com.workoutsmart.nutrition.dto.CreateFoodRequest;
import com.workoutsmart.nutrition.dto.CreateMealRequest;
import com.workoutsmart.nutrition.dto.FoodResponse;
import com.workoutsmart.nutrition.dto.MealEntryRequest;
import com.workoutsmart.nutrition.dto.MealEntryResponse;
import com.workoutsmart.nutrition.dto.MealResponse;
import com.workoutsmart.nutrition.dto.MessageResponse;
import com.workoutsmart.nutrition.dto.NutritionSummaryResponse;
import com.workoutsmart.nutrition.entity.BodyMetric;
import com.workoutsmart.nutrition.entity.FoodItem;
import com.workoutsmart.nutrition.entity.MealEntry;
import com.workoutsmart.nutrition.entity.MealLog;
import com.workoutsmart.nutrition.repository.BodyMetricRepository;
import com.workoutsmart.nutrition.repository.FoodItemRepository;
import com.workoutsmart.nutrition.repository.MealEntryRepository;
import com.workoutsmart.nutrition.repository.MealLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ dinh dưỡng — spec 002-nutrition-tracking. */
@Service
public class NutritionService {

    private final FoodItemRepository foodRepository;
    private final MealLogRepository mealLogRepository;
    private final MealEntryRepository mealEntryRepository;
    private final BodyMetricRepository bodyMetricRepository;
    private final UserRepository userRepository;

    public NutritionService(FoodItemRepository foodRepository,
                            MealLogRepository mealLogRepository,
                            MealEntryRepository mealEntryRepository,
                            BodyMetricRepository bodyMetricRepository,
                            UserRepository userRepository) {
        this.foodRepository = foodRepository;
        this.mealLogRepository = mealLogRepository;
        this.mealEntryRepository = mealEntryRepository;
        this.bodyMetricRepository = bodyMetricRepository;
        this.userRepository = userRepository;
    }

    // ---------- Food ----------

    public Page<FoodResponse> searchFoods(Long userId, String query, int page, int size) {
        String q = query == null ? "" : query.trim();
        int safeSize = Math.min(Math.max(size, 1), 100);
        return foodRepository.searchVisible(q, userId, PageRequest.of(Math.max(page, 0), safeSize))
                .map(this::toFoodResponse);
    }

    /** FR-006: tạo thực phẩm custom vào kho cá nhân. */
    @Transactional
    public FoodResponse createFood(Long userId, CreateFoodRequest request) {
        FoodItem food = FoodItem.builder()
                .name(request.name().trim())
                .caloriesPer100g(request.caloriesPer100g())
                .proteinPer100g(request.proteinPer100g())
                .carbPer100g(request.carbPer100g())
                .fatPer100g(request.fatPer100g())
                .source("user_custom")
                .createdBy(userId)
                .build();
        return toFoodResponse(foodRepository.save(food));
    }

    /** FR-006c: sửa food của chính mình. */
    @Transactional
    public FoodResponse updateFood(Long userId, Long foodId, CreateFoodRequest request) {
        FoodItem food = requireOwnFood(userId, foodId);
        food.setName(request.name().trim());
        food.setCaloriesPer100g(request.caloriesPer100g());
        food.setProteinPer100g(request.proteinPer100g());
        food.setCarbPer100g(request.carbPer100g());
        food.setFatPer100g(request.fatPer100g());
        return toFoodResponse(foodRepository.save(food));
    }

    /** FR-006c: soft-delete food của mình (giữ draft hiển thị lịch sử 1 tuần). */
    @Transactional
    public MessageResponse deleteFood(Long userId, Long foodId) {
        FoodItem food = requireOwnFood(userId, foodId);
        food.setDeletedAt(Instant.now());
        foodRepository.save(food);
        return new MessageResponse("Đã xóa thực phẩm khỏi kho cá nhân.");
    }

    // ---------- Meal ----------

    /** FR-001..004: tạo bữa nhiều món, tính calo/macro tự động. */
    @Transactional
    public MealResponse createMeal(Long userId, CreateMealRequest request) {
        if (mealLogRepository.findByUserIdAndLogDateAndMealNumber(
                userId, request.logDate(), request.mealNumber()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bữa này đã tồn tại trong ngày");
        }
        MealLog mealLog = mealLogRepository.save(MealLog.builder()
                .userId(userId)
                .mealNumber(request.mealNumber())
                .logDate(request.logDate())
                .build());

        List<MealEntryResponse> entries = saveEntries(mealLog.getId(), request.entries());

        BigDecimal totalCalories = sum(entries, MealEntryResponse::totalCalories);
        BigDecimal totalProtein = sum(entries, MealEntryResponse::totalProtein);
        BigDecimal totalCarb = sum(entries, MealEntryResponse::totalCarb);
        BigDecimal totalFat = sum(entries, MealEntryResponse::totalFat);

        return new MealResponse(mealLog.getId(), mealLog.getMealNumber(), mealLog.getLogDate(),
                entries, totalCalories, totalProtein, totalCarb, totalFat);
    }

    /** FR-006b: sửa bữa (ghi đè entries), KHÔNG xóa. */
    @Transactional
    public MealResponse updateMeal(Long userId, Long mealId, CreateMealRequest request) {
        MealLog mealLog = requireOwnMeal(userId, mealId);
        mealEntryRepository.deleteByMealLogId(mealId);
        mealLog.setMealNumber(request.mealNumber());
        mealLog.setLogDate(request.logDate());
        mealLogRepository.save(mealLog);

        List<MealEntryResponse> entries = saveEntries(mealId, request.entries());
        return new MealResponse(mealLog.getId(), mealLog.getMealNumber(), mealLog.getLogDate(),
                entries, sum(entries, MealEntryResponse::totalCalories),
                sum(entries, MealEntryResponse::totalProtein),
                sum(entries, MealEntryResponse::totalCarb),
                sum(entries, MealEntryResponse::totalFat));
    }

    public List<MealResponse> getMeals(Long userId, LocalDate date) {
        LocalDate d = date == null ? LocalDate.now() : date;
        return mealLogRepository.findByUserIdAndLogDateOrderByMealNumberAsc(userId, d).stream()
                .map(ml -> toMealResponse(ml))
                .toList();
    }

    /** FR-005 + TDEE: tổng ngày so với mục tiêu calo. */
    public NutritionSummaryResponse getSummary(Long userId, LocalDate date) {
        LocalDate d = date == null ? LocalDate.now() : date;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        BigDecimal targetCalories;
        try {
            targetCalories = TdeeCalculator.targetCalories(user);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cần nhập đầy đủ thông tin cơ thể (giới tính, tuổi, chiều cao, cân nặng, mức vận động) để tính TDEE");
        }

        List<MealResponse> meals = getMeals(userId, d);
        BigDecimal totalCalories = meals.stream().map(MealResponse::totalCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProtein = meals.stream().map(MealResponse::totalProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarb = meals.stream().map(MealResponse::totalCarb)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = meals.stream().map(MealResponse::totalFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deficit = totalCalories.subtract(targetCalories);
        String status;
        BigDecimal tolerance = new BigDecimal("100");
        if (deficit.compareTo(tolerance) > 0) {
            status = "thừa";
        } else if (deficit.compareTo(tolerance.negate()) < 0) {
            status = "thiếu";
        } else {
            status = "đủ";
        }
        return new NutritionSummaryResponse(d, totalCalories, totalProtein, totalCarb, totalFat,
                targetCalories, deficit, status);
    }

    // ---------- Body metrics ----------

    /** FR-007..009: nhập chỉ số cơ thể + sync cân nặng lên users. */
    @Transactional
    public BodyMetricResponse createBodyMetric(Long userId, CreateBodyMetricRequest request) {
        BigDecimal previous = bodyMetricRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)
                .map(BodyMetric::getWeightKg)
                .orElse(null);

        BodyMetric metric = bodyMetricRepository.save(BodyMetric.builder()
                .userId(userId)
                .weightKg(request.weightKg())
                .bodyFatPct(request.bodyFatPct())
                .waistCm(request.waistCm())
                .chestCm(request.chestCm())
                .armCm(request.armCm())
                .build());

        User user = userRepository.findById(userId).orElseThrow();
        user.setWeightKg(request.weightKg());
        userRepository.save(user);

        BigDecimal delta = previous == null ? null : request.weightKg().subtract(previous);
        return new BodyMetricResponse(metric.getId(), metric.getWeightKg(), metric.getBodyFatPct(),
                metric.getWaistCm(), metric.getChestCm(), metric.getArmCm(), delta, metric.getRecordedAt());
    }

    public List<BodyMetricResponse> getBodyMetrics(Long userId) {
        List<BodyMetric> metrics = bodyMetricRepository.findTop100ByUserIdOrderByRecordedAtDesc(userId);
        List<BodyMetricResponse> result = new ArrayList<>();
        for (int i = 0; i < metrics.size(); i++) {
            BodyMetric m = metrics.get(i);
            BigDecimal delta = i < metrics.size() - 1
                    ? m.getWeightKg().subtract(metrics.get(i + 1).getWeightKg())
                    : null;
            result.add(new BodyMetricResponse(m.getId(), m.getWeightKg(), m.getBodyFatPct(),
                    m.getWaistCm(), m.getChestCm(), m.getArmCm(), delta, m.getRecordedAt()));
        }
        return result;
    }

    // ---------- helpers ----------

    private List<MealEntryResponse> saveEntries(Long mealLogId, List<MealEntryRequest> entries) {
        List<MealEntryResponse> result = new ArrayList<>();
        for (MealEntryRequest req : entries) {
            FoodItem food = foodRepository.findById(req.foodItemId())
                    .filter(f -> f.getDeletedAt() == null)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thực phẩm không tồn tại"));
            BigDecimal ratio = req.portionGrams().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            MealEntry entry = mealEntryRepository.save(MealEntry.builder()
                    .mealLogId(mealLogId)
                    .foodItemId(food.getId())
                    .portionGrams(req.portionGrams())
                    .totalCalories(food.getCaloriesPer100g().multiply(ratio).setScale(1, RoundingMode.HALF_UP))
                    .totalProtein(food.getProteinPer100g().multiply(ratio).setScale(1, RoundingMode.HALF_UP))
                    .totalCarb(food.getCarbPer100g().multiply(ratio).setScale(1, RoundingMode.HALF_UP))
                    .totalFat(food.getFatPer100g().multiply(ratio).setScale(1, RoundingMode.HALF_UP))
                    .build());
            result.add(new MealEntryResponse(entry.getId(), food.getId(), food.getName(),
                    entry.getPortionGrams(), entry.getTotalCalories(), entry.getTotalProtein(),
                    entry.getTotalCarb(), entry.getTotalFat()));
        }
        return result;
    }

    private MealResponse toMealResponse(MealLog ml) {
        List<MealEntryResponse> entries = mealEntryRepository.findByMealLogId(ml.getId()).stream()
                .map(e -> {
                    FoodItem food = foodRepository.findById(e.getFoodItemId()).orElse(null);
                    return new MealEntryResponse(e.getId(), e.getFoodItemId(),
                            food != null ? food.getName() : "Đã xóa",
                            e.getPortionGrams(), e.getTotalCalories(), e.getTotalProtein(),
                            e.getTotalCarb(), e.getTotalFat());
                })
                .toList();
        return new MealResponse(ml.getId(), ml.getMealNumber(), ml.getLogDate(), entries,
                sum(entries, MealEntryResponse::totalCalories),
                sum(entries, MealEntryResponse::totalProtein),
                sum(entries, MealEntryResponse::totalCarb),
                sum(entries, MealEntryResponse::totalFat));
    }

    private FoodItem requireOwnFood(Long userId, Long foodId) {
        FoodItem food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thực phẩm không tồn tại"));
        if (!"user_custom".equals(food.getSource()) || !userId.equals(food.getCreatedBy())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ được sửa/xóa thực phẩm do bạn tạo");
        }
        return food;
    }

    private MealLog requireOwnMeal(Long userId, Long mealId) {
        MealLog mealLog = mealLogRepository.findById(mealId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bữa ăn không tồn tại"));
        if (!mealLog.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bữa ăn không tồn tại");
        }
        return mealLog;
    }

    private FoodResponse toFoodResponse(FoodItem food) {
        return new FoodResponse(food.getId(), food.getName(), food.getCaloriesPer100g(),
                food.getProteinPer100g(), food.getCarbPer100g(), food.getFatPer100g(), food.getSource());
    }

    private BigDecimal sum(List<MealEntryResponse> entries,
                           java.util.function.Function<MealEntryResponse, BigDecimal> getter) {
        return entries.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
