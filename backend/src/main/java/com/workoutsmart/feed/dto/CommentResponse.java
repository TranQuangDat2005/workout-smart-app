package com.workoutsmart.feed.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long userId,
        String displayName,
        String avatarUrl,
        String content,
        Instant createdAt) {
}
