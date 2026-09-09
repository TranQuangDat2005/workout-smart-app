# Phân công Domain và Định hướng thực hiện

**Ngày:** 2026-08-26  
**PM:** Người quản lý dự án  
**Mục tiêu:** Làm rõ spec, xác định yêu cầu chính xác, kiểm tra và sửa code theo từng domain.

## 1. Mô hình làm việc

Dự án có 4 domain nghiệp vụ chính. PM không trực tiếp sở hữu domain mà chịu trách nhiệm điều phối, ưu tiên công việc, xử lý blocker và quyết định nghiệp vụ.

General Spec do PM quản lý; các domain owner đề xuất thay đổi và phối hợp review để duy trì tính nhất quán toàn hệ thống.

Mỗi domain owner chịu trách nhiệm xuyên suốt:

1. Đọc General Spec và các feature spec được giao.
2. Đối chiếu spec với code, API contract, migration và test hiện tại.
3. Ghi nhận điểm thiếu, mâu thuẫn hoặc chưa rõ.
4. Đề xuất câu trả lời cho requirement chưa rõ để PM quyết định.
5. Sửa spec/tasks trước hoặc cùng lúc với code khi phát hiện sai lệch.
6. Viết hoặc cập nhật test hồi quy cho lỗi đã sửa.
7. Báo cáo kết quả và dependency cho PM.

## 2. Phân công theo 4 domain chính

Nguyên tắc mới: mỗi người sở hữu một **bounded context**, spec được chia theo nghiệp vụ; phần Admin được tách theo đối tượng quản lý thay vì gom toàn bộ vào một người.

PM điều phối chung. Bốn domain owner chịu trách nhiệm làm rõ spec, đối chiếu code và đề xuất sửa trong phạm vi của mình. Các spec dùng chung được giao cho một domain chính, các domain còn lại chỉ cung cấp contract và review.

### Domain 1: Tập luyện

**Spec sở hữu/chủ trì**

- `specs/001-profile-history/`
- `specs/008-workout-plan/`
- `specs/009-workout-tracking/`
- `specs/010-uiux-polish/` (phần màn hình tập luyện)
- `specs/011-custom-exercise/`
- `specs/012-exercise-library-filters/`
- `specs/013-manual-workout-builder/`
- `specs/014-advanced-sets/`
- `specs/015-time-based-exercises/`
- `specs/016-workout-execution-mode/`
- `specs/017-workout-music/`

**Code chính**

- `backend/src/main/java/com/workoutsmart/plan/`
- `backend/src/main/java/com/workoutsmart/exercise/`
- `backend/src/main/java/com/workoutsmart/tracking/`
- `backend/src/main/java/com/workoutsmart/profile/` (phần workout history)
- `web/src/pages/training/`
- `web/src/pages/plan/`
- `web/src/pages/profile/WorkoutHistoryPage.tsx`
- `web/src/components/ExerciseBrowser.tsx`

**Công việc**

- Làm rõ từ goal setup → plan → snapshot → execution → history.
- Kiểm tra Rule Engine, exercise library, custom exercise, advanced set, duration, timer, media và music.
- Chốt offline sync, LWW, session expiry và workout read contract cho Domain Dinh dưỡng.
- Không sở hữu TDEE, meal data, social privacy hoặc admin policy.

### Domain 2: Dinh dưỡng và Phân tích

**Spec sở hữu/chủ trì**

- `specs/002-nutrition-tracking/`
- `specs/005-stats-reports/`
- Phần body metrics/calorie data liên quan trong `specs/001-profile-history/`

**Code chính**

- `backend/src/main/java/com/workoutsmart/nutrition/`
- `backend/src/main/java/com/workoutsmart/stats/`
- `web/src/pages/nutrition/`
- `web/src/pages/stats/`

**Công việc**

- Chốt TDEE, calorie goal, meal retention, custom food và body metrics.
- Hoàn thiện Stats Dashboard, analytics read model và plan completion/calorie contracts.
- Chỉ đọc dữ liệu workout qua contract do Domain Tập luyện cung cấp.
- Kiểm tra dữ liệu thiếu, khoảng thời gian, archived plan và trạng thái biểu đồ rỗng.

