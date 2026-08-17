# Implementation Plan: Dinh dưỡng & Calories (002-nutrition-tracking)

**Branch**: `feature/002-nutrition-tracking` | **Date**: 2026-08-17 | **Spec**: [spec.md](spec.md)

## Summary

Kho thực phẩm (chung + cá nhân), ghi nhận bữa ăn nhiều món với tính calo/macro tự động, so sánh mục tiêu TDEE (Mifflin-St Jeor theo goal cutting/bulking), chỉ số cơ thể đồng bộ cân nặng, retention job gộp dữ liệu cũ >14 ngày. Package `com.workoutsmart.nutrition`, migration V6 (đã tạo ở core-foundation).

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| Layered + Bean Validation + status chuẩn | ✅ (400/403/404/409/422) |
| Không raw SQL | ✅ |
| Test ≥80% | ✅ TdeeCalculator 100%, NutritionService ~90% |
| Invariant: TDEE đã chốt Mifflin-St Jeor | ✅ |
| Retention đúng spec (2 tuần chi tiết) | ✅ |

**GATE: PASS**
