import Button from '../../../components/Button';

function fmtClock(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

interface DurationTimerProps {
  total: number;
  remaining: number;
  running: boolean;
  saving: boolean;
  onStart: () => void;
  onPause: () => void;
  onSave: () => void;
}

/** Đồng hồ bài tập tính giây (016 FR-003): đếm ngược, Bắt đầu/Tạm dừng, Lưu khi đã đếm dở. */
export default function DurationTimer({ total, remaining, running, saving, onStart, onPause, onSave }: DurationTimerProps) {
  const pct = total > 0 ? Math.round(((total - remaining) / total) * 100) : 0;
  return (
    <div style={{ textAlign: 'center' }}>
      <div
        role="timer"
        aria-label="thời gian bài tập"
        style={{ fontSize: '3.2rem', fontWeight: 800, fontFamily: 'monospace', color: 'var(--green)', lineHeight: 1 }}
      >
        {fmtClock(remaining)}
      </div>
      <div className="progress-bar" style={{ height: 6, marginTop: 12 }}>
        <div className="progress-fill" style={{ width: `${pct}%` }} />
      </div>
      <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
        {running ? (
          <Button variant="outlined" onClick={onPause} style={{ flex: 1 }}>
            Tạm dừng
          </Button>
        ) : (
          <Button onClick={onStart} loading={saving} style={{ flex: 1 }}>
            {remaining < total ? 'Tiếp tục' : 'Bắt đầu'}
          </Button>
        )}
        {!running && remaining < total && (
          <Button variant="outlined" onClick={onSave} disabled={saving} style={{ flex: 1, color: 'var(--green)', borderColor: 'rgba(30,215,96,0.4)' }}>
            Lưu hiệp
          </Button>
        )}
      </div>
    </div>
  );
}
