package com.workoutsmart.feed.dto;

import java.util.List;

/** Trang feed với cursor pagination — nextCursor null nghĩa là hết dữ liệu. */
public record FeedPageResponse(List<PostResponse> items, Long nextCursor) {
}
