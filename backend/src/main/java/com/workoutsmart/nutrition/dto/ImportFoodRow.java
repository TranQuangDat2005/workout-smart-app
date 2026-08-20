package com.workoutsmart.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 019c: một dòng CSV nhập kho thực phẩm.
 * Gram/ml = định lượng mà Protein/Carb/Fat/Calo áp dụng; hệ thống quy đổi về 100g/ml.
 */
public record ImportFoodRow(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.1") BigDecimal gram,
        @NotNull @DecimalMin(value = "0.0") BigDecimal protein,
        @NotNull @DecimalMin(value = "0.0") BigDecimal carb,
        @NotNull @DecimalMin(value = "0.0") BigDecimal fat,
        @NotNull @DecimalMin(value = "0.0") BigDecimal calories) {
}
