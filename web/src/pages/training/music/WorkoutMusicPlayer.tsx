import { useEffect, useSyncExternalStore } from 'react';
import Button from '../../../components/Button';
import type { MusicSource } from './musicSource';
import { musicEngine } from './musicEngine';

interface WorkoutMusicPlayerProps {
  source: MusicSource;
  onError: (message: string) => void;
}

function fmtClock(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) return '00:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

/**
 * Mini player nhạc luyện tập — 017 FR-003/FR-004/FR-007.
 * Chỉ là lớp UI mỏng: luồng phát nằm trong musicEngine (singleton) nên mọi
 * re-render của React KHÔNG tạo lại player (không reload iframe/audio).
 * Spotify dùng embed với điều khiển gốc.
 */
export default function WorkoutMusicPlayer({ source, onError }: WorkoutMusicPlayerProps) {
  const state = useSyncExternalStore(musicEngine.subscribe, musicEngine.getSnapshot);

  // Chỉ phụ thuộc key nguyên thủy — object identity của source thay đổi không gây re-init.
  useEffect(() => {
    musicEngine.start(source, onError);
    return () => musicEngine.stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [source.type, source.id, source.url]);

  if (source.type === 'spotify') {
    return (
      <iframe
        src={`https://open.spotify.com/embed/${source.id}`}
        width="100%"
        height="80"
        frameBorder="0"
        allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
        loading="lazy"
        title="Spotify player"
      />
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Button size="sm" onClick={() => musicEngine.toggle()} disabled={!state.ready} style={{ minWidth: 84 }}>
          {state.playing ? '❚❚ Tạm dừng' : '▶ Phát'}
        </Button>
        <div style={{ fontSize: 12, fontFamily: 'monospace', color: 'var(--text-muted)', minWidth: 90, textAlign: 'center' }}>
          {fmtClock(state.currentTime)} / {fmtClock(state.duration)}
        </div>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="text-xs text-muted" style={{ whiteSpace: 'nowrap' }}>Vol</span>
          <input
            type="range"
            min={0}
            max={100}
            value={state.volume}
            onChange={(e) => musicEngine.setVolume(Number(e.target.value))}
            aria-label="Âm lượng"
            style={{ width: 80 }}
          />
        </div>
      </div>
      <div style={{ marginTop: 8 }}>
        <input
          type="range"
          min={0}
          max={Math.max(1, Math.floor(state.duration))}
          step={1}
          value={Math.min(Math.floor(state.currentTime), Math.max(1, Math.floor(state.duration)))}
          onChange={(e) => musicEngine.seekTo(Number(e.target.value))}
          aria-label="Tiến trình phát"
          style={{ width: '100%' }}
        />
      </div>
      {source.type === 'youtube' && (
        /* YouTube chỉ phát audio: iframe bị thu nhỏ + trong suốt, không hiển thị video.
           Âm thanh vẫn phát bình thường; điều khiển nằm ở thanh mini phía trên. */
        <div
          id={`yt-player-${source.id}`}
          aria-hidden
          style={{
            position: 'absolute',
            width: 1,
            height: 1,
            opacity: 0,
            overflow: 'hidden',
            pointerEvents: 'none',
          }}
        />
      )}
    </div>
  );
}
