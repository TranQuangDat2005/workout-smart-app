package com.workoutsmart.nutrition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 019c: nhập hàng loạt thực phẩm từ CSV. */
public record ImportFoodRequest(
        @NotEmpty @Size(max = 500)
        List<@Valid ImportFoodRow> rows) {
}
