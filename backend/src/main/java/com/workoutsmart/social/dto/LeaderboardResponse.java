package com.workoutsmart.social.dto;

public record LeaderboardResponse(int rank, Long userId, String displayName, int currentStreakWeeks, int longestStreakWeeks, Integer viewerRank) {
}
