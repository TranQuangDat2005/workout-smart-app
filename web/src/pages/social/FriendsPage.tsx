import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Modal from '../../components/Modal';
import Spinner from '../../components/Spinner';
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
  const [unfriendTarget, setUnfriendTarget] = useState<FriendItem | null>(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    void Promise.allSettled([
      socialApi.friends().then(setFriends).catch(() => undefined),
      socialApi.pendingRequests().then(setPending).catch(() => undefined),
    ]).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const search = () => {
    socialApi.searchUsers(query).then(setResults).catch(() => setError('Tìm kiếm thất bại'));
  };

  const sendRequest = async (userId: number) => {
    setError(''); setNotice('');
    try {
      const res = await socialApi.sendFriendRequest(userId);
      setNotice(res.status === 'accepted' ? 'Hai bạn đã trở thành bạn bè!' : 'Đã gửi lời mời kết bạn.');
      load();
      search();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Gửi lời mời thất bại');
    }
  };

  const accept = async (id: number) => { await socialApi.accept(id); load(); search(); };
  const reject = async (id: number) => { await socialApi.reject(id); load(); search(); };

  /** FR-WITHDRAW (018): rút lại lời mời kết bạn đã gửi — chỉ người gửi mới được phép. */
  const withdraw = async (id: number) => {
    setError(''); setNotice('');
    try {
      await socialApi.unfriend(id);
      setNotice('Đã rút lại lời mời kết bạn.');
      load();
      search();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Rút lời mời thất bại');
    }
  };

  /** FR-004: nút hành động theo trạng thái quan hệ của từng kết quả tìm kiếm. */
  const renderSearchAction = (u: UserSearchItem) => {
    switch (u.relationshipStatus) {
      case 'accepted':
        return <span className="badge badge-green">Bạn bè</span>;
      case 'pending_sent':
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className="badge badge-info">Đã gửi</span>
            <Button variant="outlined" size="sm" onClick={() => u.friendshipId != null && withdraw(u.friendshipId)}>
              Rút lời mời
            </Button>
          </div>
        );
      case 'pending_received':
        return (
          <div style={{ display: 'flex', gap: 8 }}>
            <Button size="sm" onClick={() => u.friendshipId != null && accept(u.friendshipId)}>Chấp nhận</Button>
            <Button variant="outlined" size="sm" onClick={() => u.friendshipId != null && reject(u.friendshipId)}>Từ chối</Button>
          </div>
        );
      default:
        return (
          <Button variant="primary" size="sm" onClick={() => sendRequest(u.id)}>
            + Kết bạn
          </Button>
        );
    }
  };
  const openUnfriend = (friend: FriendItem) => setUnfriendTarget(friend);

  const confirmUnfriend = async () => {
    if (!unfriendTarget) return;
    await socialApi.unfriend(unfriendTarget.id);
    setUnfriendTarget(null);
    load();
  };

  const UserCard = ({ name, email, action }: { name?: string | null; email?: string | null; action: React.ReactNode }) => (
    <div
      className="card card-hover"
      style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 18px', gap: 12 }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div
          className="avatar avatar-md"
          style={{ background: `hsl(${((name?.charCodeAt(0) ?? 65) * 13) % 360}, 50%, 30%)`, color: 'white' }}
        >
          {(name ?? '?').slice(0, 2).toUpperCase()}
        </div>
        <div>
          <div className="fw-600" style={{ fontSize: 14 }}>{name ?? 'Người dùng'}</div>
          {email && <div className="text-muted" style={{ fontSize: 12 }}>{email}</div>}
        </div>
      </div>
      <div>{action}</div>
    </div>
  );

  return (
    <div className="page-container" style={{ maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <h1><Icon name="users" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Bạn bè</h1>

      {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
      {error   && <div className="notice notice-error">{error}</div>}
      {loading && friends.length === 0 && <Spinner />}

      {/* Search */}
      <div className="card">
        <h2 className="section-title" style={{ marginBottom: 14 }}>Tìm kiếm người dùng</h2>
        <div style={{ display: 'flex', gap: 10 }}>
          <div style={{ flex: 1 }}>
            <TextField
              label="Tên hoặc email"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && search()}
              placeholder="Nhập tên hoặc email..."
            />
          </div>
          <Button variant="dark" onClick={search} style={{ marginTop: 'auto' }}>Tìm</Button>
        </div>
        {results.length > 0 && (
          <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {results.map((u) => (
              <UserCard
                key={u.id}
                name={u.displayName}
                email={u.email}
                action={renderSearchAction(u)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Pending requests */}
      {pending.length > 0 && (
        <div>
          <h2 className="section-title" style={{ marginBottom: 12 }}>
            Lời mời đang chờ
            <span className="badge badge-info" style={{ marginLeft: 10 }}>{pending.length}</span>
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {pending.map((p) => (
              <UserCard
                key={p.id}
                name={p.displayName}
                action={
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Button size="sm" onClick={() => accept(p.id)}>Chấp nhận</Button>
                    <Button variant="outlined" size="sm" onClick={() => reject(p.id)}>Từ chối</Button>
                  </div>
                }
              />
            ))}
          </div>
        </div>
      )}

      {/* Friends list */}
      <div>
        <h2 className="section-title" style={{ marginBottom: 12 }}>
          Bạn bè của tôi
          {friends.length > 0 && <span className="badge badge-neutral" style={{ marginLeft: 10 }}>{friends.length}</span>}
        </h2>
        {friends.length === 0 ? (
          <div className="card">
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <div className="empty-state-icon"><Icon name="wave" size={42} /></div>
              <p className="empty-state-text">Chưa có bạn bè nào. Hãy kết nối với cộng đồng!</p>
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {friends.map((f) => (
              <UserCard
                key={f.id}
                name={f.displayName}
                action={
                  <Button variant="danger" size="sm" onClick={() => openUnfriend(f)}>
                    Hủy kết bạn
                  </Button>
                }
              />
            ))}
          </div>
        )}
      </div>

      <Modal
        open={unfriendTarget != null}
        title="Hủy kết bạn"
        onClose={() => setUnfriendTarget(null)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setUnfriendTarget(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => void confirmUnfriend()}>Hủy kết bạn</Button>
          </>
        }
      >
        <p className="text-secondary text-sm" style={{ lineHeight: 1.7 }}>
          Hủy kết bạn với {unfriendTarget?.displayName ?? 'người này'}? Hai bạn sẽ không còn thấy hoạt động của nhau trên bảng tin.
        </p>
      </Modal>
    </div>
  );
}
