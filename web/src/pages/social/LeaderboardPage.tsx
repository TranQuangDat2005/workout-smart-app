import { useCallback, useEffect, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import type { IconName } from '../../components/Icon';
import Spinner from '../../components/Spinner';
import { socialApi } from '../../services/socialApi';
import type { Challenge, ChallengeResult, LeaderboardItem } from '../../services/socialApi';
import { profileApi } from '../../services/profileApi';
import { CHALLENGE_STATUS_LABELS, label } from '../../services/labels';

const MEDAL: Record<number, { icon: IconName; color: string }> = {
  1: { icon: 'award', color: '#f2c14e' },
  2: { icon: 'award', color: '#c9cdd6' },
  3: { icon: 'award', color: '#d08b53' },
};

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
  const [selectedResults, setSelectedResults] = useState<ChallengeResult[] | null>(null);
  const [selectedChallengeName, setSelectedChallengeName] = useState('');

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

  const loadResults = async (challengeId: number, challengeName: string) => {
    try {
      const results = await socialApi.challengeResults(challengeId);
      setSelectedResults(results);
      setSelectedChallengeName(challengeName);
    } catch {
      setError('Không thể tải kết quả thử thách');
    }
  };

  const myRank = board.find((item) => item.userId === myUserId);
  const viewerRank = board.find((item) => item.viewerRank !== null && item.viewerRank !== undefined)?.viewerRank;

  return (
    <div className="page-container" style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1><Icon name="trophy" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Bảng xếp hạng Streak</h1>
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

      {/* Own position pin — luôn hiển thị khi myUserId có giá trị */}
      {myUserId && (
        <div className="card" style={{ padding: '12px 18px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="badge badge-green">Vị trí của bạn</span>
          <span className="fw-700">#{myRank ? myRank.rank : (viewerRank ?? '?')}</span>
          <span className="text-secondary">{myRank ? myRank.displayName : ''}</span>
          <span className="text-green fw-700" style={{ marginLeft: 'auto' }}>{myRank ? myRank.currentStreakWeeks : ''} {myRank ? 'tuần' : ''}</span>
        </div>
      )}

      {/* Leaderboard table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {board.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon"><Icon name="inbox" size={42} /></div>
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
                      <span><Icon name={MEDAL[item.rank].icon} size={20} style={{ color: MEDAL[item.rank].color }} /></span>
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
      {challenges.filter((c) => c.status === 'open').length > 0 && (
        <div>
          <h2 className="section-title" style={{ marginBottom: 12 }}><Icon name="zap" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Thử thách đang mở</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {challenges.filter((c) => c.status === 'open').map((c) => (
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
                    {c.participantCount > 0 && ` · ${c.participantCount} người tham gia`}
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
        </div>
      )}

      {/* Finished challenges */}
      {challenges.filter((c) => c.status === 'finished').length > 0 && (
        <div>
          <h2 className="section-title" style={{ marginBottom: 12 }}><Icon name="award" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Kết quả thử thách</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {challenges.filter((c) => c.status === 'finished').map((c) => (
              <div
                key={c.id}
                className="card card-hover"
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px' }}
              >
                <div>
                  <div className="fw-600" style={{ marginBottom: 4 }}>{c.name}</div>
                  <div className="text-secondary text-sm">
                    {c.durationDays} ngày · Kết thúc {c.endDate}
                    {c.participantCount > 0 && ` · ${c.participantCount} người tham gia`}
                    {c.joined && c.finalRank != null && (
                      <span className="text-green fw-600"> · Hạng của bạn: #{c.finalRank}</span>
                    )}
                  </div>
                </div>
                <Button
                  variant="dark"
                  size="sm"
                  onClick={() => loadResults(c.id, c.name)}
                >
                  Xem kết quả
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Challenge results detail */}
      {selectedResults && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h2 className="section-title" style={{ margin: 0 }}><Icon name="award" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> {selectedChallengeName}</h2>
            <button className="btn btn-sm btn-dark" onClick={() => setSelectedResults(null)}>Đóng</button>
          </div>
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th style={{ width: 60 }}>Hạng</th>
                  <th>Người dùng</th>
                </tr>
              </thead>
              <tbody>
                {selectedResults.map((r) => (
                  <tr key={r.userId} style={r.finalRank <= 3 ? { background: 'rgba(30,215,96,0.03)' } : undefined}>
                    <td>
                      {r.finalRank <= 3 ? (
                        <span><Icon name={MEDAL[r.finalRank].icon} size={20} style={{ color: MEDAL[r.finalRank].color }} /></span>
                      ) : (
                        <span className="fw-700 text-secondary">#{r.finalRank}</span>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div
                          className="avatar avatar-sm"
                          style={{ background: `hsl(${(r.userId * 47) % 360}, 60%, 30%)`, color: 'white', fontSize: 11 }}
                        >
                          {r.displayName.slice(0, 2).toUpperCase()}
                        </div>
                        <span className="fw-600">{r.displayName}</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* My challenges */}
      {myChallenges.length > 0 && (
        <div>
          <h2 className="section-title" style={{ marginBottom: 12 }}><Icon name="pin" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Thử thách của tôi</h2>
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
