# Data Model: Màn tập trung tối giản (016)

## Nguyên tắc

**Không thay đổi schema.** Database đang ở Flyway v16; mọi nhu cầu dữ liệu đã có sẵn từ 013/014/015. Chỉ có 2 thay đổi ở tầng API.

## 1. `SessionExerciseResponse` — thêm `mediaUrl`

| Field | Kiểu | Ghi chú |
|---|---|---|
| `mediaUrl` | `String` (nullable) | `Exercise.gifUrl` nếu có, nếu không lấy `Exercise.image`; null khi bài không có media |

- Nguồn: `TrackingService.toResponse(...)` — join `ExerciseRepository.findById(exerciseId)`.
- Chống N+1: trong 1 request, build map `exerciseId → mediaUrl` một lần rồi gán cho từng exercise của session.
- KHÔNG lưu vào snapshot (`workout_session_exercises`) — media lấy live từ bài tập; nếu bài bị ẩn/xóa sau snapshot thì fallback icon (chấp nhận).

## 2. Endpoint xóa hiệp (undo) — `workout_sets`

| Field | Quy tắc |
|---|---|
| `DELETE /api/v1/workout-sessions/{sessionId}/sets/{setId}` | 404 nếu set không tồn tại hoặc không thuộc session; 409 nếu session không active (message riêng cho `expired`); 200/204 khi xóa thành công |

**Validation (service)**:
1. `sessionRepository.findById(sessionId)` → 404 nếu không có.
2. `session.userId == userId` → 404 nếu khác.
3. `session.status == "active"` → 409 nếu không (completed/interrupted/expired).
4. `setRepository.findById(setId)` và `set.sessionId == sessionId` → 404 nếu sai.
5. `setRepository.delete(set)`.

**Giới hạn 60 giây**: chỉ phía client (ẩn nút sau 60s từ lúc ghi) — server không cần cột timestamp mới. Rủi ro (client giả mạo xóa set cũ trong session active) chấp nhận được ở MVP: session active chỉ sống trong ngày (auto-expire), và user là chủ sở hữu dữ liệu của mình.

## 3. State transitions (không đổi)

- `WorkoutSession`: active → completed (kết thúc sớm/tự nhiên) / expired (sang ngày) — không đổi.
- `WorkoutSet`: tạo qua `POST /sets` (UPSERT theo session_id + session_exercise_id + set_number) — không đổi; bổ sung khả năng DELETE trong session active.

## 4. Luồng ghi hiệp ở màn tối giản (không đổi contract ghi)

Nút HOÀN THÀNH gọi đúng `POST /api/v1/workout-sessions/{id}/sets` hiện tại với:

```json
{
  "exerciseId": "<exerciseId>",
  "sessionExerciseId": "<sessionExerciseId>",
  "setNumber": "<hiệp hiện tại>",
  "repsCompleted": "<targetReps của target hiệp>",
  "weightUsed": "<tạ hiệp liền trước cùng bài, hoặc null>",
  "restTimeSeconds": "<restTimeSeconds của bài>",
  "setType": "<setType của target hiệp trong kế hoạch>"
}
```

Bài duration ghi khi hết giờ:

```json
{
  "exerciseId": "<exerciseId>",
  "sessionExerciseId": "<sessionExerciseId>",
  "setNumber": "<hiệp hiện tại>",
  "durationSeconds": "<số giây đã đếm>",
  "restTimeSeconds": "<restTimeSeconds của bài>",
  "setType": "<setType của target hiệp>"
}
```

## 5. Không có migration mới

- Files V13–V16 giữ nguyên (không sửa, không xóa).
- Nếu sau này cần undo không giới hạn hoặc audit xóa set, sẽ thêm migration V17 (ví dụ `deleted_at`) — ngoài scope 016.
