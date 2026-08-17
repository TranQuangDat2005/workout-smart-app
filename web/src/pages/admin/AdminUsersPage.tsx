import { useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { adminApi } from '../../services/adminApi';
import type { AdminUser } from '../../services/adminApi';

export default function AdminUsersPage() {
  const [q, setQ] = useState('');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState('');

  const onSearch = async () => {
    setError('');
    try {
      setUsers(await adminApi.searchUsers(q));
    } catch {
      setError('Không thể tìm kiếm');
    }
  };

  const onBan = async (user: AdminUser) => {
    const reason = window.prompt(`Lý do khóa ${user.email}:`);
    if (!reason) return;
    try {
      await adminApi.banUser(user.id, reason);
      onSearch();
    } catch {
      setError('Khóa thất bại');
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

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32 }}>
      <h1 style={{ fontSize: 24, fontWeight: 700 }}>Quản lý người dùng</h1>
      <div style={{ display: 'flex', gap: 8, margin: '16px 0' }}>
        <TextField label="Email / tên / ID" value={q} onChange={(e) => setQ(e.target.value)} />
        <Button onClick={onSearch}>Tìm</Button>
      </div>
      {error && <p style={{ color: 'var(--text-negative)' }}>{error}</p>}
      {users.map((user) => (
        <div key={user.id} style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 12, marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>{user.email}</strong>
            <span style={{ color: 'var(--text-secondary)' }}> · {user.accountStatus}</span>
          </div>
          {user.accountStatus === 'ACTIVE' ? (
            <Button onClick={() => onBan(user)} style={{ color: 'var(--text-negative)' }}>Khóa</Button>
          ) : (
            <Button onClick={() => onUnban(user)}>Mở khóa</Button>
          )}
        </div>
      ))}
    </div>
  );
}
