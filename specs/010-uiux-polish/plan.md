# Implementation Plan: Chuẩn hoá UI/UX & Hardening Acceptance

**Branch**: `010-uiux-polish` | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-uiux-polish/spec.md`

## Summary

Đóng các gap UI/UX và hạ tầng client phát hiện trong đợt QA acceptance: silent refresh chưa áp dụng cho mọi request, giao diện chưa responsive, thiếu Rest Timer, luồng đổi mục tiêu/xóa tài khoản chưa đủ an toàn, thiếu accessibility/localization, và một số luồng phụ chưa hoàn thiện.

## Technical Context

**Language/Version**: TypeScript 5.x (strict) + React 18 + Vite 5

**Primary Dependencies**: react, react-dom, react-router-dom, axios

**Storage**: Không thay đổi schema; dùng dữ liệu từ REST API hiện có.

**Testing**: `npm run build`, `npm run lint` (web); không thêm test mới trừ khi cần cho thành phần dialog/timer (tuỳ chọn).

**Target Platform**: Web (desktop/tablet/mobile responsive)

**Project Type**: web-app (SPA)

**Performance Goals**: Rest Timer cập nhật mỗi giây không gây re-render toàn trang; dialog mở/tắt mượt.

**Constraints**: Tuân thủ `DESIGN.md` (theme tối, pill button, green accent); không thay đổi invariant trong constitution.

**Scale/Scope**: ~30 file `web/src`, tập trung ở `services/`, `components/`, `pages/`, `index.css`.

## Constitution Check

- [x] Không thêm công nghệ ngoài stack (React/TS/axios).
- [x] Không thay đổi invariant (streak, retention, sync, account).
- [x] Không commit secrets; không đọc `.env`.
- [x] FRs EARS tiếng Việt giữ nguyên trong spec.
- [x] Thay đổi ở tầng web, không phá backend migration.

## Project Structure

### Documentation (this feature)

```text
specs/010-uiux-polish/
├── plan.md
├── spec.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
web/src/
├── services/
│   ├── http.ts            # HTTP client dùng chung (silent refresh)
│   ├── authApi.ts         # refactor dùng http.ts
│   ├── profileApi.ts      # refactor
│   ├── nutritionApi.ts    # refactor
│   ├── trackingApi.ts     # refactor
│   ├── socialApi.ts       # refactor
│   ├── adminApi.ts        # refactor
│   ├── planApi.ts         # refactor
│   ├── statsApi.ts        # refactor
│   └── labels.ts          # map enum -> nhãn tiếng Việt
├── components/
│   ├── Modal.tsx          # dialog design-system
│   ├── AppShell.tsx       # responsive sidebar/bottom nav
│   ├── Button.tsx
│   └── TextField.tsx
├── pages/
│   ├── tracking/WorkoutPage.tsx   # Rest Timer
│   ├── profile/ProfilePage.tsx    # dialog đổi mục tiêu + xóa tài khoản
│   ├── nutrition/NutritionPage.tsx# tên món + macro
│   ├── social/LeaderboardPage.tsx # scope + highlight
│   ├── profile/WorkoutHistoryPage.tsx # phân trang
│   └── admin/*.tsx                # chi tiết user + form bài tập
└── index.css               # responsive + focus-visible
```

**Structure Decision**: Giữ nguyên cấu trúc hiện tại; thêm `http.ts` và `labels.ts` vào `services/`, thêm `Modal.tsx` vào `components/`.
