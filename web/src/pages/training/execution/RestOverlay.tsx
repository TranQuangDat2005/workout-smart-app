import Button from '../../../components/Button';

function fmtClock(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

interface RestOverlayProps {
  remaining: number;
  total: number;
  onSkip: () => void;
}

/** Màn nghỉ tập trung (016 FR-004): đếm ngược cỡ lớn + vòng tiến độ + BỎ QUA. */
export default function RestOverlay({ remaining, total, onSkip }: RestOverlayProps) {
  const pct = Math.round((remaining / Math.max(1, total)) * 100);
  return (
    <div
      className="card animate-slide-up"
      style={{
        textAlign: 'center',
        padding: '28px 20px',
        border: '1px solid rgba(30,215,96,0.25)',
      }}
    >
      <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 2, marginBottom: 8 }}>
        Nghỉ giữa hiệp
      </div>
      <div
        role="timer"
        aria-label="thời gian nghỉ"
        style={{ fontSize: '4rem', fontWeight: 800, fontFamily: 'monospace', color: 'var(--green)', lineHeight: 1 }}
      >
        {fmtClock(remaining)}
      </div>
      <div className="progress-bar" style={{ height: 6, marginTop: 16 }}>
        <div className="progress-fill" style={{ width: `${pct}%` }} />
      </div>
      <Button variant="outlined" size="sm" onClick={onSkip} style={{ marginTop: 18 }}>
        BỎ QUA
      </Button>
    </div>
  );
}
