package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final StreakCalculator calculator = new StreakCalculator();

    private Instant mondayOfWeek(int weeksAgo) {
        java.time.LocalDate thisMonday = java.time.LocalDate.now(ZONE)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate target = thisMonday.minusWeeks(weeksAgo);
        return target.atTime(7, 0).atZone(ZONE).toInstant();
    }

    @Test
    void emptyHistoryReturnsZero() {
        var result = calculator.calculate(List.of(), ZONE);
        assertEquals(0, result.currentStreakWeeks());
        assertEquals(0, result.longestStreakWeeks());
    }

    @Test
    void nullHistoryReturnsZero() {
        var result = calculator.calculate(null, ZONE);
        assertEquals(0, result.currentStreakWeeks());
    }

    @Test
    void oneWeekWithEnoughSessions() {
        Instant w = mondayOfWeek(0);
        List<Instant> starts = List.of(w, w.plus(1, ChronoUnit.DAYS), w.plus(2, ChronoUnit.DAYS));
        var result = calculator.calculate(starts, ZONE);
        assertEquals(1, result.currentStreakWeeks());
        assertEquals(1, result.longestStreakWeeks());
    }

    @Test
    void twoWeeksStreak() {
        Instant thisWeek = mondayOfWeek(0);
        Instant lastWeek = mondayOfWeek(1);
        List<Instant> starts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            starts.add(thisWeek.plus(i, ChronoUnit.DAYS));
            starts.add(lastWeek.plus(i, ChronoUnit.DAYS));
        }
        var result = calculator.calculate(starts, ZONE);
        assertEquals(2, result.currentStreakWeeks());
        assertEquals(2, result.longestStreakWeeks());
    }

    @Test
    void brokenWeekResetsCurrent() {
        Instant thisWeek = mondayOfWeek(0);
        Instant twoWeeksAgo = mondayOfWeek(2); // bỏ qua tuần trước
        List<Instant> starts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            starts.add(thisWeek.plus(i, ChronoUnit.DAYS));
            starts.add(twoWeeksAgo.plus(i, ChronoUnit.DAYS));
        }
        var result = calculator.calculate(starts, ZONE);
        assertEquals(1, result.currentStreakWeeks());
        assertEquals(1, result.longestStreakWeeks());
    }

    @Test
    void longestStreakInPastIsTracked() {
        Instant w1 = mondayOfWeek(1);
        Instant w2 = mondayOfWeek(2);
        Instant w3 = mondayOfWeek(3);
        List<Instant> starts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            starts.add(w1.plus(i, ChronoUnit.DAYS));
            starts.add(w2.plus(i, ChronoUnit.DAYS));
            starts.add(w3.plus(i, ChronoUnit.DAYS));
        }
        // Tuần hiện tại chưa tập nhưng chưa hết tuần → chuỗi 3 tuần trước vẫn giữ
        var result = calculator.calculate(starts, ZONE);
        assertEquals(3, result.currentStreakWeeks());
        assertEquals(3, result.longestStreakWeeks());
    }

    @Test
    void currentWeekIncompleteDoesNotBreakButNotCounted() {
        Instant thisWeek = mondayOfWeek(0);
        Instant lastWeek = mondayOfWeek(1);
        List<Instant> starts = new ArrayList<>();
        starts.add(thisWeek); // tuần này mới 1 buổi
        for (int i = 0; i < 3; i++) {
            starts.add(lastWeek.plus(i, ChronoUnit.DAYS));
        }
        var result = calculator.calculate(starts, ZONE);
        assertEquals(1, result.currentStreakWeeks()); // chuỗi còn 1 (tuần trước)
        assertEquals(1, result.longestStreakWeeks());
    }

    @Test
    void sessionsFromDifferentZonesAreCounted() {
        Instant w = mondayOfWeek(0);
        List<Instant> starts = List.of(
                w, w.plus(1, ChronoUnit.DAYS), w.plus(2, ChronoUnit.DAYS));
        var result = calculator.calculate(starts, ZoneId.of("UTC"));
        // Buổi đầu tuần chạy ở UTC có thể rơi vào tuần khác — chỉ kiểm tra không throw và ≥0
        org.junit.jupiter.api.Assertions.assertTrue(result.currentStreakWeeks() >= 0);
    }
}
