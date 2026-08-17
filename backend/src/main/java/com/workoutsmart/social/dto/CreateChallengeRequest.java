package com.workoutsmart.social.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateChallengeRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 30) String goalType,
        @Min(1) int durationDays,
        LocalDate startDate) {
}
