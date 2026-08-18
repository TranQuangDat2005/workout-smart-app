import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import { socialApi } from '../../services/socialApi';
import type { Challenge, LeaderboardItem } from '../../services/socialApi';

export default function LeaderboardPage() {
  const [board, setBoard] = useState<LeaderboardItem[]>([]);
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [myChallenges, setMyChallenges] = useState<Challenge[]>([]);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const load = () => {
    socialApi.leaderboard().then(setBoard).catch(() => setError('Không thể tải bảng xếp hạng'));
    socialApi.challenges().then(setChallenges).catch(() => undefined);
    socialApi.myChallenges().then(setMyChallenges).catch(() => undefined);
  };

  useEffect(load, []);

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

  return (
    <div style={{  }}>
      <div style={{ maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Bảng xếp hạng Streak</h1>
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}

        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead>
            <tr style={{ color: 'var(--text-secondary)', textAlign: 'left' }}>
              <th style={{ padding: '8px 0' }}>Hạng</th>
              <th>Tên</th>
              <th>Streak hiện tại</th>
              <th>Streak dài nhất</th>
            </tr>
          </thead>
          <tbody>
            {board.map((item) => (
              <tr key={item.userId} style={{ borderTop: '1px solid var(--border-dark)' }}>
                <td style={{ padding: '10px 0', fontWeight: 700 }}>#{item.rank}</td>
                <td>{item.displayName}</td>
                <td style={{ color: 'var(--green)' }}>{item.currentStreakWeeks} tuần</td>
                <td>{item.longestStreakWeeks} tuần</td>
              </tr>
            ))}
          </tbody>
        </table>
        {board.length === 0 && <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>Chưa có dữ liệu.</p>}

        <h2 style={{ fontSize: 18, fontWeight: 600, marginTop: 16 }}>Thử thách đang mở</h2>
        {challenges.map((c) => (
          <div key={c.id} style={{
            background: 'var(--dark-surface)', borderRadius: 6, padding: '12px 16px',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <div style={{ fontSize: 14 }}>
              <b>{c.name}</b>
              <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                {c.durationDays} ngày · từ {c.startDate ?? 'ngay'} đến {c.endDate ?? '—'}
              </div>
            </div>
            <Button variant="dark" onClick={() => join(c.id)} disabled={c.joined}>
              {c.joined ? 'Đã tham gia' : 'Tham gia'}
            </Button>
          </div>
        ))}
        {challenges.length === 0 && <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>Chưa có thử thách nào.</p>}

        {myChallenges.length > 0 && (
          <>
            <h2 style={{ fontSize: 18, fontWeight: 600 }}>Thử thách của tôi</h2>
            {myChallenges.map((c) => (
              <div key={c.id} style={{
                background: 'var(--mid-dark)', borderRadius: 6, padding: '10px 16px', fontSize: 14,
              }}>
                {c.name} — {c.durationDays} ngày ({c.status})
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  );
}
