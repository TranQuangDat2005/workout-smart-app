# Brainstorm Report: Lọc thư viện bài tập + Tự xây lộ trình

**Ngày:** 2026-08-18  
**Trạng thái:** Đã chốt hướng (chờ SpecKit specify)  
**Stack thực tế:** React 18 + Vite SPA (không phải Next.js), Spring Boot 3.3, PostgreSQL 18, JWT  
**Nguồn taxonomy:** `exercises-dataset/index.html` + `exercises-dataset/data/exercises.json`

---

## 1. Problem-first

### 1.1 Solution-jumping diagnosis

Tín hiệu ban đầu: “copy cách filter của `index.html` (trừ target muscle) sang thư viện và tự xây”.  
Đằng sau đó là hai nỗi đau khác nhau bị gói chung một câu:

1. Kho 1324 bài không lọc được đúng taxonomy dataset (category 10 + equipment 28), nên tìm bài chậm / sai.
2. Lộ trình Rule Engine là mẫu tuần cứng; user muốn tự lắp bài (kéo-thả, set/rep) và sửa một buổi lặp mà **không làm hỏng buổi đã tập / mẫu tuần còn lại**.

### 1.2 Underlying problem

Người tập cần (a) tìm bài theo đúng nhãn kho dữ liệu và (b) chỉnh lịch tuần như một template do chính họ sở hữu, trong khi mỗi lần bấm “Bắt đầu tập” phải là bản sao độc lập để lịch sử không bị rewrite.

### 1.3 Assumption challenges

| Giả định | Rủi ro nếu sai | Cách kiểm |
|---|---|---|
| “Tự xây” = modal tạo 1 bài custom | Spec sai entity (Exercise vs Plan/Session) | Đã bác: tự xây = builder lộ trình |
| Cần snapshot mọi ngày trên lịch | Schema phình, timezone, ngày nghỉ mồ côi | Không làm; snapshot lúc start |
| Filter thư viện cần Target Muscle | UI nhiễu, trùng gần với `target` 19 giá trị | Loại trừ theo yêu cầu user |
| 28 equipment khớp Rule Engine | Bài custom `cable` không vào auto-plan | Giữ Goal Setup 5 dụng cụ; để vòng sau |
| `UNIQUE (session_id, set_number)` đủ cho nhiều bài/buổi | Set của bài A đè bài B | Phải đổi uniqueness khi làm builder |

### 1.4 Problem statement

- **Users:** người tập đã có plan active; người tự tạo bài custom.  
- **Struggle:** lọc thư viện thiếu category/equipment dataset; không sửa được template tuần; sợ sửa hôm nay làm hỏng chuỗi lặp.  
- **Cause:** UI lọc dùng 6 nhóm cơ + 5 dụng cụ; plan là template `day_of_week` không có editor; tracking ghi set theo session chứ không snapshot plan.  
- **Consequence:** không tìm được bài máy/cáp; không dám chỉnh lịch; tracking không phản ánh “buổi hôm nay khác template”.  
- **Success:** lọc được như HTML (trừ target); kéo-thả sửa template tuần; bấm Bắt đầu → snapshot; sửa template sau đó không đụng buổi đã start.

### 1.5 Three alternative framings

- **Frame A — Chỉ taxonomy thư viện:** copy chip Category/Equipment. Không giải “tự xây”.  
- **Frame B — Template tuần + snapshot lúc start (chốt):** plan là mẫu do user sửa; session copy lúc bắt đầu.  
- **Frame C — Materialize từng ngày lịch:** mỗi occurrence một bản ghi. Quá nặng so với gym app.

### 1.6 Evidence status

**Medium.** Có dataset 1324 bài + `index.html` thật; schema plan/session đã tách; tracking hiện tại chưa snapshot. Chưa có telemetry user, nhưng khớp mô hình Strong/Hevy.

### 1.7 Validation plan

- Filter: search `cable` + category `chest` trả đúng subset, phân trang lại.  
- Builder: sửa Thứ 2 trên Lộ trình → tuần sau Thứ 2 đổi; session đã start hôm nay không đổi.  
- Kill criterion: nếu user thực sự cần “chỉ hôm nay” trên màn Lộ trình trước khi start → model B không đủ (phải overlay ngày). **User đã chốt: màn Lộ trình = sửa template.**

---

## 2. Quyết định đã chốt

