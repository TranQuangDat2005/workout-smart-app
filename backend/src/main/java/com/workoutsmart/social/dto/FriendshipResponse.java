package com.workoutsmart.social.dto;

public record FriendshipResponse(Long id, Long friendId, String displayName, String avatarUrl, String status) {
}
