import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from '../components/Icon';
import { adminApi } from '../services/adminApi';
import type { AdminExercise, AdminUser } from '../services/adminApi';

/** Dashboard Admin — tổng quan người dùng, bài tập + hành động quản trị. */
export default function AdminDashboard() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [exercises, setExercises] = useState<AdminExercise[]>([]);

  useEffect(() => {
    adminApi.searchUsers('').then(setUsers).catch(() => {});
    adminApi.listExercises('').then(setExercises).catch(() => {});
  }, []);

  const bannedCount = users.filter((u) => u.accountStatus === 'BANNED').length;
  const activeExercises = exercises.filter((e) => e.status === 'active').length;

  return (
    <div className="page-container" style={{ maxWidth: 960, display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div className="page-header">
        <div>
          <h1><Icon name="shield" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Bảng điều khiển quản trị</h1>
          <p className="text-secondary text-sm" style={{ marginTop: 4 }}>
            Quản lý người dùng và thư viện bài tập WorkoutSmart.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Link to="/admin/users" className="btn btn-outlined btn-sm">Quản lý người dùng</Link>
          <Link to="/admin/exercises" className="btn btn-primary btn-sm">Quản lý bài tập</Link>
        </div>
      </div>

      {/* Stat cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12 }}>
        <div className="stat-card">
          <div className="stat-icon stat-icon-blue"><Icon name="users" /></div>
          <div className="stat-value">{users.length}</div>
          <div className="stat-label">Người dùng</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon stat-icon-red"><Icon name="block" /></div>
          <div className="stat-value text-negative">{bannedCount}</div>
          <div className="stat-label">Tài khoản bị khóa</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon stat-icon-green"><Icon name="strength" /></div>
          <div className="stat-value text-green">{activeExercises}</div>
          <div className="stat-label">Bài tập đang hoạt động</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon stat-icon-orange"><Icon name="box" /></div>
          <div className="stat-value">{exercises.length}</div>
          <div className="stat-label">Tổng bài tập</div>
        </div>
      </div>

      {/* Tables */}
      <div className="grid-two" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {/* Recent users */}
        <div className="card">
          <div className="section-header">
            <h2 className="section-title">Người dùng gần đây</h2>
            <Link to="/admin/users" className="section-link">Xem tất cả →</Link>
          </div>
          {users.length === 0 ? (
            <div className="empty-state" style={{ padding: '20px 0' }}>
              <p className="empty-state-text">Chưa có người dùng.</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>Email</th><th>Trạng thái</th></tr>
              </thead>
              <tbody>
                {users.slice(0, 5).map((u) => (
                  <tr key={u.id}>
                    <td className="truncate" style={{ maxWidth: 160 }}>{u.email}</td>
                    <td>
                      <span className={`badge ${u.accountStatus === 'BANNED' ? 'badge-negative' : u.accountStatus === 'ACTIVE' ? 'badge-green' : 'badge-neutral'}`}>
                        {u.accountStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Recent exercises */}
        <div className="card">
          <div className="section-header">
            <h2 className="section-title">Bài tập gần đây</h2>
            <Link to="/admin/exercises" className="section-link">Xem tất cả →</Link>
          </div>
          {exercises.length === 0 ? (
            <div className="empty-state" style={{ padding: '20px 0' }}>
              <p className="empty-state-text">Chưa có bài tập.</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>Tên</th><th>Trạng thái</th></tr>
              </thead>
              <tbody>
                {exercises.slice(0, 5).map((e) => (
                  <tr key={e.id}>
                    <td className="truncate" style={{ maxWidth: 160 }}>{e.name}</td>
                    <td>
                      <span className={`badge ${e.status === 'active' ? 'badge-green' : 'badge-neutral'}`}>
                        {e.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
