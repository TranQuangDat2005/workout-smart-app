package com.workoutsmart.social.dto;

import java.time.LocalDate;

public record UserSearchResponse(Long id, String displayName, String avatarUrl, String email) {
}
