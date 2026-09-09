package com.workoutsmart.social.dto;

import java.time.LocalDate;

public record ChallengeResultResponse(
        Long userId,
        String displayName,
        Integer finalRank,
        int currentStreakWeeks,
        LocalDate streakStartWeek) {
}
