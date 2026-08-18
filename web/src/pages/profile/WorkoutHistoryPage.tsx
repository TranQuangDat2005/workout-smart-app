import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import { profileApi } from '../../services/profileApi';
import type { WorkoutSessionDetail, WorkoutSessionItem } from '../../services/profileApi';

export default function WorkoutHistoryPage() {
  const [sessions, setSessions] = useState<WorkoutSessionItem[]>([]);
  const [detail, setDetail] = useState<WorkoutSessionDetail | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    profileApi
      .getSessions(0, 20)
      .then((page) => setSessions(page.content))
      .catch(() => setError('Không thể tải lịch sử tập'));
  }, []);

  const openDetail = (id: number) => {
    profileApi.getSessionDetail(id).then(setDetail).catch(() => setError('Không thể tải chi tiết'));
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
      }}
    >
      <div
        style={{
          background: 'var(--dark-surface)',
          borderRadius: 8,
          boxShadow: 'var(--shadow-heavy)',
          padding: 32,
          width: '100%',
          maxWidth: 720,
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
        }}
      >
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Lịch sử buổi tập</h1>
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {sessions.length === 0 && !error && (
          <p style={{ color: 'var(--text-secondary)' }}>
            Chưa có buổi tập nào. Hãy bắt đầu buổi tập đầu tiên!
          </p>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {sessions.map((s) => (
            <div
              key={s.id}
              style={{
                background: 'var(--mid-dark)',
                borderRadius: 6,
                padding: '12px 16px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div>
                <div style={{ fontWeight: 700, fontSize: 16 }}>
                  {new Date(s.startTime).toLocaleString('vi-VN')}
                </div>
                <div style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
                  {s.totalSets} hiệp · {s.totalVolumeKg} kg · {s.status}
                </div>
              </div>
              <Button variant="dark" onClick={() => openDetail(s.id)}>
                Chi tiết
              </Button>
            </div>
          ))}
        </div>
        {detail && (
          <div style={{ borderTop: '1px solid var(--border-dark)', paddingTop: 16 }}>
            <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 8 }}>Chi tiết buổi tập</h2>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
              <thead>
                <tr style={{ color: 'var(--text-secondary)', textAlign: 'left' }}>
                  <th>Hiệp</th>
                  <th>Reps</th>
                  <th>Kg</th>
                  <th>Nghỉ (s)</th>
                </tr>
              </thead>
              <tbody>
                {detail.sets.map((set) => (
                  <tr key={set.id} style={{ borderTop: '1px solid var(--border-dark)' }}>
                    <td style={{ padding: '6px 0' }}>{set.setNumber}</td>
                    <td>{set.repsCompleted ?? '—'}</td>
                    <td>{set.weightUsed ?? '—'}</td>
                    <td>{set.restTimeSeconds ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
