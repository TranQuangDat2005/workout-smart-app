package com.workoutsmart.feed.dto;

import java.time.Instant;
import java.util.List;

/**
 * Bài đăng cộng đồng. mediaUrl là key trên SeaweedFS
 * (client gọi GET /api/v1/feed/media/{key} để lấy nội dung).
 */
public record PostResponse(
        Long id,
        Long userId,
        String displayName,
        String avatarUrl,
        String content,
        String mediaType,
        String mediaUrl,
        String gifUrl,
        String audience,
        Instant createdAt,
        long likeCount,
        boolean likedByMe,
        List<CommentResponse> comments) {
}
