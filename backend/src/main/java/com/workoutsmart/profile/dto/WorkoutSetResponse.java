package com.workoutsmart.profile.dto;

import java.math.BigDecimal;

public record WorkoutSetResponse(
        Long id,
        int setNumber,
        Integer repsCompleted,
        BigDecimal weightUsed,
        Integer restTimeSeconds) {
}
