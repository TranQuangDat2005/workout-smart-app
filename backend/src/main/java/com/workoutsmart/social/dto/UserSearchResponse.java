package com.workoutsmart.social.dto;

public record UserSearchResponse(Long id, String displayName, String avatarUrl, String email,
                                 String relationshipStatus, Long friendshipId) {
}
