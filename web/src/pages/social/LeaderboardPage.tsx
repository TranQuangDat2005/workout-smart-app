import { useCallback, useEffect, useState } from 'react';
import Button from '../../components/Button';
import Spinner from '../../components/Spinner';
import { socialApi } from '../../services/socialApi';
import type { Challenge, LeaderboardItem } from '../../services/socialApi';
import { profileApi } from '../../services/profileApi';
import { CHALLENGE_STATUS_LABELS, label } from '../../services/labels';

const MEDAL: Record<number, string> = { 1: '🥇', 2: '🥈', 3: '🥉' };

type Scope = 'server' | 'friends';

export default function LeaderboardPage() {
  const [scope, setScope] = useState<Scope>('server');
  const [board, setBoard] = useState<LeaderboardItem[]>([]);
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [myChallenges, setMyChallenges] = useState<Challenge[]>([]);
  const [myUserId, setMyUserId] = useState<number | null>(null);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const req = scope === 'friends' ? socialApi.friendsLeaderboard() : socialApi.leaderboard();
    void Promise.allSettled([
      req.then(setBoard).catch(() => setError('Không thể tải bảng xếp hạng')),
      socialApi.challenges().then(setChallenges).catch(() => undefined),
      socialApi.myChallenges().then(setMyChallenges).catch(() => undefined),
      profileApi.getProfile().then((p) => setMyUserId(p.id)).catch(() => undefined),
    ]).finally(() => setLoading(false));
  }, [scope]);

  useEffect(() => {
    load();
  }, [load]);

  const join = async (id: number) => {
    setNotice(''); setError('');
    try {
      await socialApi.joinChallenge(id);
      setNotice('Đã tham gia thử thách!');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Tham gia thất bại');
    }
  };

  const myRank = board.find((item) => item.userId === myUserId);

  return (
    <div className="page-container" style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1>🏆 Bảng xếp hạng Streak</h1>
      </div>

      {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
      {error && <div className="notice notice-error">{error}</div>}
      {loading && board.length === 0 && <Spinner />}

      {/* Scope tabs */}
      <div style={{ display: 'flex', gap: 8 }}>
        {([
          { key: 'server', label: 'Toàn server' },
          { key: 'friends', label: 'Nhóm bạn bè' },
        ] as { key: Scope; label: string }[]).map((s) => (
          <button
            key={s.key}
            className={`btn btn-sm ${scope === s.key ? 'btn-primary' : 'btn-dark'}`}
            onClick={() => setScope(s.key)}
          >
            {s.label}
          </button>
        ))}
      </div>

      {/* Own position pin */}
      {myRank && (
        <div className="card" style={{ padding: '12px 18px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="badge badge-green">Vị trí của bạn</span>
          <span className="fw-700">#{myRank.rank}</span>
          <span className="text-secondary">{myRank.displayName}</span>
          <span className="text-green fw-700" style={{ marginLeft: 'auto' }}>{myRank.currentStreakWeeks} tuần</span>
        </div>
      )}

      {/* Leaderboard table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {board.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📭</div>
            <p className="empty-state-text">Chưa có dữ liệu xếp hạng.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: 60 }}>Hạng</th>
                <th>Người dùng</th>
                <th>Streak hiện tại</th>
                <th>Kỷ lục</th>
              </tr>
            </thead>
            <tbody>
              {board.map((item) => (
                <tr
                  key={item.userId}
                  style={item.rank <= 3 ? { background: 'rgba(30,215,96,0.03)' } : undefined}
                  className={item.userId === myUserId ? 'leaderboard-me' : undefined}
                >
                  <td>
                    {item.rank <= 3 ? (
                      <span style={{ fontSize: 20 }}>{MEDAL[item.rank]}</span>
                    ) : (
                      <span className="fw-700 text-secondary">#{item.rank}</span>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div
                        className="avatar avatar-sm"
                        style={{ background: `hsl(${(item.userId * 47) % 360}, 60%, 30%)`, color: 'white', fontSize: 11 }}
                      >
                        {item.displayName.slice(0, 2).toUpperCase()}
                      </div>
                      <span className="fw-600">{item.displayName}</span>
                      {item.userId === myUserId && <span className="badge badge-green">Bạn</span>}
                    </div>
                  </td>
                  <td>
                    <span className="text-green fw-700">{item.currentStreakWeeks}</span>
                    <span className="text-muted text-sm"> tuần</span>
                  </td>
                  <td className="text-secondary">{item.longestStreakWeeks} tuần</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Open challenges */}
      <div>
        <h2 className="section-title" style={{ marginBottom: 12 }}>⚡ Thử thách đang mở</h2>
        {challenges.length === 0 ? (
          <div className="card">
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <p className="empty-state-text">Chưa có thử thách nào đang mở.</p>
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {challenges.map((c) => (
              <div
                key={c.id}
                className="card card-hover"
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px' }}
              >
                <div>
                  <div className="fw-600" style={{ marginBottom: 4 }}>{c.name}</div>
                  <div className="text-secondary text-sm">
                    {c.durationDays} ngày
                    {c.startDate && ` · từ ${c.startDate}`}
                    {c.endDate && ` đến ${c.endDate}`}
                  </div>
                </div>
                <Button
                  variant={c.joined ? 'dark' : 'primary'}
                  size="sm"
                  onClick={() => !c.joined && join(c.id)}
                  disabled={c.joined}
                >
                  {c.joined ? '✓ Đã tham gia' : 'Tham gia'}
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* My challenges */}
      {myChallenges.length > 0 && (
        <div>
          <h2 className="section-title" style={{ marginBottom: 12 }}>📌 Thử thách của tôi</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {myChallenges.map((c) => (
              <div key={c.id} className="card card-sm" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <span className="fw-600">{c.name}</span>
                  <span className="text-muted text-sm"> · {c.durationDays} ngày</span>
                </div>
                <span className={`badge ${c.status === 'finished' || c.status === 'completed' ? 'badge-green' : c.status === 'open' || c.status === 'active' ? 'badge-info' : 'badge-neutral'}`}>
                  {label(CHALLENGE_STATUS_LABELS, c.status)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
