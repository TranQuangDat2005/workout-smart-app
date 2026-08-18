# Tasks: Chuẩn hoá UI/UX & Hardening Acceptance

**Input**: Design documents from `/specs/010-uiux-polish/`

**Prerequisites**: plan.md, spec.md

**Tests**: Chạy `npm run build` + `npm run lint` sau mỗi phase; test thủ công bằng code review.

## Phase 1: Setup (Hạ tầng dùng chung)

- [X] T001 Tạo `web/src/services/http.ts` — axios client dùng chung với request (gắn token) + response interceptor (silent refresh khi 401, kick về login khi refresh hết hạn)
- [X] T002 Refactor 8 service (`authApi`, `profileApi`, `nutritionApi`, `trackingApi`, `socialApi`, `adminApi`, `planApi`, `statsApi`) import `http.ts` thay vì tự tạo client

## Phase 2: Critical UX

- [X] T003 Thêm Rest Timer vào `web/src/pages/tracking/WorkoutPage.tsx` (đếm ngược + cảnh báo 5 giây cuối)
- [X] T004 Tạo `web/src/components/Modal.tsx` (dialog design-system, đóng bằng overlay/ESC)
- [X] T005 Thêm dialog đổi mục tiêu (2 lựa chọn) vào `web/src/pages/profile/ProfilePage.tsx`
- [X] T006 Thêm xác nhận mật khẩu khi xóa tài khoản vào `web/src/pages/profile/ProfilePage.tsx`
- [X] T007 Thêm responsive (breakpoint + bottom nav/collapse) vào `web/src/index.css` + `web/src/components/AppShell.tsx`
- [X] T008 Thêm `:focus-visible` và nhãn OTP (`aria-label`) vào `index.css` + `web/src/pages/auth/VerifyOtpPage.tsx`

## Phase 3: High UX

- [X] T009 Tạo `web/src/services/labels.ts` và áp dụng nhãn tiếng Việt cho trạng thái (admin user/exercise, leaderboard challenge)
- [X] T010 Sửa `web/src/pages/nutrition/NutritionPage.tsx` để danh sách "Món đã chọn" hiển thị tên món + calo/macro
- [X] T011 Sửa `web/src/pages/social/LeaderboardPage.tsx` thêm 2 phạm vi + highlight vị trí cá nhân
- [X] T012 Sửa `web/src/pages/profile/WorkoutHistoryPage.tsx` thêm phân trang
- [X] T013 Sửa `web/src/pages/admin/AdminUsersPage.tsx` thêm chi tiết user + thay `prompt` bằng `Modal`
- [X] T014 Sửa `web/src/pages/admin/AdminExercisesPage.tsx` thêm thumbnail/instructions + chế độ sửa
- [X] T015 Thay `window.confirm` bằng `Modal` tại `FoodLibraryPage`, `FriendsPage`

## Phase 4: Polish & Validation

- [X] T016 Chạy `npm run build` và `npm run lint`, sửa lỗi phát sinh
- [X] T017 Cập nhật ghi chú hoàn tất và đánh dấu tasks

## Ghi chú

- Đã bổ sung backend: `GET /api/v1/admin/exercises/{id}` (chi tiết bài tập cho form sửa) và `GET /api/v1/leaderboard/friends` (xếp hạng nhóm bạn bè).
- Frontend đã nối: `adminApi.getExercise/getUserDetail`, `socialApi.friendsLeaderboard`; AdminExercisesPage có chế độ sửa, AdminUsersPage tải chi tiết + lịch sử, LeaderboardPage tải theo phạm vi.

## Dependencies & Execution Order

- Phase 1 → Phase 2 (critical) → Phase 3 (high) → Phase 4.
- T004 phải xong trước T005/T006/T013/T015 (phụ thuộc Modal).
- T001 phải xong trước T002.