| # | Chủ đề | Quyết định |
|---|---|---|
| 1 | “Tự xây” | Buổi tập / lộ trình builder (kéo-thả), **không** chỉ modal tạo bài |
| 2 | Model dữ liệu | **B — Snapshot lúc bấm Bắt đầu tập** |
| 3 | Màn Lộ trình (trước Start) | Sửa **template tuần**; user tự sửa các template |
| 4 | Lọc thư viện | Category + Equipment như `index.html`; **không** chip Target Muscle; **không** chip 6 nhóm cơ |
| 5 | Chip | Đa chọn: OR trong nhóm, AND giữa Category và Equipment |
| 6 | `muscle_group` 6 nhóm | Ẩn khỏi filter; **bắt buộc** trên form bài custom (Rule Engine) |
| 7 | UX thư viện lần này | Chỉ chip + search; không badge / clear-all |
| 8 | Kéo-thả web | `@dnd-kit` |
| 9 | Lưu dữ liệu | Backend API + PostgreSQL (đồng bộ nhiều thiết bị) |
| 10 | Tách spec | **012** filter thư viện trước; **013** builder sau |
| 11 | Set/rep khác nhau từng hiệp | **P2** — MVP: một `target_reps` cho mọi set; tracking vẫn ghi actual từng set |

Modal tạo bài custom (011) **giữ nguyên** như nguồn bài cho cả auto-plan và builder.

---

## 3. Approaches đã đánh giá

### 3.1 Lọc thư viện

| Cách | Ưu | Nhược | Kết luận |
|---|---|---|---|
| A. Client-side filter 1324 rows | Đúng hành vi HTML | Phá pagination API hiện tại | Không |
| B. Server `IN` list + chip UI | Khớp REST hiện có, nhỏ | Đổi query spec | **Chọn** |
| C. Giữ 6 nhóm cơ + thêm category | Ít phá FR-013 | Hai tầng “cơ” gây rối | Không — user chọn A |

### 3.2 Builder / buổi lặp

| Cách | Ưu | Nhược | Kết luận |
|---|---|---|---|
| A. Chỉ sửa `workout_plan_exercises` | Không bảng mới | Không bảo vệ buổi đang/đã tập khi user sửa template giữa chừng | Thiếu |
| **B. Snapshot lúc Start** | Khớp `workout_sessions`; sửa template không đụng buổi đã start | Đổi uniqueness `workout_sets`; thêm `session_exercises` | **Chọn** |
| C. Snapshot mọi ngày lịch | “Chỉ hôm nay” dễ | Timezone, ngày nghỉ, mồ côi | Không |

Rule Engine v1 **vẫn sinh template ban đầu**. Builder là editor của template đó, không thay auto-schedule.

---

## 4. Solution được chọn

### 4.1 Feature 012 — Lọc thư viện (nhỏ, làm trước)

**UI:** `web/src/pages/plan/ExerciseSearchPage.tsx`  
**API:** `GET /api/v1/exercises` — `category` và `equipment` nhận **nhiều giá trị** (repeated query param hoặc comma-separated; recommend repeated: `?category=chest&category=back`).  
**Service:** `ExerciseService.search` đổi `cb.equal` → `root.get(...).in(values)`.

Giá trị **đúng dataset** (không bịa):

**Category (10)** — DB giữ khoảng trắng như seed, `category === body_part`:

`back`, `cardio`, `chest`, `lower arms`, `lower legs`, `neck`, `shoulders`, `upper arms`, `upper legs`, `waist`

**Equipment (28)** — DB `snake_case` (seeder `normalizeEquipment`):

`assisted`, `band`, `barbell`, `body_weight`, `bosu_ball`, `cable`, `dumbbell`, `elliptical_machine`, `ez_barbell`, `hammer`, `kettlebell`, `leverage_machine`, `medicine_ball`, `olympic_barbell`, `resistance_band`, `roller`, `rope`, `skierg_machine`, `sled_machine`, `smith_machine`, `stability_ball`, `stationary_bike`, `stepmill_machine`, `tire`, `trap_bar`, `upper_body_ergometer`, `weighted`, `wheel_roller`

Nhãn tiếng Việt đặt trong `web/src/services/labels.ts` (hoặc hằng số filter dùng chung).

**Sửa spec 008 FR-013:** bỏ bắt buộc lọc theo 6 nhóm cơ trên UI thư viện; thay bằng category + equipment đa chọn.

**Out of 012:** badge filter, clear-all, Target Muscle, sidebar HTML, grid GIF hover, mở rộng Rule Engine 28 dụng cụ, builder kéo-thả.

### 4.2 Feature 013 — Manual plan builder (lớn hơn)

**Template (user sửa trên `/plan`):**

- Giữ `workout_plans` / `workout_plan_days` / `workout_plan_exercises`.
- Thêm: thứ tự bài (`sort_order`), API PATCH/PUT thay thế danh sách bài một ngày, kéo-thả dnd-kit.
- Picker bài tái sử dụng filter 012 (kể cả bài `user_custom`).
- Sửa Thứ 2 trên Lộ trình = sửa template → mọi Thứ 2 **chưa start** sau đó.

