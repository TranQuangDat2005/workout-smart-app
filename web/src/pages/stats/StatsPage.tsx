import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
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
  const w = 640; const h = 200;
  if (points.length === 0) return null;
  const values = points.map((p) => p.weightKg);
  const min = Math.min(...values); const max = Math.max(...values);
  const span = max - min || 1;
  const coords = points.map((p, i) => ({
    x: points.length === 1 ? w / 2 : (i / (points.length - 1)) * (w - 40) + 20,
    y: h - 20 - ((p.weightKg - min) / span) * (h - 40),
  }));
  const path = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x},${c.y}`).join(' ');
  const area = `${path} L${coords[coords.length - 1].x},${h} L${coords[0].x},${h} Z`;
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }}>
      <defs>
        <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--green)" stopOpacity="0.2" />
          <stop offset="100%" stopColor="var(--green)" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill="url(#areaGrad)" />
      <path d={path} fill="none" stroke="var(--green)" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
      {coords.map((c, i) => (
        <g key={i}>
          <circle cx={c.x} cy={c.y} r={4} fill="var(--near-black)" stroke="var(--green)" strokeWidth={2} />
          <text x={c.x} y={c.y - 10} fontSize={10} fill="var(--text-secondary)" textAnchor="middle">{points[i].weightKg}</text>
        </g>
      ))}
    </svg>
  );
}

function BarChart({ points, color }: { points: VolumePoint[]; color: string }) {
  const w = 640; const h = 200;
  if (points.length === 0) return null;
  const max = Math.max(...points.map((p) => p.totalKg)) || 1;
  const barW = Math.min(40, (w - 40) / points.length - 8);
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }}>
      {points.map((p, i) => {
        const x = 20 + i * ((w - 40) / points.length) + ((w - 40) / points.length - barW) / 2;
        const bh = (p.totalKg / max) * (h - 50);
        return (
          <g key={i}>
            <rect x={x} y={h - 30 - bh} width={barW} height={bh} rx={4} fill={color} opacity={0.85} />
            <text x={x + barW / 2} y={h - 34 - bh} fontSize={10} fill="var(--text-secondary)" textAnchor="middle">{p.totalKg}</text>
            <text x={x + barW / 2} y={h - 10} fontSize={9} fill="var(--text-muted)" textAnchor="middle">{p.weekStart.slice(5)}</text>
          </g>
        );
      })}
    </svg>
  );
}

function CalorieChart({ points }: { points: CaloriePoint[] }) {
  const w = 640; const h = 200;
  if (points.length === 0) return null;
  const max = Math.max(...points.map((p) => Math.max(p.caloriesIn, p.caloriesBurned))) || 1;
  const groupW = (w - 40) / points.length;
  const barW = Math.min(8, groupW / 3);
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }}>
      {points.map((p, i) => {
        const cx = 20 + i * groupW + groupW / 2;
        const inH = (p.caloriesIn / max) * (h - 50);
        const outH = (p.caloriesBurned / max) * (h - 50);
        return (
          <g key={i}>
            <rect x={cx - barW - 2} y={h - 30 - inH} width={barW} height={inH} rx={2} fill="var(--green)" opacity={0.85} />
            <rect x={cx + 2} y={h - 30 - outH} width={barW} height={outH} rx={2} fill="var(--text-announcement)" opacity={0.85} />
            {i % 5 === 0 && (
              <text x={cx} y={h - 10} fontSize={9} fill="var(--text-muted)" textAnchor="middle">{p.date.slice(5)}</text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

function EmptyHint({ text }: { text: string }) {
  return (
    <div className="empty-state" style={{ padding: '32px 0' }}>
      <div className="empty-state-icon"><Icon name="inbox" size={42} /></div>
      <p className="empty-state-text">{text}</p>
    </div>
  );
}

export default function StatsPage() {
  const [range, setRange] = useState<RangeKey>('30');
  const [customFrom, setCustomFrom] = useState(iso(new Date(Date.now() - 30 * 86400000)));
  const [customTo, setCustomTo] = useState(iso(new Date()));
  const [data, setData] = useState<StatsDashboard | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = (key: RangeKey, from?: string, to?: string) => {
    const { from: f, to: t } = key === 'custom' && from && to ? { from, to } : rangeDates(key);
    setLoading(true);
    setError('');
    statsApi
      .dashboard(f, t)
      .then(setData)
      .catch((err: unknown) => {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
        setError(msg ?? 'Không thể tải thống kê');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    setError('');
    load(range, customFrom, customTo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [range]);

  const empty = (list: unknown[] | undefined) => !list || list.length === 0;

  const dateInputStyle: React.CSSProperties = {
    background: 'var(--mid-dark)',
    color: 'var(--text-base)',
    border: '1px solid var(--border-dark)',
    borderRadius: 8,
    padding: '9px 12px',
    fontSize: 13,
    outline: 'none',
  };

  return (
    <div className="page-container" style={{ maxWidth: 860, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1><Icon name="chart" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Thống kê tiến độ</h1>
      </div>

      {/* Period selector */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        {(['7', '30', '90'] as RangeKey[]).map((k) => (
          <button
            key={k}
            className={`btn btn-sm ${range === k ? 'btn-primary' : 'btn-dark'}`}
            onClick={() => setRange(k)}
          >
            {k} ngày
          </button>
        ))}
        <button
          className={`btn btn-sm ${range === 'custom' ? 'btn-primary' : 'btn-dark'}`}
          onClick={() => setRange('custom')}
        >
          Tùy chọn
        </button>
        {range === 'custom' && (
          <>
            <input type="date" value={customFrom} onChange={(e) => setCustomFrom(e.target.value)} style={dateInputStyle} />
            <span className="text-muted" style={{ fontSize: 12 }}>→</span>
            <input type="date" value={customTo} onChange={(e) => setCustomTo(e.target.value)} style={dateInputStyle} />
            <Button variant="dark" size="sm" onClick={() => load('custom', customFrom, customTo)}>
              Áp dụng
            </Button>
          </>
        )}
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      {loading && !data && (
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 60 }}>
          <div className="btn-spinner" style={{ width: 28, height: 28, borderWidth: 3, color: 'var(--green)' }} aria-label="Đang tải" />
        </div>
      )}

      {data && (
        <>
          {/* Streak + Plan */}
          <div className="grid-two" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <div className="card">
              <h2 className="section-title" style={{ marginBottom: 14 }}><Icon name="flame" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Streak</h2>
              <div style={{ display: 'flex', gap: 12 }}>
                <div style={{ flex: 1, background: 'var(--mid-dark)', borderRadius: 8, padding: '14px 18px' }}>
                  <div style={{ fontSize: 26, fontWeight: 700, color: 'var(--green)' }}>{data.streak.currentStreakWeeks}</div>
                  <div className="stat-label">tuần hiện tại</div>
                </div>
                <div style={{ flex: 1, background: 'var(--mid-dark)', borderRadius: 8, padding: '14px 18px' }}>
                  <div style={{ fontSize: 26, fontWeight: 700 }}>{data.streak.longestStreakWeeks}</div>
                  <div className="stat-label">tuần kỷ lục</div>
                </div>
              </div>
            </div>
            <div className="card">
              <h2 className="section-title" style={{ marginBottom: 14 }}><Icon name="check" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Hoàn thành lộ trình</h2>
              {data.planCompletion.plannedDays === 0 ? (
                <p className="text-secondary text-sm">Chưa có kế hoạch tập nào.</p>
              ) : (
                <>
                  <div style={{ fontSize: 14, marginBottom: 10 }}>
                    {data.planCompletion.completedSessions}/{data.planCompletion.plannedDays} buổi
                    <span className="text-green fw-700" style={{ marginLeft: 8 }}>{data.planCompletion.completionPct}%</span>
                  </div>
                  <div className="progress-bar">
                    <div className="progress-fill" style={{ width: `${Math.min(100, data.planCompletion.completionPct)}%` }} />
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Charts */}
          <div className="card">
            <h2 className="section-title" style={{ marginBottom: 14 }}><Icon name="scale" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Cân nặng theo thời gian</h2>
            {empty(data.weight)
              ? <EmptyHint text="Chưa có chỉ số cơ thể. Cập nhật cân nặng để xem biểu đồ." />
              : <LineChart points={data.weight} />}
          </div>

          <div className="card">
            <h2 className="section-title" style={{ marginBottom: 14 }}><Icon name="strength" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Volume (kg nâng / tuần)</h2>
            {empty(data.volume)
              ? <EmptyHint text="Chưa có buổi tập hoàn thành trong khoảng này." />
              : <BarChart points={data.volume} color="var(--green)" />}
          </div>

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
              <h2 className="section-title"><Icon name="apple" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Calo nạp vs. tiêu thụ</h2>
              <div style={{ display: 'flex', gap: 14, fontSize: 12 }}>
                <span className="text-green">■ Nạp</span>
                <span className="text-info">■ Tiêu thụ</span>
              </div>
            </div>
            {empty(data.calories) || data.calories.every((c) => c.caloriesIn === 0 && c.caloriesBurned === 0)
              ? <EmptyHint text="Chưa có dữ liệu calo. Ghi nhận bữa ăn để so sánh." />
              : <CalorieChart points={data.calories} />}
          </div>
        </>
      )}
    </div>
  );
}
