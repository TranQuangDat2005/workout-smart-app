package com.workoutsmart.profile.dto;

/** Phản hồi cập nhật hồ sơ — goalChanged=true khi mục tiêu bị thay đổi. */
public record UpdateProfileResponse(ProfileResponse profile, boolean goalChanged) {
}
