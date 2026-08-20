import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Spinner from '../../components/Spinner';
import { profileApi } from '../../services/profileApi';
import type { ExerciseProgress, WorkoutSessionDetail, WorkoutSessionItem } from '../../services/profileApi';

function deltaText(value: number | null, suffix: string) {
  if (value == null) return <span className="text-muted">—</span>;
  if (value > 0) return <span style={{ color: 'var(--green)', fontWeight: 600 }}>+{value}{suffix}</span>;
  if (value < 0) return <span style={{ color: '#f87171', fontWeight: 600 }}>{value}{suffix}</span>;
  return <span className="text-muted">0{suffix}</span>;
}

function statusBadge(status: string) {
  if (status === 'completed') return <span className="badge badge-green">Hoàn thành</span>;
  if (status === 'active')    return <span className="badge badge-info">Đang tập</span>;
  if (status === 'expired')   return <span className="badge badge-warning">Hết hạn</span>;
  return <span className="badge badge-neutral">{status}</span>;
}

export default function WorkoutHistoryPage() {
  const [sessions, setSessions] = useState<WorkoutSessionItem[]>([]);
  const [detail, setDetail] = useState<WorkoutSessionDetail | null>(null);
  const [detailSessionId, setDetailSessionId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [listLoading, setListLoading] = useState(true);
  const [progress, setProgress] = useState<ExerciseProgress[]>([]);
  const [progressLoading, setProgressLoading] = useState(true);
  const [showHistory, setShowHistory] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [notice, setNotice] = useState('');

  useEffect(() => {
    setProgressLoading(true);
    profileApi
      .getProgress(30)
      .then(setProgress)
      .catch(() => {})
      .finally(() => setProgressLoading(false));
  }, []);

  useEffect(() => {
    setListLoading(true);
    profileApi
      .getSessions(page, 20)
      .then((res) => {
        setSessions(res.content);
        setTotalPages(res.totalPages > 0 ? res.totalPages : 1);
      })
      .catch(() => setError('Không thể tải lịch sử tập'))
      .finally(() => setListLoading(false));
  }, [page]);

  const openDetail = (id: number) => {
    setDetailSessionId(id);
    setDetail(null);
    profileApi.getSessionDetail(id).then(setDetail).catch(() => setError('Không thể tải chi tiết'));
  };

  const closeDetail = () => {
    setDetailSessionId(null);
    setDetail(null);
  };

  const clearHistory = async () => {
    if (
      !window.confirm(
        'Xóa toàn bộ lịch sử tập? Thống kê, streak và bảng tiến bộ sẽ bị ảnh hưởng. Hành động này KHÔNG thể hoàn tác.',
      )
    ) {
      return;
    }
    if (!window.confirm('Bạn chắc chắn chứ? Dữ liệu các buổi tập đã qua sẽ bị xóa vĩnh viễn.')) {
      return;
    }
    setClearing(true);
    setError('');
    setNotice('');
    try {
      const res = await profileApi.clearHistory();
      setNotice(res.message);
      setSessions([]);
      setTotalPages(1);
      setPage(0);
      setDetailSessionId(null);
      setDetail(null);
      profileApi
        .getSessions(0, 20)
        .then((r) => {
          setSessions(r.content);
          setTotalPages(r.totalPages > 0 ? r.totalPages : 1);
        })
        .catch(() => {});
      profileApi
        .getProgress(30)
        .then(setProgress)
        .catch(() => {});
    } catch {
      setError('Không thể xóa lịch sử tập');
    } finally {
      setClearing(false);
    }
  };

  return (
    <div className="page-container" style={{ maxWidth: 800, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1><Icon name="clipboard" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Lịch sử buổi tập</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          {sessions.length > 0 && !detailSessionId && (
            <Button variant="danger" size="sm" loading={clearing} onClick={clearHistory}>
              Xóa lịch sử tập
            </Button>
          )}
          {detailSessionId && (
            <Button variant="outlined" size="sm" onClick={closeDetail}>
              ← Quay lại danh sách
            </Button>
          )}
        </div>
      </div>

      {notice && <div className="notice notice-success">{notice}</div>}
      {error && <div className="notice notice-error">{error}</div>}

      {/* Tiến bộ 30 ngày */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="section-header" style={{ padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
          <h2 className="section-title" style={{ margin: 0 }}>Tiến bộ 30 ngày</h2>
        </div>
        {progressLoading ? (
          <div style={{ padding: 24 }}><Spinner /></div>
        ) : progress.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-state-icon"><Icon name="trendingUp" size={42} /></div>
            <p className="empty-state-text">Chưa đủ dữ liệu để tính tiến bộ.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Bài tập</th>
                <th>Tạ (kg)</th>
                <th>Δ Tạ</th>
                <th>Reps</th>
                <th>Δ Reps</th>
              </tr>
            </thead>
            <tbody>
              {progress.map((p) => (
                <tr key={p.exerciseId}>
                  <td className="fw-600">{p.exerciseName}</td>
                  <td>{p.firstWeight != null && p.lastWeight != null ? `${p.firstWeight} → ${p.lastWeight}` : <span className="text-muted">—</span>}</td>
                  <td>{deltaText(p.weightDelta, ' kg')}</td>
                  <td>{p.firstReps != null && p.lastReps != null ? `${p.firstReps} → ${p.lastReps}` : <span className="text-muted">—</span>}</td>
                  <td>{deltaText(p.repsDelta, '')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={{ display: 'flex', justifyContent: 'center' }}>
        <Button variant="dark" size="sm" onClick={() => setShowHistory((v) => !v)}>
          {showHistory ? 'Ẩn lịch sử chi tiết' : 'Xem lịch sử chi tiết'}
        </Button>
      </div>

      {showHistory && (
        <>
      {/* Pagination */}
      {!detailSessionId && sessions.length > 0 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12 }}>
          <Button variant="dark" size="sm" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
            ← Trang trước
          </Button>
          <span className="text-secondary text-sm">
            Trang {page + 1} / {totalPages}
          </span>
          <Button variant="dark" size="sm" onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>
            Trang sau →
          </Button>
        </div>
      )}

      {/* Session detail panel */}
      {detailSessionId && (
        <div className="card animate-slide-up">
          <h2 className="section-title" style={{ marginBottom: 16 }}>Chi tiết buổi tập #{detailSessionId}</h2>
          {!detail ? (
            <p className="text-secondary text-sm">Đang tải…</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Hiệp</th>
                  <th>Số lần</th>
                  <th>Tạ (kg)</th>
                  <th>Nghỉ (s)</th>
                </tr>
              </thead>
              <tbody>
                {detail.sets.map((set) => (
                  <tr key={set.id}>
                    <td><span className="fw-700">#{set.setNumber}</span></td>
                    <td>{set.repsCompleted ?? <span className="text-muted">—</span>}</td>
                    <td>{set.weightUsed != null ? `${set.weightUsed} kg` : <span className="text-muted">—</span>}</td>
                    <td className="text-secondary">{set.restTimeSeconds ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Sessions list */}
      {!detailSessionId && listLoading && sessions.length === 0 && <Spinner />}
      {!detailSessionId && !listLoading && (
        sessions.length === 0 && !error ? (
          <div className="card">
            <div className="empty-state">
              <div className="empty-state-icon"><Icon name="strength" size={42} /></div>
              <p className="empty-state-text">Chưa có buổi tập nào. Hãy bắt đầu buổi tập đầu tiên!</p>
            </div>
          </div>
        ) : (
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Hiệp</th>
                  <th>Volume</th>
                  <th>Phân tâm</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <div className="fw-600" style={{ fontSize: 14 }}>
                        {new Date(s.startTime).toLocaleDateString('vi-VN')}
                      </div>
                      <div className="text-secondary" style={{ fontSize: 12 }}>
                        {new Date(s.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td>{s.totalSets} hiệp</td>
                    <td className="fw-600">{s.totalVolumeKg} kg</td>
                    <td>
                      {s.focusInterruptionsCount > 0
                        ? <span className="text-warning">{s.focusInterruptionsCount}×</span>
                        : <span className="text-muted">—</span>}
                    </td>
                    <td>{statusBadge(s.status)}</td>
                    <td>
                      <Button variant="dark" size="sm" onClick={() => openDetail(s.id)}>
                        Chi tiết
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
        </>
      )}
    </div>
  );
}
