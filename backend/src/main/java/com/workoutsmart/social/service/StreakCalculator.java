package com.workoutsmart.social.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Streak — định nghĩa DUY NHẤT toàn hệ thống (constitution §4):
 * chuỗi TUẦN liên tiếp đạt ≥3 buổi tập completed; bỏ 1 tuần → reset.
 */
public class StreakCalculator {

    public static final int REQUIRED_SESSIONS_PER_WEEK = 3;

    public record StreakResult(int currentStreakWeeks, int longestStreakWeeks) {
    }

    /**
     * @param completedStarts thời điểm bắt đầu của các buổi tập status=completed
     * @param zone            múi giờ địa phương của user (mặc định hệ thống)
     */
    public StreakResult calculate(List<Instant> completedStarts, ZoneId zone) {
        if (completedStarts == null || completedStarts.isEmpty()) {
            return new StreakResult(0, 0);
        }
        // Gom theo tuần (thứ 2 đầu tuần)
        var sessionsByWeek = new java.util.HashMap<LocalDate, Integer>();
        for (Instant start : completedStarts) {
            LocalDate date = start.atZone(zone).toLocalDate();
            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            sessionsByWeek.merge(weekStart, 1, Integer::sum);
        }

        LocalDate thisWeek = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Tuần hiện tại: nếu đã đạt 3 buổi → tính vào chuỗi; nếu chưa → chưa đứt (tuần chưa kết thúc),
        // đếm ngược từ tuần trước.
        LocalDate cursor = thisWeek;
        if (sessionsByWeek.getOrDefault(thisWeek, 0) < REQUIRED_SESSIONS_PER_WEEK) {
            cursor = thisWeek.minusWeeks(1);
        }
        int current = 0;
        while (sessionsByWeek.getOrDefault(cursor, 0) >= REQUIRED_SESSIONS_PER_WEEK) {
            current++;
            cursor = cursor.minusWeeks(1);
        }

        int longest = current;
        // Chuỗi dài nhất có thể nằm hoàn toàn trong quá khứ
        for (LocalDate week : sessionsByWeek.keySet().stream().sorted().toList()) {
            int run = 0;
            LocalDate w = week;
            while (sessionsByWeek.getOrDefault(w, 0) >= REQUIRED_SESSIONS_PER_WEEK) {
                run++;
                w = w.plusWeeks(1);
            }
            longest = Math.max(longest, run);
        }
        return new StreakResult(current, longest);
    }
}
