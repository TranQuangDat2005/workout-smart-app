import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import { statsApi } from '../../services/statsApi';
import type { CaloriePoint, StatsDashboard, VolumePoint, WeightPoint } from '../../services/statsApi';

type RangeKey = '7' | '30' | '90' | 'custom';

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function rangeDates(key: RangeKey): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setDate(to.getDate() - Number(key));
  return { from: iso(from), to: iso(to) };
}

function LineChart({ points }: { points: WeightPoint[] }) {
  const w = 640;
  const h = 220;
  if (points.length === 0) return null;
  const values = points.map((p) => p.weightKg);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const coords = points.map((p, i) => ({
    x: points.length === 1 ? w / 2 : (i / (points.length - 1)) * (w - 40) + 20,
    y: h - 20 - ((p.weightKg - min) / span) * (h - 40),
  }));
  const path = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x},${c.y}`).join(' ');
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }}>
      <path d={path} fill="none" stroke="var(--green)" strokeWidth={2.5} />
      {coords.map((c, i) => (
        <circle key={i} cx={c.x} cy={c.y} r={3.5} fill="var(--green)" />
      ))}
      {coords.map((c, i) => (
        <text key={i} x={c.x} y={c.y - 8} fontSize={10} fill="var(--text-secondary)" textAnchor="middle">
          {points[i].weightKg}
        </text>
      ))}
    </svg>
  );
}

function BarChart({ points, color }: { points: VolumePoint[]; color: string }) {
  const w = 640;
  const h = 220;
  if (points.length === 0) return null;
  const max = Math.max(...points.map((p) => p.totalKg)) || 1;
  const barW = Math.min(48, (w - 40) / points.length - 8);
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }}>
      {points.map((p, i) => {
        const x = 20 + i * ((w - 40) / points.length) + ((w - 40) / points.length - barW) / 2;
        const bh = (p.totalKg / max) * (h - 50);
        return (
          <g key={i}>
            <rect x={x} y={h - 30 - bh} width={barW} height={bh} rx={4} fill={color} />
            <text x={x + barW / 2} y={h - 40 - bh} fontSize={10} fill="var(--text-secondary)" textAnchor="middle">
              {p.totalKg}
            </text>
            <text x={x + barW / 2} y={h - 12} fontSize={10} fill="var(--text-secondary)" textAnchor="middle">
              {p.weekStart.slice(5)}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

function CalorieChart({ points }: { points: CaloriePoint[] }) {
  const w = 640;
  const h = 220;
  if (points.length === 0) return null;
  const max = Math.max(...points.map((p) => Math.max(p.caloriesIn, p.caloriesBurned))) || 1;
  const groupW = (w - 40) / points.length;
  const barW = Math.min(10, groupW / 3);
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }}>
      {points.map((p, i) => {
        const cx = 20 + i * groupW + groupW / 2;
        const inH = (p.caloriesIn / max) * (h - 50);
        const outH = (p.caloriesBurned / max) * (h - 50);
        return (
          <g key={i}>
            <rect x={cx - barW - 2} y={h - 30 - inH} width={barW} height={inH} rx={2} fill="var(--green)" />
            <rect x={cx + 2} y={h - 30 - outH} width={barW} height={outH} rx={2} fill="var(--text-announcement)" />
            {i % 5 === 0 && (
              <text x={cx} y={h - 12} fontSize={9} fill="var(--text-secondary)" textAnchor="middle">
                {p.date.slice(5)}
              </text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

export default function StatsPage() {
  const [range, setRange] = useState<RangeKey>('30');
  const [customFrom, setCustomFrom] = useState(iso(new Date(Date.now() - 30 * 86400000)));
  const [customTo, setCustomTo] = useState(iso(new Date()));
  const [data, setData] = useState<StatsDashboard | null>(null);
  const [error, setError] = useState('');

  const load = (key: RangeKey, from?: string, to?: string) => {
    const { from: f, to: t } = key === 'custom' && from && to ? { from, to } : rangeDates(key);
    statsApi
      .dashboard(f, t)
      .then(setData)
      .catch((err: unknown) => {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
        setError(msg ?? 'Không thể tải thống kê');
      });
  };

  useEffect(() => {
    setError('');
    load(range, customFrom, customTo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [range]);

  const pick = (key: RangeKey) => {
    setRange(key);
    if (key === 'custom') {
      load('custom', customFrom, customTo);
    } else {
      load(key);
    }
  };

  const empty = (list: unknown[] | undefined) => !list || list.length === 0;

  return (
    <div style={{  }}>
      <div style={{ maxWidth: 860, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Thống kê tiến độ</h1>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          {(['7', '30', '90'] as RangeKey[]).map((k) => (
            <Button key={k} variant={range === k ? 'primary' : 'dark'} onClick={() => pick(k)}>
              {k} ngày
            </Button>
          ))}
          <Button variant={range === 'custom' ? 'primary' : 'dark'} onClick={() => pick('custom')}>
            Tùy chọn
          </Button>
          {range === 'custom' && (
            <>
              <input
                type="date"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
                style={dateInputStyle}
              />
              <input
                type="date"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
                style={dateInputStyle}
              />
              <Button variant="dark" onClick={() => load('custom', customFrom, customTo)}>
                Áp dụng
              </Button>
            </>
          )}
        </div>

        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}

        {data && (
          <>
            <section style={cardStyle}>
              <h2 style={sectionTitle}>Streak</h2>
              <div style={{ display: 'flex', gap: 16 }}>
                <div style={statBox}>
                  <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--green)' }}>
                    {data.streak.currentStreakWeeks}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>tuần hiện tại</div>
                </div>
                <div style={statBox}>
                  <div style={{ fontSize: 28, fontWeight: 700 }}>{data.streak.longestStreakWeeks}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>tuần dài nhất</div>
                </div>
              </div>
            </section>

            <section style={cardStyle}>
              <h2 style={sectionTitle}>Hoàn thành kế hoạch</h2>
              {data.planCompletion.plannedDays === 0 ? (
                <EmptyHint text="Chưa có kế hoạch tập nào. Hãy tạo lộ trình để theo dõi tiến độ." />
              ) : (
                <>
                  <div style={{ fontSize: 14, marginBottom: 8 }}>
                    {data.planCompletion.completedSessions} / {data.planCompletion.plannedDays} buổi —{' '}
                    <b style={{ color: 'var(--green)' }}>{data.planCompletion.completionPct}%</b>
                  </div>
                  <div style={{ height: 8, background: 'var(--mid-dark)', borderRadius: 9999 }}>
                    <div
                      style={{
                        width: `${Math.min(100, data.planCompletion.completionPct)}%`,
                        height: '100%',
                        background: 'var(--green)',
                        borderRadius: 9999,
                      }}
                    />
                  </div>
                </>
              )}
            </section>

            <section style={cardStyle}>
              <h2 style={sectionTitle}>Cân nặng</h2>
              {empty(data.weight) ? (
                <EmptyHint text="Chưa có chỉ số cơ thể. Cập nhật cân nặng để xem biểu đồ." />
              ) : (
                <LineChart points={data.weight} />
              )}
            </section>

            <section style={cardStyle}>
              <h2 style={sectionTitle}>Volume (kg nâng / tuần)</h2>
              {empty(data.volume) ? (
                <EmptyHint text="Chưa có buổi tập hoàn thành nào trong khoảng thời gian này." />
              ) : (
                <BarChart points={data.volume} color="var(--green)" />
              )}
            </section>

            <section style={cardStyle}>
              <h2 style={sectionTitle}>Calo nạp vs. tiêu thụ</h2>
              <div style={{ display: 'flex', gap: 16, marginBottom: 8, fontSize: 12 }}>
                <span style={{ color: 'var(--green)' }}>■ Nạp</span>
                <span style={{ color: 'var(--text-announcement)' }}>■ Tiêu thụ</span>
              </div>
              {empty(data.calories) || data.calories.every((c) => c.caloriesIn === 0 && c.caloriesBurned === 0) ? (
                <EmptyHint text="Chưa có dữ liệu calo. Ghi nhận bữa ăn và hoàn thành buổi tập để so sánh." />
              ) : (
                <CalorieChart points={data.calories} />
              )}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

function EmptyHint({ text }: { text: string }) {
  return (
    <div style={{ padding: '24px 16px', textAlign: 'center' }}>
      <p style={{ fontSize: 14, color: 'var(--text-secondary)', margin: 0 }}>{text}</p>
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  background: 'var(--dark-surface)',
  borderRadius: 8,
  padding: 20,
};

const sectionTitle: React.CSSProperties = {
  fontSize: 18,
  fontWeight: 600,
  margin: '0 0 12px',
};

const statBox: React.CSSProperties = {
  background: 'var(--mid-dark)',
  borderRadius: 6,
  padding: '14px 20px',
  minWidth: 120,
};

const dateInputStyle: React.CSSProperties = {
  background: 'var(--mid-dark)',
  color: 'var(--text-base)',
  border: 'none',
  borderRadius: 6,
  padding: '8px 10px',
  fontSize: 14,
};
