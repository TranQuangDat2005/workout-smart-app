package com.workoutsmart.social.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ChallengeResponse(
        Long id,
        String name,
        String goalType,
        int durationDays,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        boolean joined,
        int participantCount,
        Instant completedAt,
        Integer finalRank) {
}
