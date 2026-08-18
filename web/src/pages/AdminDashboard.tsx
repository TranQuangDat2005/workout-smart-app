import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../services/adminApi';
import type { AdminExercise, AdminUser } from '../services/adminApi';

const cardStyle: CSSProperties = {
  background: 'var(--dark-surface)',
  borderRadius: 12,
  padding: 20,
  flex: '1 1 200px',
  minWidth: 200,
  textAlign: 'center',
};

const actionStyle: CSSProperties = {
  background: 'var(--dark-surface)',
  color: 'var(--text-base)',
  borderRadius: 9999,
  padding: '12px 20px',
  fontSize: 14,
  fontWeight: 700,
  textDecoration: 'none',
  border: '1px solid var(--text-announcement)',
};

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
    <div style={{ width: '100%', maxWidth: 960, display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div>
        <h1 style={{ fontSize: 26, fontWeight: 700 }}>Bảng điều khiển quản trị</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>
          Quản lý người dùng và thư viện bài tập của WorkoutSmartApp.
        </p>
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700 }}>{users.length}</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Người dùng (hiển thị tối đa 50)
          </div>
        </div>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--text-negative)' }}>{bannedCount}</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Tài khoản bị khóa
          </div>
        </div>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--green)' }}>{exercises.length}</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Bài tập (hiển thị tối đa 200)
          </div>
        </div>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700 }}>{activeExercises}</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Bài tập đang hoạt động
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <Link to="/admin/users" style={actionStyle}>👤 Quản lý người dùng</Link>
        <Link to="/admin/exercises" style={actionStyle}>🏋️ Quản lý bài tập</Link>
      </div>

      <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
        <div style={{ background: 'var(--dark-surface)', borderRadius: 12, padding: 20, flex: '1 1 320px' }}>
          <h2 style={{ fontSize: 18, fontWeight: 700 }}>Người dùng gần đây</h2>
          {users.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)' }}>Chưa có người dùng.</p>
          ) : (
            users.slice(0, 5).map((u) => (
              <div key={u.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--mid-dark)' }}>
                <span>{u.email}</span>
                <span style={{ color: u.accountStatus === 'BANNED' ? 'var(--text-negative)' : 'var(--text-secondary)' }}>
                  {u.accountStatus}
                </span>
              </div>
            ))
          )}
        </div>
        <div style={{ background: 'var(--dark-surface)', borderRadius: 12, padding: 20, flex: '1 1 320px' }}>
          <h2 style={{ fontSize: 18, fontWeight: 700 }}>Bài tập gần đây</h2>
          {exercises.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)' }}>Chưa có bài tập.</p>
          ) : (
            exercises.slice(0, 5).map((e) => (
              <div key={e.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--mid-dark)' }}>
                <span>{e.name}</span>
                <span style={{ color: 'var(--text-secondary)' }}>
                  {e.equipment} · {e.status}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