### Domain 3: Admin và Tài khoản

**Spec sở hữu/chủ trì**

- `specs/007-core-auth/`
- `specs/006-admin-management/`
- Phần account/privacy dùng chung trong `specs/001-profile-history/`

**Code chính**

- `backend/src/main/java/com/workoutsmart/auth/`
- `backend/src/main/java/com/workoutsmart/admin/`
- `web/src/pages/auth/`
- Phần quản trị trong `web/src/pages/admin/`
- `web/src/context/`
- `web/src/services/authApi.ts`
- `web/src/services/adminApi.ts`
- `web/src/services/tokenStorage.ts`

**Công việc**

- Làm rõ register, OTP, login, refresh, logout, reset password, soft-delete và restore.
- Kiểm tra role, ban/unban, token revoke, audit log và quyền Admin.
- Sở hữu account status, timezone của user và security contract dùng chung.
- Phối hợp Domain Tập luyện về session bị interrupted; phối hợp Domain Mạng xã hội về privacy/account visibility.

### Domain 4: Mạng xã hội

**Spec sở hữu/chủ trì**

- `specs/003-social-community/`
- Phần feed/community trong `specs/010-uiux-polish/`

**Code chính**

- `backend/src/main/java/com/workoutsmart/social/`
- `backend/src/main/java/com/workoutsmart/feed/`
- `web/src/pages/social/`
- `web/src/services/socialApi.ts`
- `web/src/services/feedApi.ts`

**Công việc**

- Làm rõ friendship, feed, privacy, leaderboard, streak và challenge.
- Kiểm tra media authorization của feed và visibility của user banned/deleted.
- Nhận streak/workout completion read contract từ Domain Tập luyện.
- Phối hợp Domain Admin về role/account status và audit các thao tác quản trị social.

### Phần dùng chung và quy tắc ownership

- `specs/General Spec.md` do PM quản lý; mỗi domain owner đề xuất thay đổi, PM quyết định và cập nhật bản chính.
- `specs/010-uiux-polish/` chia theo màn hình: Tập luyện và Social sở hữu phần UI của mình; shared components/API do PM điều phối.
- `specs/006-admin-management/` có một owner chính là Domain Admin; phần exercise management phối hợp Domain Tập luyện.
- Không để hai domain cùng sửa một controller/service/DTO. Nếu file chứa nhiều phần, chia ownership theo method hoặc tạo task phối hợp trước.

### Đánh giá tải sau khi gom 4 domain

| Domain | Tải | Nhận xét |
|---|---|---|
| Tập luyện | Cao nhất | Bao phủ toàn bộ workout lifecycle và nhiều state/timer; cần ưu tiên correctness, không nhận thêm Admin/Stats. |
| Dinh dưỡng và Phân tích | Cao | `005-stats-reports` còn nhiều task mở và phụ thuộc read contract từ Tập luyện. |
| Admin và Tài khoản | Cao vừa | Auth là nền tảng, Admin có rủi ro security; cần làm theo thứ tự account trước, admin sau. |
| Mạng xã hội | Trung bình | Ít task mở hơn nhưng privacy, feed media và streak cần review kỹ. |

Việc gom còn 4 domain giúp giảm số đầu mối phối hợp, nhưng Tập luyện vẫn là domain lớn nhất. PM nên chia domain này thành các workstream nội bộ: Plan/Exercise, Tracking/Execution và Sync/History; vẫn giữ một owner cuối.

## 3. Thứ tự triển khai đề xuất

### Giai đoạn 0: Chuẩn hóa môi trường và baseline

PM phối hợp kiểm tra:

- Backend có JDK và `JAVA_HOME` hợp lệ.
- Web đã cài dependency và chạy được TypeScript/Vite.
- Database/Docker có thể khởi động theo tài liệu trong `docs/`.
- Có baseline test report trước khi sửa code.

