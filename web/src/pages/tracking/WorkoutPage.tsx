import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { trackingApi } from '../../services/trackingApi';
import type { WorkoutSession } from '../../services/trackingApi';

interface SetLog { setNumber: number; reps?: number; weight?: number; savedAt: Date; }

const DEFAULT_REST_SECONDS = 60;

function playBeep() {
  try {
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new Ctx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.type = 'sine';
    osc.frequency.value = 880;
    gain.gain.value = 0.08;
    osc.start();
    osc.stop(ctx.currentTime + 0.18);
  } catch {
    /* Audio API không khả dụng — bỏ qua cảnh báo âm thanh */
  }
}

export default function WorkoutPage() {
  const [session, setSession] = useState<WorkoutSession | null>(null);
  const [setNumber, setSetNumber] = useState('1');
  const [reps, setReps] = useState('');
  const [weight, setWeight] = useState('');
  const [restSeconds, setRestSeconds] = useState(String(DEFAULT_REST_SECONDS));
  const [restRemaining, setRestRemaining] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [setLog, setSetLog] = useState<SetLog[]>([]);
  const [elapsed, setElapsed] = useState(0);

  // Elapsed timer
  useEffect(() => {
    if (!session || session.status !== 'active') return;
    const id = setInterval(() => setElapsed((s) => s + 1), 1000);
    return () => clearInterval(id);
  }, [session]);

  // Rest timer countdown (FR-001/FR-002)
  const isResting = restRemaining != null && restRemaining > 0;
  useEffect(() => {
    if (!isResting) return;
    const id = setInterval(() => {
      setRestRemaining((s) => (s != null && s > 0 ? s - 1 : null));
    }, 1000);
    return () => clearInterval(id);
  }, [isResting]);

  // Alert at 5 seconds left and when rest finishes
  useEffect(() => {
    if (restRemaining === 5 || restRemaining === 0) {
      playBeep();
    }
  }, [restRemaining]);

  // FR-004: focus detection — phát hiện phân tâm khi tab bị ẩn > 15 giây
  useEffect(() => {
    if (!session || session.status !== 'active') return;
    let hiddenAt: number | null = null;
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') {
        hiddenAt = Date.now();
      } else if (hiddenAt != null) {
        if (Date.now() - hiddenAt >= 15000) {
          trackingApi.incrementFocus(session.id).then(setSession).catch(() => {});
        }
        hiddenAt = null;
      }
    };
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, [session]);

  const fmt = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const onStart = async () => {
    setError('');
    try {
      const s = await trackingApi.startSession();
      setSession(s);
      setElapsed(0);
      setSetLog([]);
      setRestRemaining(null);
      setNotice('Buổi tập đã bắt đầu. Cố lên! 💪');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể bắt đầu buổi tập');
    }
  };

  // FR-011: chống double-tap khi lưu hiệp
  const onRecordSet = async () => {
    if (!session || saving) return;
    setSaving(true);
    setError('');
    try {
      const rest = restSeconds ? Number(restSeconds) : undefined;
      await trackingApi.recordSet(session.id, {
        setNumber: Number(setNumber),
        repsCompleted: reps ? Number(reps) : undefined,
        weightUsed: weight ? Number(weight) : undefined,
        restTimeSeconds: rest,
      });
      setSetLog((prev) => [...prev, { setNumber: Number(setNumber), reps: reps ? Number(reps) : undefined, weight: weight ? Number(weight) : undefined, savedAt: new Date() }]);
      setNotice(`✅ Đã lưu hiệp ${setNumber}.`);
      setSetNumber(String(Number(setNumber) + 1));
      setReps('');
      setWeight('');
      if (rest != null && rest > 0) setRestRemaining(rest);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu hiệp thất bại');
    } finally {
      setSaving(false);
    }
  };

  const onFocus = async () => {
    if (!session) return;
    const updated = await trackingApi.incrementFocus(session.id);
    setSession(updated);
    setNotice('⚠️ Đã ghi nhận 1 lần phân tâm.');
  };

  const onComplete = async () => {
    if (!session) return;
    await trackingApi.completeSession(session.id);
    setNotice('🎉 Buổi tập đã hoàn thành. Tuyệt vời!');
    setSession(null);
    setSetLog([]);
    setElapsed(0);
    setRestRemaining(null);
  };

  const restPct = restRemaining != null && restRemaining > 0
    ? Math.round((restRemaining / Math.max(1, Number(restSeconds) || DEFAULT_REST_SECONDS)) * 100)
    : 0;

  return (
    <div className="page-container" style={{ maxWidth: 640, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <h1>💪 Buổi tập</h1>

      {!session ? (
        /* ── Pre-session card ── */
        <div className="card" style={{ textAlign: 'center', padding: '48px 32px' }}>
          <div style={{ fontSize: 64, marginBottom: 16 }}>🏋️</div>
          <h2 style={{ marginBottom: 8 }}>Sẵn sàng chinh phục?</h2>
          <p className="text-secondary" style={{ marginBottom: 28, fontSize: 14 }}>
            Bắt đầu buổi tập để ghi nhận sets, reps và tạ theo thời gian thực.
          </p>
          <Button onClick={onStart} size="lg">
            Bắt đầu buổi tập
          </Button>
          {error && <div className="notice notice-error" style={{ marginTop: 16 }}>{error}</div>}
        </div>
      ) : (
        /* ── Active session ── */
        <>
          {/* Session status bar */}
          <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 20px', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
              <div>
                <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1 }}>Thời gian</div>
                <div style={{ fontSize: 24, fontWeight: 700, fontFamily: 'monospace', color: 'var(--green)' }}>
                  {fmt(elapsed)}
                </div>
              </div>
              <div style={{ marginLeft: 24 }}>
                <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1 }}>Phân tâm</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: session.focusInterruptionsCount > 0 ? 'var(--text-warning)' : 'var(--text-base)' }}>
                  {session.focusInterruptionsCount}×
                </div>
              </div>
              <div style={{ marginLeft: 24 }}>
                <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1 }}>Hiệp đã lưu</div>
                <div style={{ fontSize: 24, fontWeight: 700 }}>
                  {setLog.length}
                </div>
              </div>
            </div>
            <span className="badge badge-green" style={{ animation: 'pulse 2s infinite' }}>● ĐANG TẬP</span>
          </div>

          {/* Rest timer */}
          {restRemaining != null && restRemaining > 0 && (
            <div className="card animate-slide-up" style={{ textAlign: 'center', padding: '22px 20px', border: '1px solid rgba(30,215,96,0.25)' }}>
              <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1, marginBottom: 6 }}>
                ⏳ Nghỉ giữa hiệp
              </div>
              <div style={{ fontSize: 40, fontWeight: 800, fontFamily: 'monospace', color: 'var(--green)', lineHeight: 1 }}>
                {fmt(restRemaining)}
              </div>
              <div className="progress-bar" style={{ height: 6, marginTop: 12 }}>
                <div className="progress-fill" style={{ width: `${restPct}%` }} />
              </div>
              <Button variant="outlined" size="sm" onClick={() => setRestRemaining(null)} style={{ marginTop: 14 }}>
                Bỏ qua nghỉ
              </Button>
            </div>
          )}

          {/* Input row */}
          <div className="card">
            <h3 style={{ marginBottom: 16 }}>Ghi nhận hiệp tập</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 12, marginBottom: 12 }}>
              <TextField label="Hiệp" type="number" value={setNumber} onChange={(e) => setSetNumber(e.target.value)} min="1" />
              <TextField label="Số lần (reps)" type="number" value={reps} onChange={(e) => setReps(e.target.value)} placeholder="12" min="0" />
              <TextField label="Tạ (kg)" type="number" value={weight} onChange={(e) => setWeight(e.target.value)} placeholder="0" min="0" step="0.5" />
            </div>
            <TextField label="Thời gian nghỉ (giây)" type="number" value={restSeconds} onChange={(e) => setRestSeconds(e.target.value)} min="5" placeholder={String(DEFAULT_REST_SECONDS)} />
            <Button onClick={onRecordSet} fullWidth loading={saving} style={{ marginTop: 16 }}>
              Lưu hiệp {setNumber}
            </Button>
          </div>

          {/* Set log table */}
          {setLog.length > 0 && (
            <div className="card animate-fade">
              <h3 style={{ marginBottom: 14 }}>Hiệp đã tập ({setLog.length})</h3>
              <table className="data-table">
                <thead>
                  <tr><th>Hiệp</th><th>Reps</th><th>Tạ (kg)</th></tr>
                </thead>
                <tbody>
                  {setLog.map((s, i) => (
                    <tr key={i}>
                      <td className="fw-700">#{s.setNumber}</td>
                      <td>{s.reps ?? <span className="text-muted">—</span>}</td>
                      <td>{s.weight != null ? `${s.weight} kg` : <span className="text-muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Notices */}
          {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
          {error && <div className="notice notice-error animate-slide-up">{error}</div>}

          {/* Footer actions */}
          <div style={{ display: 'flex', gap: 10 }}>
            <Button variant="outlined" onClick={onFocus} style={{ flex: 1 }}>
              ⚠️ Báo phân tâm
            </Button>
            <Button
              variant="outlined"
              onClick={onComplete}
              style={{ flex: 1, color: 'var(--green)', borderColor: 'rgba(30,215,96,0.4)' }}
            >
              ✅ Kết thúc buổi tập
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
