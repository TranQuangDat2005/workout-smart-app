import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import TextField from '../../components/TextField';
import { adminApi } from '../../services/adminApi';
import type { AdminUser, AdminUserDetail } from '../../services/adminApi';
import { ACCOUNT_STATUS_LABELS, label } from '../../services/labels';

export default function AdminUsersPage() {
  const [q, setQ] = useState('');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [banTarget, setBanTarget] = useState<AdminUser | null>(null);
  const [banReason, setBanReason] = useState('');
  const [banLoading, setBanLoading] = useState(false);
  const [detailUser, setDetailUser] = useState<AdminUser | null>(null);
  const [detail, setDetail] = useState<AdminUserDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const onSearch = async () => {
    setError(''); setLoading(true);
    try {
      setUsers(await adminApi.searchUsers(q));
    } catch {
      setError('Không thể tìm kiếm');
    } finally {
      setLoading(false);
    }
  };

  const openBan = (user: AdminUser) => {
    setBanTarget(user);
    setBanReason('');
  };

  const openDetail = async (user: AdminUser) => {
    setDetailUser(user);
    setDetail(null);
    setDetailLoading(true);
    try {
      setDetail(await adminApi.getUserDetail(user.id));
    } catch {
      setError('Không thể tải chi tiết người dùng');
    } finally {
      setDetailLoading(false);
    }
  };

  const confirmBan = async () => {
    if (!banTarget || !banReason.trim()) {
      setError('Vui lòng nhập lý do khóa.');
      return;
    }
    setBanLoading(true);
    setError('');
    try {
      await adminApi.banUser(banTarget.id, banReason.trim());
      setBanTarget(null);
      await onSearch();
    } catch {
      setError('Khóa thất bại');
    } finally {
      setBanLoading(false);
    }
  };

  const onUnban = async (user: AdminUser) => {
    try {
      await adminApi.unbanUser(user.id);
      onSearch();
    } catch {
      setError('Mở khóa thất bại');
    }
  };

  const statusBadge = (status: string) => {
    const cls = status === 'ACTIVE' || status === 'active' ? 'badge-green' : status === 'BANNED' || status === 'banned' ? 'badge-negative' : 'badge-neutral';
    return <span className={`badge ${cls}`}>{label(ACCOUNT_STATUS_LABELS, status)}</span>;
  };

  return (
    <div className="page-container" style={{ maxWidth: 820, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1>👥 Quản lý người dùng</h1>
        <span className="badge badge-neutral">{users.length > 0 ? `${users.length} kết quả` : ''}</span>
      </div>

      {/* Search */}
      <div className="card" style={{ padding: 18 }}>
        <div style={{ display: 'flex', gap: 10 }}>
          <div style={{ flex: 1 }}>
            <TextField
              label="Tìm theo email / tên / ID"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && onSearch()}
              placeholder="user@email.com, tên hoặc ID..."
            />
          </div>
          <Button variant="dark" onClick={onSearch} loading={loading} style={{ marginTop: 'auto' }}>
            Tìm kiếm
          </Button>
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      {/* Results */}
      {users.length === 0 && !loading ? (
        <div className="card">
          <div className="empty-state" style={{ padding: '24px 0' }}>
            <p className="empty-state-text">Nhập từ khóa và nhấn Tìm kiếm để xem kết quả.</p>
          </div>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Email</th>
                <th>Trạng thái</th>
                <th>Xác thực</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td className="text-muted" style={{ fontSize: 12 }}>#{user.id}</td>
                  <td className="fw-600">{user.email}</td>
                  <td>{statusBadge(user.accountStatus)}</td>
                  <td>
                    <span className={`badge ${user.emailVerified ? 'badge-green' : 'badge-warning'}`}>
                      {user.emailVerified ? 'Đã xác thực' : 'Chưa xác thực'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <Button variant="dark" size="sm" onClick={() => void openDetail(user)}>Chi tiết</Button>
                      {user.accountStatus === 'ACTIVE' || user.accountStatus === 'active' ? (
                        <Button variant="danger" size="sm" onClick={() => openBan(user)}>Khóa</Button>
                      ) : (
                        <Button variant="dark" size="sm" onClick={() => onUnban(user)}>Mở khóa</Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Ban reason modal */}
      <Modal
        open={banTarget != null}
        title={`Khóa tài khoản ${banTarget?.email ?? ''}`}
        onClose={() => setBanTarget(null)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setBanTarget(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => void confirmBan()} loading={banLoading}>Khóa</Button>
          </>
        }
      >
        <TextField
          label="Lý do khóa (bắt buộc)"
          value={banReason}
          onChange={(e) => setBanReason(e.target.value)}
          placeholder="Vi phạm điều khoản, spam..."
        />
      </Modal>

      {/* User detail modal */}
      <Modal
        open={detailUser != null}
        title="Chi tiết người dùng"
        onClose={() => setDetailUser(null)}
      >
        {detailLoading && <p className="text-secondary text-sm">Đang tải…</p>}
        {detail && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, fontSize: 14 }}>
            <div className="row-item">
              <span className="text-secondary">Email</span>
              <span className="fw-600">{detail.profile.email}</span>
            </div>
            <div className="row-item">
              <span className="text-secondary">Tên hiển thị</span>
              <span className="fw-600">{detail.profile.displayName ?? '—'}</span>
            </div>
            <div className="row-item">
              <span className="text-secondary">Trạng thái</span>
              {statusBadge(detail.profile.accountStatus)}
            </div>
            <div className="row-item">
              <span className="text-secondary">Xác thực email</span>
              <span className={`badge ${detail.profile.emailVerified ? 'badge-green' : 'badge-warning'}`}>
                {detail.profile.emailVerified ? 'Đã xác thực' : 'Chưa xác thực'}
              </span>
            </div>
            <div style={{ marginTop: 6 }}>
              <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 }}>Buổi tập gần đây</div>
              {detail.recentSessions.length === 0 ? (
                <p className="text-muted text-sm">Chưa có buổi tập nào.</p>
              ) : (
                detail.recentSessions.slice(0, 5).map((s) => (
                  <div key={s.id} className="row-item" style={{ fontSize: 13 }}>
                    <span className="text-secondary">{new Date(s.startTime).toLocaleDateString('vi-VN')}</span>
                    <span>{s.totalSets} hiệp · {s.totalVolumeKg} kg</span>
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
