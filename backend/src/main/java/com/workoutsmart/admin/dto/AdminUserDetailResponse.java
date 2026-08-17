package com.workoutsmart.admin.dto;

import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.WorkoutSessionResponse;
import java.util.List;

/** Chi tiết user cho Admin — FR-003 (006): hồ sơ + lịch sử tập gần đây. */
public record AdminUserDetailResponse(
        ProfileResponse profile,
        List<WorkoutSessionResponse> recentSessions) {
}
