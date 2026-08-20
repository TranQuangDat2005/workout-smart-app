# Tasks: Nhạc luyện tập qua URL (017)

**Input**: Design documents từ `/specs/017-workout-music/`

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [X] T001 Cập nhật `.specify/feature.json` → `specs/017-workout-music`

## Phase 2: Governance (owner-approved)

- [X] T002 Sửa `.specify/memory/constitution.md` §5: thêm YouTube IFrame Player API + Spotify embed vào allowlist (nhạc luyện tập, chỉ Web client)
- [X] T003 [P] Sửa `AGENTS.md` §2: bổ sung ngoại lệ allowlist tương ứng

## Phase 3: User Story 1 — Ô nhập URL + lưu localStorage (P1)

- [X] T004 [US1] Tạo `web/src/pages/training/music/musicSource.ts`: `parseMusicSource(url)` hỗ trợ mp3/ogg/m4a/wav, YouTube (watch/shorts/youtu.be/embed), Spotify (web link/URI)
- [X] T005 [US1] Thêm ô nhập "Nhạc luyện tập (URL…)" ở màn bắt đầu buổi tập trong `web/src/pages/training/WorkoutSession.tsx`; lưu/khôi phục `localStorage` key `workoutMusicUrl`
- [X] T006 [US1] Unit test `web/src/pages/training/music/musicSource.test.ts` (12+ case: 3 loại + invalid + edge)

## Phase 4: User Story 2 — Tự động phát khi bắt đầu (P1)

- [X] T007 [US2] Tạo `web/src/pages/training/music/WorkoutMusicPlayer.tsx`: nhánh `audio` (HTMLAudioElement, autoplay, error → onError), nhánh `youtube` (load `https://www.youtube.com/iframe_api`, autoplay, fallback nút play), nhánh `spotify` (iframe `https://open.spotify.com/embed/...`)
- [X] T008 [US2] Mount player trong `WorkoutSession.tsx` khi session active + có URL hợp lệ; URL không hợp lệ → lỗi nhẹ, không chặn luồng

## Phase 5: User Story 3 — Mini player (P1)

- [X] T009 [US3] Mini player controls: play/pause, volume slider, seek slider + hiển thị mm:ss/mm:ss cho mp3 + YouTube (poll ≤ 2 lần/giây)
- [X] T010 [US3] Component test `web/src/pages/training/music/WorkoutMusicPlayer.test.tsx` (audio render + play/pause; spotify iframe src đúng)

## Phase 6: User Story 4 — Tự dừng khi kết thúc (P2)

- [X] T011 [US4] Player unmount khi session kết thúc/expired → cleanup dừng nhạc (audio.pause, player.destroy) trong `WorkoutMusicPlayer.tsx` + điều kiện render ở `WorkoutSession.tsx`
- [X] T012 [US4] Test: hoàn tất buổi → player biến mất (trong `WorkoutSession.test.tsx`)

## Phase 7: Polish

- [X] T013 [P] `npm --prefix web test` + `npm --prefix web run build` pass
- [X] T014 Kiểm tra tay: dán mp3/YouTube/Spotify → bắt đầu → play/pause/volume/seek → kết thúc → nhạc dừng

## Dependencies

- US1 → US2 → US3 → US4 tuần tự (US4 độc lập, làm song song được với US3)
- Governance (T002/T003) làm trước hoặc song song — bắt buộc trước khi merge

## Notes

- Không backend, không migration, không dependency mới
- Commit Conventional Commits sau mỗi phase
