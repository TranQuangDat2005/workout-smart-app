package com.workoutsmart.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(max = 1000, message = "Bình luận tối đa 1000 ký tự")
        String content) {
}