Không đánh dấu domain lỗi chỉ vì môi trường chưa chạy được. Phải phân biệt lỗi môi trường với lỗi source/test.

### Giai đoạn 1: Làm rõ yêu cầu toàn cục

PM chủ trì, bốn domain owner tham gia.

- Đọc `specs/General Spec.md` trước.
- Đánh dấu các requirement mâu thuẫn hoặc thiếu acceptance criteria.
- Chốt các vấn đề ảnh hưởng nhiều domain.
- Cập nhật status Draft/Approved/Ready dựa trên mức độ rõ ràng thực tế.

Các điểm cần ưu tiên: TDEE/calorie goal, streak, privacy, inactive exercise, soft-delete, offline sync, mobile deferred và notification/draft queue.

### Giai đoạn 2: Chốt các domain có rủi ro cao

1. Domain Dinh dưỡng và Phân tích xử lý `005-stats-reports` vì còn 16 task mở.
2. Domain Tập luyện bổ sung tasks/plan cho `011-custom-exercise`.
3. Domain Tập luyện kiểm tra runtime vì đây là luồng có nhiều dependency và timer state.
4. Domain Admin và Tài khoản, cùng Domain Mạng xã hội, kiểm tra các contract auth, ban và privacy song song.

### Giai đoạn 3: Verify và fix theo domain

Mỗi domain thực hiện theo vòng lặp:

1. Chạy test/build nhỏ nhất có thể.
2. Đối chiếu lỗi với FR hoặc acceptance scenario cụ thể.
3. Sửa root cause trong module sở hữu.
4. Bổ sung test regression.
5. Chạy lại test domain.
6. Gửi cross-domain review nếu thay đổi API/entity/behavior dùng chung.

### Giai đoạn 4: Integration review

Sau khi từng domain đạt DoD:

- Kiểm tra auth → profile → plan → tracking → stats.
- Kiểm tra admin ban → middleware → session hiện tại.
- Kiểm tra admin inactive exercise → plan/search/history.
- Kiểm tra nutrition/body metrics → stats.
- Kiểm tra web API typing → backend contract.

## 4. Bản đồ review kiến trúc theo domain

Bảng này chuyển các vấn đề đã phát hiện trong review sơ bộ thành đầu việc có owner cụ thể.

| Mức độ | Vấn đề cần review | Owner chính | Phối hợp | Kết quả cần có |
|---|---|---|---|---|
| P1 | Offline sync có thể trả sai rejected ID, chưa trả HTTP 409, chưa kiểm tra session/snapshot đầy đủ | Tập luyện | Admin và Tài khoản, PM | Chốt sync contract, test expired session, test LWW và session offline mới |
| P1 | Ban user dùng cache 60 giây và cache in-memory, chưa phù hợp yêu cầu chặn tối đa 3 giây/multi-instance | Admin và Tài khoản | Tập luyện, PM | Quyết định token revocation/status propagation; test request ngay sau ban |
| P1 | Media feed mở public, có nguy cơ bỏ qua privacy của bài viết friends/private | Mạng xã hội | Admin và Tài khoản, PM | Chốt public/private media policy; test người lạ, bạn bè và chủ sở hữu |
| P1 | Media custom exercise/user upload chưa có policy authorization rõ ràng | Tập luyện | Mạng xã hội, Admin và Tài khoản | Chốt quyền đọc media; test owner, bạn bè và user không liên quan |
| P1 | Ranh giới WorkoutSession/WorkoutSet thuộc profile nhưng nghiệp vụ thuộc tracking | Tập luyện | Dinh dưỡng và Phân tích, Admin và Tài khoản | Chốt ownership entity và hướng đọc dữ liệu lịch sử/thống kê |
| P1 | Timezone dùng `systemDefault()` thay vì timezone của user | Tập luyện | Admin và Tài khoản, PM | Chốt nguồn timezone; test session gần nửa đêm, expiry và sync |
| P2 | Stats tính trực tiếp mỗi request, calories dùng hằng số 5 kcal/phút, plan completion có thể sai tỷ lệ | Dinh dưỡng và Phân tích | Tập luyện, PM | Chốt công thức và semantics; test dữ liệu thiếu, plan archive, 7/30/90 ngày |
| P2 | Import exercise chưa có unique constraint bảo vệ duplicate khi chạy đồng thời | Tập luyện | Admin và Tài khoản, PM | Chốt normalized key và database constraint/upsert transaction |
| P2 | Docker dùng PostgreSQL 17, expose database trực tiếp và có default password fallback | PM/Governance | Tất cả domain | Chốt target môi trường; cập nhật compose/deployment checklist |

