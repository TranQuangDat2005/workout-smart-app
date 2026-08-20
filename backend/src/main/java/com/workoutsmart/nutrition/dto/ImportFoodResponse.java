package com.workoutsmart.nutrition.dto;

import java.util.List;

/** 019c: kết quả nhập CSV — số món đã nhập + danh sách lỗi từng dòng. */
public record ImportFoodResponse(
        int imported,
        List<String> errors) {
}
