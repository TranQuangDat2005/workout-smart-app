package com.workoutsmart.social.dto;

import java.time.Instant;

public record FeedItemResponse(Long id, Long friendId, String displayName, String actionType, String detailsJson, Instant createdAt) {
}
