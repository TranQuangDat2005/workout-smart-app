package com.workoutsmart.plan.dto;

import java.util.List;

/** Kết quả sau khi lưu mục tiêu + trigger tạo plan — FR-001, FR-005. */
public record GoalSetupResponse(
        String goalType,
        String fitnessLevel,
        List<String> equipment,
        Long planId,
        String warning) {
}
