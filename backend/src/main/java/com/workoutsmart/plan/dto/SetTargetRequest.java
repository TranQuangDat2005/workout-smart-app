package com.workoutsmart.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Target một hiệp trong payload sửa ngày — FR-001 (014) + duration (015). */
public record SetTargetRequest(
        @NotNull @Min(1) Integer setNumber,
        @Min(0) @Max(100) Integer targetReps,
        @Min(0) BigDecimal targetWeight,
        @Pattern(regexp = "normal|warm_up|drop_set") String setType,
        @Min(0) Integer targetDurationSeconds) {
}
