package com.workoutsmart.plan.dto;

import java.time.LocalDate;

/** Trigger tạo lại lộ trình thủ công — FR-002. */
public record GeneratePlanRequest(LocalDate effectiveDate) {
}
