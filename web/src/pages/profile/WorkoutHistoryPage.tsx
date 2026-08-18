import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Spinner from '../../components/Spinner';
import { profileApi } from '../../services/profileApi';
import type { WorkoutSessionDetail, WorkoutSessionItem } from '../../services/profileApi';

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

  return (
    <div className="page-container" style={{ maxWidth: 800, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1>📋 Lịch sử buổi tập</h1>
        {detailSessionId && (
          <Button variant="outlined" size="sm" onClick={closeDetail}>
            ← Quay lại danh sách
          </Button>
        )}
      </div>

      {error && <div className="notice notice-error">{error}</div>}

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
              <div className="empty-state-icon">🏋️</div>
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
    </div>
  );
}
