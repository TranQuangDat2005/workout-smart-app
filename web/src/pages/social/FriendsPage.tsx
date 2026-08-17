import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { socialApi } from '../../services/socialApi';
import type { FriendItem, UserSearchItem } from '../../services/socialApi';

export default function FriendsPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserSearchItem[]>([]);
  const [friends, setFriends] = useState<FriendItem[]>([]);
  const [pending, setPending] = useState<FriendItem[]>([]);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const load = () => {
    socialApi.friends().then(setFriends).catch(() => undefined);
    socialApi.pendingRequests().then(setPending).catch(() => undefined);
  };

  useEffect(load, []);

  const search = () => {
    socialApi.searchUsers(query).then(setResults).catch(() => setError('Tìm kiếm thất bại'));
  };

  const sendRequest = async (userId: number) => {
    setError(''); setNotice('');
    try {
      const res = await socialApi.sendFriendRequest(userId);
      setNotice(res.status === 'accepted' ? 'Hai bạn đã trở thành bạn bè!' : 'Đã gửi lời mời.');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Gửi lời mời thất bại');
    }
  };

  const accept = async (id: number) => {
    await socialApi.accept(id);
    load();
  };

  const reject = async (id: number) => {
    await socialApi.reject(id);
    load();
  };

  const unfriend = async (id: number, name: string | null) => {
    if (!window.confirm(`Hủy kết bạn với ${name ?? 'người này'}?`)) return;
    await socialApi.unfriend(id);
    load();
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32 }}>
      <div style={{ maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Bạn bè</h1>

        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}

        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <TextField label="Tìm theo tên/email" value={query} onChange={(e) => setQuery(e.target.value)} />
          </div>
          <Button variant="dark" onClick={search}>Tìm</Button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {results.map((u) => (
            <div key={u.id} style={{
              background: 'var(--dark-surface)', borderRadius: 6, padding: '12px 16px',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            }}>
              <div style={{ fontSize: 14 }}>
                <b>{u.displayName ?? 'Người dùng'}</b>
                {u.email && <span style={{ color: 'var(--text-secondary)' }}> · {u.email}</span>}
              </div>
              <Button variant="dark" onClick={() => sendRequest(u.id)}>Kết bạn</Button>
            </div>
          ))}
        </div>

        <h2 style={{ fontSize: 18, fontWeight: 600 }}>Lời mời đang chờ</h2>
        {pending.map((p) => (
          <div key={p.id} style={{
            background: 'var(--dark-surface)', borderRadius: 6, padding: '12px 16px',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <span style={{ fontSize: 14 }}>{p.displayName ?? 'Người dùng'}</span>
            <div style={{ display: 'flex', gap: 8 }}>
              <Button onClick={() => accept(p.id)}>Chấp nhận</Button>
              <Button variant="outlined" onClick={() => reject(p.id)}>Từ chối</Button>
            </div>
          </div>
        ))}
        {pending.length === 0 && <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>Không có lời mời nào.</p>}

        <h2 style={{ fontSize: 18, fontWeight: 600 }}>Danh sách bạn bè</h2>
        {friends.map((f) => (
          <div key={f.id} style={{
            background: 'var(--dark-surface)', borderRadius: 6, padding: '12px 16px',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <span style={{ fontSize: 14 }}>{f.displayName ?? 'Người dùng'}</span>
            <Button variant="outlined" onClick={() => unfriend(f.id, f.displayName)}>Hủy kết bạn</Button>
          </div>
        ))}
        {friends.length === 0 && <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>Chưa có bạn bè.</p>}
      </div>
    </div>
  );
}
