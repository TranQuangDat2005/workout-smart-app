package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Streak — định nghĩa DUY NHẤT: chuỗi tuần liên tiếp đạt ≥3 buổi completed (constitution §4). */
class StreakCalculatorTest {

    private final StreakCalculator calculator = new StreakCalculator();

    /** n buổi trong tuần cách đây weeksAgo, rải vào các ngày day 0..n-1 trong tuần. */
    private List<Instant> sessions(int weeksAgo, int count) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(weeksAgo);
        List<Instant> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(monday.plusDays(Math.min(i, 6)).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return result;
    }

    @Test
    void emptyReturnsZero() {
        var res = calculator.calculate(List.of(), ZoneId.systemDefault());
        assertEquals(0, res.currentStreakWeeks());
        assertEquals(0, res.longestStreakWeeks());
        assertNull(res.streakStartWeek());
    }

    @Test
    void threeThisWeekGivesOne() {
        var res = calculator.calculate(sessions(0, 3), ZoneId.systemDefault());
        assertEquals(1, res.currentStreakWeeks());
        assertEquals(1, res.longestStreakWeeks());
        LocalDate thisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertEquals(thisWeek, res.streakStartWeek());
    }

    @Test
    void incompleteCurrentWeekCountsFromLastWeek() {
        // Tuần này 2 buổi (chưa đủ, tuần chưa kết thúc) + tuần trước 3 buổi → streak = 1
        List<Instant> starts = new ArrayList<>(sessions(0, 2));
        starts.addAll(sessions(1, 3));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertEquals(1, res.currentStreakWeeks());
        LocalDate lastWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        assertEquals(lastWeek, res.streakStartWeek());
    }

    @Test
    void consecutiveWeeksCount() {
        List<Instant> starts = new ArrayList<>(sessions(0, 3));
        starts.addAll(sessions(1, 3));
        starts.addAll(sessions(2, 3));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertEquals(3, res.currentStreakWeeks());
        assertEquals(3, res.longestStreakWeeks());
        // Chuỗi bắt đầu từ 3 tuần trước
        LocalDate threeWeeksAgo = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2);
        assertEquals(threeWeeksAgo, res.streakStartWeek());
    }

    @Test
    void gapResetsCurrentButKeepsLongest() {
        // Tuần -1 và -3 đạt 3 buổi, tuần -2 trống → chuỗi hiện tại 1, kỷ lục 1
        List<Instant> starts = new ArrayList<>(sessions(1, 3));
        starts.addAll(sessions(3, 3));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertEquals(1, res.currentStreakWeeks());
        assertEquals(1, res.longestStreakWeeks());
        LocalDate oneWeekAgo = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        assertEquals(oneWeekAgo, res.streakStartWeek());
    }

    @Test
    void longestCanBeEntirelyInPast() {
        // Tuần -2, -3, -4 đều đạt 3 buổi; tuần này + tuần trước trống → current 0, longest 3
        List<Instant> starts = new ArrayList<>(sessions(2, 3));
        starts.addAll(sessions(3, 3));
        starts.addAll(sessions(4, 3));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertEquals(0, res.currentStreakWeeks());
        assertEquals(3, res.longestStreakWeeks());
        assertNull(res.streakStartWeek());
    }

    @Test
    void fourOrMoreStillCountsOnePerWeek() {
        // 5 buổi cùng tuần vẫn chỉ tính 1 tuần
        var res = calculator.calculate(sessions(0, 5), ZoneId.systemDefault());
        assertEquals(1, res.currentStreakWeeks());
    }

    // ---------- US3: streakStartWeek, >1000 sessions, current week edge cases ----------

    @Test
    void noCap_thousandPlusSessions() {
        // 1500 sessions phân bổ đều 3/tuần trong 500 tuần → streak = 500
        List<Instant> starts = new ArrayList<>();
        IntStream.range(0, 500).forEach(w -> starts.addAll(sessions(w, 3)));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertEquals(500, res.currentStreakWeeks());
        assertEquals(500, res.longestStreakWeeks());
        assertNotNull(res.streakStartWeek());
    }

    @Test
    void currentWeekNotYetThree_doesNotBreakStreak() {
        // Tuần này 1 buổi + tuần trước 3 + tuần trước nữa 3 → streak = 2 (chưa reset)
        List<Instant> starts = new ArrayList<>(sessions(0, 1));
        starts.addAll(sessions(1, 3));
        starts.addAll(sessions(2, 3));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertEquals(2, res.currentStreakWeeks());
        LocalDate twoWeeksAgo = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2);
        assertEquals(twoWeeksAgo, res.streakStartWeek());
    }

    @Test
    void streakStartWeek_isMonday() {
        // 3 tuần liên tiếp → streakStartWeek phải là thứ 2
        List<Instant> starts = new ArrayList<>(sessions(0, 3));
        starts.addAll(sessions(1, 3));
        var res = calculator.calculate(starts, ZoneId.systemDefault());
        assertNotNull(res.streakStartWeek());
        assertEquals(DayOfWeek.MONDAY, res.streakStartWeek().getDayOfWeek());
    }
}