### Quy tắc xử lý finding

- Owner chính chịu trách nhiệm tái hiện, mô tả impact và đề xuất hướng xử lý.
- Nếu finding làm thay đổi behavior hoặc public contract, PM phải cập nhật/đối chiếu General Spec trước.
- PM quyết định các điểm nghiệp vụ chưa rõ; domain owner không tự đoán.
- Chỉ sau khi requirement được chốt mới tạo task sửa code và test regression.
- Finding không sửa ngay phải có lý do, mức chấp nhận rủi ro và thời điểm xem lại.

## 5. Bản đồ review các luồng chính

### Luồng A: Đăng ký, đăng nhập và hồ sơ

`Web Auth → Auth API → JWT/Refresh Token → Security Filter → Profile API`

- **Admin và Tài khoản:** review OTP, login, refresh, logout, reset password, soft-delete/restore, profile và ban/unban.
- **Mạng xã hội:** xác nhận visibility của user trong social/feed.
- **PM:** chốt trạng thái account, thời điểm token bị vô hiệu hóa và error contract.
- **Điểm kiểm tra:** banned user dùng access token cũ, refresh token cũ, request sau khi offline, user deleted và restore trong 30 ngày.

### Luồng B: Goal setup và tạo Workout Plan

`Profile/Goal Setup → Rule Engine → Exercise Library → Workout Plan → Plan UI`

- **Tập luyện:** review rule engine, taxonomy, active/inactive exercise, custom exercise và manual builder.
- **Admin và Tài khoản:** xác nhận dữ liệu goal/fitness/equipment và account context.
- **Tập luyện:** xác nhận snapshot plan khi bắt đầu session.
- **Điểm kiểm tra:** đổi plan ngay/từ ngày mai, không có exercise phù hợp, exercise bị inactive, custom exercise bị xóa và target từng set.

### Luồng C: Bắt đầu buổi tập và ghi set

`Active Plan → Start Session → Session Snapshot → Execution UI → Record Set → Rest/Complete`

- **Tập luyện:** owner của runtime, timer, focus interruption, set type, duration, undo và snapshot.
- **Admin và Tài khoản:** xác nhận session bị interrupted khi user bị ban.
- **Điểm kiểm tra:** reload giữa buổi, deadline timer, drop-set không nghỉ, duration tự ghi, double-tap và session expired.

### Luồng D: Offline workout và đồng bộ

`Local Queue → Reconnect → Sync API → Validate Session → LWW Upsert → Sync Result`

- **Tập luyện:** owner của API sync và tính đúng dữ liệu set.
- **Admin và Tài khoản:** xác nhận account/timezone context.
- **PM:** chốt LWW key, rejected item contract và timezone.
- **Điểm kiểm tra:** session chưa tồn tại, session đã expired, cùng set từ hai thiết bị, retry rejected item, user bị ban khi offline.

### Luồng E: Nutrition và Stats

`Body Metrics/Profile → TDEE → Meal Log → Retention/Summary → Stats Dashboard`

- **Dinh dưỡng và Phân tích:** owner của dữ liệu nutrition, TDEE, retention và stats.
- **Admin và Tài khoản:** xác nhận profile và body metric ownership.
- **Tập luyện:** cung cấp workout volume, duration và completed session.
- **PM:** chốt công thức calorie goal và semantics của khoảng thời gian.
- **Điểm kiểm tra:** thiếu dữ liệu TDEE, nhiều món/bữa, không cho xóa meal, dữ liệu cũ hơn 14 ngày, archived plan và biểu đồ rỗng.