**Snapshot (lúc Bắt đầu tập):**

- Tạo `workout_session_exercises`: copy `exercise_id`, `target_sets`, `target_reps`, `rest_time_seconds`, `sort_order` từ plan day tương ứng (timezone user).
- `workout_sets` gắn `session_exercise_id` (hoặc `(session_id, exercise_id, set_number)`).
- **Phải đổi** `UNIQUE (session_id, set_number)` ở `V4__workout_tracking.sql` — không xóa migration cũ; thêm migration mới drop constraint + unique mới. Constitution: không xóa file migration.

**Luồng:**

```
Rule Engine (lần đầu / tạo lại goal)
    → Template tuần (user kéo-thả sửa trên /plan)
         → User bấm Bắt đầu
              → Snapshot session_exercises
                   → Ghi actual vào workout_sets
```

Sửa template **sau** khi session đã `active`/`completed` **không** rewrite snapshot đó.

**Out of 013 (MVP):** materialize lịch từng ngày; “chỉ hôm nay” trên màn Lộ trình; per-set target array; DnD Flutter; đổi Goal Setup sang 28 equipment.

---

## 5. Implementation considerations & risks

1. **Tracking hiện tại primitive.** `WorkoutPage` ghi một dải `set_number` toàn session. Builder bắt buộc refactor tracking theo từng bài trong snapshot.  
2. **Offline/LWW (General Spec §6).** UPSERT set phải đổi key theo bài; cập nhật 009 + client sync.  
3. **Equipment 28 vs Rule Engine 5.** Filter dùng 28; auto-plan vẫn 5. Custom `kettlebell` có thể không được Rule Engine chọn — chấp nhận vòng này.  
4. **Category Admin lệch.** Form admin dùng `strength`; seed thật là `chest`/`waist`/… Cần label đúng seed, không theo data-model.md cũ.  
5. **dnd-kit chỉ web.** Flutter làm sau, DnD riêng.  
6. **Coverage 80%** module mới (constitution).  
7. **Nhánh Git Flow:** `feature/012-...` rồi `feature/013-...`; không commit `main`/`develop`.

### Touchpoints

| Lớp | File chính |
|---|---|
| Web filter | `ExerciseSearchPage.tsx`, `planApi.ts`, `labels.ts` |
| Backend filter | `ExerciseController`, `ExerciseService`, tests |
| Web builder | `PlanPage.tsx`, `WorkoutPage.tsx`, dnd-kit dependency |
| Backend builder | `WorkoutPlanController/Service`, `TrackingService`, entity session exercises, Flyway V12+ |
| Spec cũ | `008` FR-013; `009` uniqueness sets; `011` form custom giữ `muscleGroup` |

---

## 6. Success metrics / acceptance

**012**

- Chip Category/Equipment đa chọn; search tên debounce hoặc Enter+đổi chip reload page 0.  
- Kết quả AND giữa hai chiều, OR trong chiều.  
- Không còn chip 6 nhóm cơ trên thư viện.  
- Pagination `totalElements` đúng sau filter.

**013**

- User thêm/xóa/đổi thứ tự bài trên một ngày template; persist API.  
- Bắt đầu tập copy snapshot; đổi template sau đó không đổi session đã start.  
- Lịch sử buổi cũ (completed) không đổi tên/bài đã ghi.  
- Bài custom xuất hiện trong picker nếu thuộc user.

---

## 7. Next steps

**Không implement trong phiên brainstorm.** SpecKit implement cần `spec.md` + `plan.md` + `tasks.md` — chưa có cho 012/013.

Thứ tự:

1. `/speckit-specify` cho **012-exercise-library-filters** (filter Category/Equipment).  
2. `/speckit-plan` → `/speckit-tasks` → `/speckit-implement` (012).  
3. Lặp lại cho **013-manual-workout-builder**.

Prompt gợi ý 012:

> Lọc thư viện bài tập theo Category (10 giá trị dataset) và Equipment (28 giá trị snake_case), chip đa chọn OR trong nhóm AND giữa hai nhóm, search theo tên. Bỏ chip nhóm cơ 6 giá trị trên UI. Backend GET /exercises nhận nhiều category/equipment. Không làm Target Muscle, badge, builder kéo-thả.

Prompt gợi ý 013:

> User tự sửa template tuần trên màn Lộ trình (kéo-thả dnd-kit, thêm/xóa bài, set/rep thống nhất). Rule Engine vẫn sinh template ban đầu. Khi bấm Bắt đầu tập, copy snapshot vào session_exercises; sửa template không ảnh hưởng buổi đã start. Đổi unique workout_sets theo bài. Không materialize từng ngày lịch. Per-set target khác nhau là P2.
