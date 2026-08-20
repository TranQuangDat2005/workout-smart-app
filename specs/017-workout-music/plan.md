# Implementation Plan: Nhạc luyện tập qua URL (017)

**Branch**: `017-workout-music` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

## Summary

Thêm nhạc luyện tập cho màn Tập hôm nay: ô nhập URL (YouTube/Spotify/mp3) ở màn bắt đầu buổi tập, lưu `localStorage`, tự phát khi bắt đầu, mini player (play/pause/volume/seek) khi tập, tự dừng khi kết thúc buổi. Thuần frontend — không backend, không migration.

## Technical Context

**Language/Version**: TypeScript (strict) + React 18

**Primary Dependencies**: React 18, Vite (KHÔNG thêm package); YouTube IFrame Player API (script `https://www.youtube.com/iframe_api`, chỉ Web client); Spotify embed iframe

**Storage**: `localStorage` (key `workoutMusicUrl`) — không DB

**Testing**: Jest + React Testing Library (parse URL thuần túy + render test)

**Target Platform**: Web SPA (responsive)

**Project Type**: web (SPA) — không đổi backend

**Performance Goals**: poll thời gian YouTube ≤ 2 lần/giây; không block render chính

**Constraints**: Không gọi external API từ backend; autoplay theo policy trình duyệt; player hủy sạch khi unmount (không phát vãng lai)

**Scale/Scope**: 1 ô input + 1 mini player + 2 file module

## Constitution Check

| Nguyên tắc | Trạng thái | Ghi chú |
|---|---|---|
| §2 Immutable Tech Stack | ✅ PASS | Không thêm dependency npm |
| §5 External API allowlist | ⚠️ CẦN SỬA ĐỔI | YouTube IFrame Player API + Spotify embed là external API phía Web client. Owner đã phê duyệt (Q1=B). Sửa constitution §5 + AGENTS.md §2: cho phép "nhạc luyện tập qua embed YouTube/Spotify, chỉ Web client, URL do User cung cấp, không gọi từ backend" |
| §5 Media không external | ✅ PASS | Không upload/tải media; chỉ nhúng player theo URL user |
| §6 Code quality | ✅ PASS | Component tách file, comment "why" |
| §7 Migration | ✅ PASS | Không đổi schema |
| §9 DoD | ⏳ verify | Unit test parse + component test |

## Project Structure

```text
web/src/pages/training/music/
├── musicSource.ts              # parseMusicSource(url) — thuần túy, testable
├── WorkoutMusicPlayer.tsx      # mini player: mp3 (Audio) / YouTube (IFrame API) / Spotify (embed)
├── musicSource.test.ts
└── WorkoutMusicPlayer.test.tsx

web/src/pages/training/WorkoutSession.tsx  # tích hợp: ô input + mount player + dừng khi kết thúc
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| External API (YouTube IFrame API) ngoài allowlist cũ | User yêu cầu tường minh YouTube/Spotify (Q1=B) | mp3-only không đáp ứng yêu cầu; Spotify embed là chuẩn duy nhất không cần API key |

## Governance Amendment (owner-approved 2026-08-19)

- **constitution §5** allowlist: thêm "YouTube IFrame Player API + Spotify embed (nhạc luyện tập — chỉ Web client, URL do User cung cấp, không gọi từ backend)".
- **AGENTS.md §2**: bổ sung ngoại lệ tương ứng cho allowlist external API.
