import { useState } from 'react';
import TextField from '../../components/TextField';
import Button from '../../components/Button';
import { planApi } from '../../services/planApi';
import type { ExerciseDetail } from '../../services/planApi';

export default function ExerciseSearchPage() {
  const [q, setQ] = useState('');
  const [equipment, setEquipment] = useState('');
  const [results, setResults] = useState<ExerciseDetail[]>([]);
  const [detail, setDetail] = useState<ExerciseDetail | null>(null);
  const [error, setError] = useState('');

  const onSearch = async () => {
    setError('');
    try {
      const res = await planApi.searchExercises({ q: q || undefined, equipment: equipment || undefined });
      setResults(res.content);
      setDetail(null);
    } catch {
      setError('Tìm kiếm thất bại');
    }
  };

  const onOpen = async (id: number) => {
    try {
      setDetail(await planApi.getExercise(id));
    } catch {
      setError('Không thể tải chi tiết');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32 }}>
      <h1 style={{ fontSize: 24, fontWeight: 700 }}>Thư viện bài tập</h1>
      <div style={{ display: 'flex', gap: 8, margin: '16px 0' }}>
        <TextField label="Tìm tên" value={q} onChange={(e) => setQ(e.target.value)} />
        <TextField label="Dụng cụ" value={equipment} onChange={(e) => setEquipment(e.target.value)} />
        <Button onClick={onSearch}>Tìm</Button>
      </div>
      {error && <p style={{ color: 'var(--text-negative)' }}>{error}</p>}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div>
          {results.map((ex) => (
            <button key={ex.id} onClick={() => onOpen(ex.id)}
              style={{ display: 'block', width: '100%', textAlign: 'left', padding: 12, marginBottom: 8, background: 'var(--dark-surface)', border: 'none', borderRadius: 8, color: 'var(--text-base)' }}>
              <strong>{ex.name}</strong> <span style={{ color: 'var(--text-secondary)' }}>· {ex.equipment}</span>
            </button>
          ))}
        </div>
        <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 16 }}>
          {detail ? (
            <>
              <h2 style={{ fontWeight: 700 }}>{detail.name}</h2>
              <p style={{ color: 'var(--text-secondary)' }}>Nhóm cơ: {detail.muscleGroup} · Bộ phận: {detail.bodyPart}</p>
              {detail.gifUrl && <img src={detail.gifUrl} alt={detail.name} style={{ width: 180, height: 180 }} />}
              {detail.instructions && <p style={{ whiteSpace: 'pre-wrap' }}>{detail.instructions}</p>}
            </>
          ) : (
            <p style={{ color: 'var(--text-secondary)' }}>Chọn một bài tập để xem chi tiết.</p>
          )}
        </div>
      </div>
    </div>
  );
}
