# Quickstart: Nhạc luyện tập qua URL (017)

## Prerequisites

- Frontend Vite `:5173` chạy (WSFrontend), backend `:8080` (WSBackend) — feature thuần frontend.

## Kịch bản kiểm thử tay

### T1 — Ô nhập + lưu localStorage (FR-001)
1. Mở tab Hôm nay (không có session) → thấy ô "Nhạc luyện tập (URL YouTube/Spotify/mp3 — tùy chọn)".
2. Dán `https://www.youtube.com/watch?v=dQw4w9WgXcQ` → bấm "Bắt đầu buổi tập" → reload → URL vẫn còn trong ô.

### T2 — mp3 tự phát (FR-003)
1. Dán URL mp3 trực tiếp (vd `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3`) → Bắt đầu.
2. **Kỳ vọng**: nhạc phát ngay; thanh mini player hiển thị.

### T3 — Mini player (FR-004)
1. Bấm pause → nhạc dừng; bấm play → tiếp tục.
2. Kéo volume → âm lượng đổi; kéo seek → nhảy đúng vị trí.

### T4 — YouTube (FR-002/FR-003)
1. Dán URL YouTube (watch/shorts/youtu.be) → Bắt đầu.
2. **Kỳ vọng**: nghe được âm thanh từ player YouTube; KHÔNG hiển thị khung video — chỉ còn thanh điều khiển mini của app (play/pause/volume/seek). Nếu autoplay bị chặn, bấm nút play trên thanh của app.

### T5 — Spotify (FR-002)
1. Dán `https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT` → Bắt đầu.
2. **Kỳ vọng**: embed Spotify hiện với điều khiển gốc.

### T6 — URL sai (FR-006)
1. Dán `abc` → Bắt đầu → buổi vẫn bắt đầu, hiện lỗi "URL nhạc không hợp lệ — đã bỏ qua nhạc".

### T7 — Tự dừng khi kết thúc (FR-007)
1. Đang phát → "Kết thúc buổi tập" → nhạc dừng, player biến mất.

### T8 — Nghỉ giữa hiệp (FR-005)
1. Ghi 1 hiệp → màn nghỉ hiện → nhạc vẫn phát, beep 5s/0s vẫn kêu.

## Run

```bat
npm --prefix web test
npm --prefix web run build
```

Vite HMR tự cập nhật — không cần restart.