### Luồng F: Social, Feed và Admin Exercise

`User/Admin Auth → Privacy/Role Check → Social/Feed/Admin API → DB/Media Storage`

- **Mạng xã hội:** owner của friendship, feed, leaderboard, challenge và feed privacy.
- **Tập luyện:** owner của exercise operations, replacement và draft queue.
- **Admin và Tài khoản:** xác nhận role/account status và middleware.
- **Điểm kiểm tra:** privacy public/friends/private, user banned/deleted, concurrent friend request, import duplicate, media authorization và inactive exercise.

## 6. Trình tự review thực tế

1. PM công bố danh sách requirement cần quyết định, sau khi nhận tổng hợp từ các domain.
2. Mỗi owner review luồng của domain mình và ghi finding theo bảng trên.
3. PM chốt các câu hỏi nghiệp vụ theo nhóm, không chốt rời rạc từng file.
4. Owner cập nhật `spec.md`, `plan.md`, `tasks.md` tương ứng.
5. Owner mới bắt đầu sửa code và viết test regression.
6. PM review cross-domain, sau đó nghiệm thu theo acceptance criteria.

## 7. Definition of Done cho một domain

Domain chỉ được đánh dấu hoàn thành khi:

- Spec không còn requirement quan trọng bị hiểu theo nhiều cách.
- Có `plan.md` và `tasks.md` phản ánh đúng tình trạng code.
- Backend compile/test liên quan pass.
- Frontend typecheck/build/test liên quan pass.
- API contract, database model và UI behavior khớp nhau.
- Test hồi quy bao phủ các lỗi đã sửa.
- Dependency với domain khác đã được xác nhận.
- Không có TODO quan trọng bị bỏ qua mà không ghi blocker.

## 8. Quy tắc phối hợp và ownership

- Chỉ domain owner sửa code thuộc module của mình, trừ khi đã thống nhất với owner liên quan.
- Với module dùng chung như `admin/`, ownership được chia theo controller/service method và DTO; không tự ý chỉnh phần của owner khác.
- PM quyết định trade-off nghiệp vụ và review các thay đổi cross-domain.
- Không sửa trực tiếp `General Spec` để che một lỗi code. Nếu code chưa đúng, phải ghi rõ gap và sửa code hoặc cập nhật quyết định của PM.
- Mọi thay đổi public API phải cập nhật DTO, contract, frontend client và test liên quan.
- Không xóa migration hiện có.
- Không đọc hoặc commit `.env`, secrets, credentials hay token.
- Mỗi thay đổi nên nhỏ, có test và có mô tả lỗi/requirement được xử lý.

## 9. Mẫu báo cáo domain hàng ngày

```text
Domain:
Owner:
Ngày:

1. Spec đã đọc/đã làm rõ:
2. Requirement còn cần PM quyết định:
3. Lỗi code/test đã phát hiện:
4. Lỗi đã sửa:
5. Test/build đã chạy và kết quả:
6. File đã thay đổi:
7. Dependency hoặc blocker:
8. Đề xuất bước tiếp theo:

Status: IN_PROGRESS | BLOCKED | READY_FOR_REVIEW | DONE
```

## 10. Việc PM cần làm ngay

1. Chỉ định tên thật cho 6 vai trò và xác nhận owner từng domain.
2. Gỡ blocker môi trường JDK/`JAVA_HOME` và web dependencies.
3. Mở clarification log trong General Spec.
4. Giao Domain Dinh dưỡng và Phân tích xử lý `005-stats-reports` trước.
5. Giao Domain Tập luyện tạo tasks/plan rõ cho `011-custom-exercise`.
6. Yêu cầu tất cả owner chạy baseline test trước khi sửa.
7. Tổ chức review ngắn sau mỗi domain, không đợi đến cuối sprint mới tích hợp.

Tài liệu nền tảng:

- [General Spec](../specs/General%20Spec.md)
- [AGENTS.md](../AGENTS.md)
- [Domain Ownership Plan](../plans/20260826-domain-ownership-spec-clarification/plan.md)
