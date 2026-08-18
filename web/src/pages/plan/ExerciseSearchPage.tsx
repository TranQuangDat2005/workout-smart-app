import { useState } from 'react';
import TextField from '../../components/TextField';
import Button from '../../components/Button';
import { mediaUrl, planApi } from '../../services/planApi';
import type { ExerciseDetail } from '../../services/planApi';

const EQUIPMENT_LIST = ['', 'body_weight', 'dumbbell', 'barbell', 'machine', 'resistance_band'];
const EQUIPMENT_LABELS: Record<string, string> = {
  '': 'Tất cả', body_weight: 'Tự trọng', dumbbell: 'Tạ đơn',
  barbell: 'Tạ đòn', machine: 'Máy tập', resistance_band: 'Dây kháng lực',
};

export default function ExerciseSearchPage() {
  const [q, setQ] = useState('');
  const [equipment, setEquipment] = useState('');
  const [results, setResults] = useState<ExerciseDetail[]>([]);
  const [detail, setDetail] = useState<ExerciseDetail | null>(null);
  const [imgSrc, setImgSrc] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const onSearch = async () => {
    setError(''); setDetail(null); setImgSrc(null);
    try {
      const res = await planApi.searchExercises({ q: q || undefined, equipment: equipment || undefined });
      setResults(res.content);
      setSearched(true);
    } catch {
      setError('Tìm kiếm thất bại');
    }
  };

  const onOpen = async (id: number) => {
    try {
      const d = await planApi.getExercise(id);
      setDetail(d);
      setImgSrc(mediaUrl(d.gifUrl) ?? mediaUrl(d.image));
    } catch {
      setError('Không thể tải chi tiết');
    }
  };

  const onImgError = () => {
    if (detail) {
      const fallback = mediaUrl(detail.image);
      if (fallback && imgSrc !== fallback) setImgSrc(fallback);
    }
  };

  return (
    <div className="page-container" style={{ maxWidth: 960, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <h1>📚 Thư viện bài tập</h1>

      {/* Search bar */}
      <div className="card" style={{ padding: 20 }}>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 200 }}>
            <TextField
              label="Tên bài tập"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && onSearch()}
              placeholder="Pull up, squat..."
            />
          </div>
          <Button variant="dark" onClick={onSearch} style={{ marginTop: 'auto' }}>Tìm kiếm</Button>
        </div>
        {/* Equipment filter chips */}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 14 }}>
          {EQUIPMENT_LIST.map((e) => (
            <button
              key={e}
              className={`chip ${equipment === e ? 'selected' : ''}`}
              onClick={() => setEquipment(e)}
            >
              {EQUIPMENT_LABELS[e]}
            </button>
          ))}
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      {/* Results + detail */}
      <div className="grid-two" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, alignItems: 'start' }}>
        {/* Results list */}
        <div>
          {searched && results.length === 0 && (
            <div className="empty-state" style={{ paddingTop: 40 }}>
              <div className="empty-state-icon">🔍</div>
              <p className="empty-state-text">Không tìm thấy kết quả phù hợp.</p>
            </div>
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {results.map((ex) => (
              <button
                key={ex.id}
                onClick={() => onOpen(ex.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  width: '100%',
                  textAlign: 'left',
                  padding: '12px 14px',
                  background: detail?.id === ex.id ? 'var(--mid-dark)' : 'var(--dark-surface)',
                  border: `1px solid ${detail?.id === ex.id ? 'var(--green)' : 'transparent'}`,
                  borderRadius: 8,
                  color: 'var(--text-base)',
                  cursor: 'pointer',
                  transition: 'background var(--t-fast), border-color var(--t-fast)',
                }}
              >
                {mediaUrl(ex.image) ? (
                  <img
                    src={mediaUrl(ex.image) ?? undefined}
                    alt={ex.name}
                    style={{ width: 44, height: 44, borderRadius: 6, objectFit: 'cover', background: 'var(--mid-dark)', flexShrink: 0 }}
                  />
                ) : (
                  <div style={{ width: 44, height: 44, borderRadius: 6, background: 'var(--mid-dark)', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
                    💪
                  </div>
                )}
                <div>
                  <div className="fw-600" style={{ fontSize: 14 }}>{ex.name}</div>
                  <div className="text-secondary" style={{ fontSize: 12 }}>{ex.equipment} · {ex.muscleGroup}</div>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Detail panel */}
        <div className="card" style={{ position: 'sticky', top: 20 }}>
          {detail ? (
            <div className="animate-fade">
              <h2 style={{ marginBottom: 6 }}>{detail.name}</h2>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
                <span className="badge badge-neutral">{detail.equipment}</span>
                <span className="badge badge-info">{detail.muscleGroup}</span>
                {detail.bodyPart && <span className="badge badge-neutral">{detail.bodyPart}</span>}
              </div>
              {imgSrc ? (
                <img
                  src={imgSrc}
                  alt={detail.name}
                  onError={onImgError}
                  style={{ width: 180, height: 180, objectFit: 'contain', background: 'var(--mid-dark)', borderRadius: 10, display: 'block', marginBottom: 14 }}
                />
              ) : (
                <div style={{ width: 180, height: 180, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--mid-dark)', borderRadius: 10, fontSize: 48, marginBottom: 14 }}>
                  💪
                </div>
              )}
              {detail.instructions && (
                <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                  {detail.instructions}
                </p>
              )}
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '32px 16px' }}>
              <div className="empty-state-icon">👆</div>
              <p className="empty-state-text">Chọn một bài tập để xem hướng dẫn chi tiết và ảnh/GIF.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
