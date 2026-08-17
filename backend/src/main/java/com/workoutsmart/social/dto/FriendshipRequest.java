package com.workoutsmart.social.dto;

import jakarta.validation.constraints.NotNull;

public record FriendshipRequest(@NotNull Long targetUserId) {
}
