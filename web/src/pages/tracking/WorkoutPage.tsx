import { useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { trackingApi } from '../../services/trackingApi';
import type { WorkoutSession } from '../../services/trackingApi';

export default function WorkoutPage() {
  const [session, setSession] = useState<WorkoutSession | null>(null);
  const [setNumber, setSetNumber] = useState('1');
  const [reps, setReps] = useState('');
  const [weight, setWeight] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const onStart = async () => {
    setError('');
    try {
      setSession(await trackingApi.startSession());
      setNotice('Buổi tập đã bắt đầu.');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể bắt đầu buổi tập');
    }
  };

  const onRecordSet = async () => {
    if (!session) return;
    setError('');
    try {
      await trackingApi.recordSet(session.id, {
        setNumber: Number(setNumber),
        repsCompleted: reps ? Number(reps) : undefined,
        weightUsed: weight ? Number(weight) : undefined,
      });
      setNotice(`Đã lưu hiệp ${setNumber}.`);
      setSetNumber(String(Number(setNumber) + 1));
      setReps('');
      setWeight('');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu hiệp thất bại');
    }
  };

  const onFocus = async () => {
    if (!session) return;
    setSession(await trackingApi.incrementFocus(session.id));
  };

  const onComplete = async () => {
    if (!session) return;
    await trackingApi.completeSession(session.id);
    setNotice('Buổi tập đã hoàn thành.');
    setSession(null);
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32, display: 'flex', justifyContent: 'center' }}>
      <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 32, width: '100%', maxWidth: 560, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Buổi tập</h1>
        {!session ? (
          <Button onClick={onStart}>Bắt đầu buổi tập</Button>
        ) : (
          <>
            <p style={{ color: 'var(--text-secondary)' }}>Trạng thái: {session.status} · Phân tâm: {session.focusInterruptionsCount}</p>
            <div style={{ display: 'flex', gap: 8 }}>
              <TextField label="Hiệp" type="number" value={setNumber} onChange={(e) => setSetNumber(e.target.value)} />
              <TextField label="Số lần" type="number" value={reps} onChange={(e) => setReps(e.target.value)} />
              <TextField label="Tạ (kg)" type="number" value={weight} onChange={(e) => setWeight(e.target.value)} />
            </div>
            <Button onClick={onRecordSet}>Lưu hiệp</Button>
            <Button variant="outlined" onClick={onFocus}>Báo phân tâm</Button>
            <Button variant="outlined" onClick={onComplete} style={{ color: 'var(--text-announcement)' }}>Kết thúc buổi tập</Button>
          </>
        )}
        {notice && <span style={{ color: 'var(--text-announcement)' }}>{notice}</span>}
        {error && <span style={{ color: 'var(--text-negative)' }}>{error}</span>}
      </div>
    </div>
  );
}
