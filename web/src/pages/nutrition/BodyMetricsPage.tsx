import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Spinner from '../../components/Spinner';
import TextField from '../../components/TextField';
import { nutritionApi } from '../../services/nutritionApi';
import type { BodyMetric } from '../../services/nutritionApi';

export default function BodyMetricsPage() {
  const [metrics, setMetrics] = useState<BodyMetric[]>([]);
  const [weight, setWeight] = useState('');
  const [bodyFat, setBodyFat] = useState('');
  const [waist, setWaist] = useState('');
  const [chest, setChest] = useState('');
  const [arm, setArm] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);
  const [listLoading, setListLoading] = useState(true);

  const load = () => nutritionApi.getBodyMetrics().then(setMetrics).catch(() => setError('Không thể tải chỉ số'));
  useEffect(() => {
    setListLoading(true);
    load().finally(() => setListLoading(false));
  }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice(''); setLoading(true);
    try {
      await nutritionApi.createBodyMetric({
        weightKg: Number(weight),
        bodyFatPct: bodyFat ? Number(bodyFat) : undefined,
        waistCm:   waist  ? Number(waist)   : undefined,
        chestCm:   chest  ? Number(chest)   : undefined,
        armCm:     arm    ? Number(arm)     : undefined,
      });
      setNotice('Đã cập nhật chỉ số cơ thể.');
      setWeight(''); setBodyFat(''); setWaist(''); setChest(''); setArm('');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Cập nhật thất bại');
    } finally {
      setLoading(false);
    }
  };

  const latest = metrics[0];

  return (
    <div className="page-container" style={{ maxWidth: 700, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <h1>📐 Chỉ số cơ thể</h1>

      {listLoading && metrics.length === 0 && <Spinner />}

      {/* Latest metrics */}
      {latest && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 10 }}>
          {[
            { label: 'Cân nặng', val: latest.weightKg != null ? `${latest.weightKg} kg` : '—', icon: '⚖️' },
            { label: '% Mỡ',     val: latest.bodyFatPct != null ? `${latest.bodyFatPct}%` : '—', icon: '📊' },
            { label: 'Vòng eo',  val: latest.waistCm != null ? `${latest.waistCm} cm` : '—', icon: '📏' },
            { label: 'Vòng ngực', val: latest.chestCm != null ? `${latest.chestCm} cm` : '—', icon: '💪' },
            { label: 'Vòng tay',  val: latest.armCm != null ? `${latest.armCm} cm` : '—', icon: '🦾' },
          ].map((item) => (
            <div key={item.label} className="stat-card" style={{ padding: '14px 16px' }}>
              <div style={{ fontSize: 20, marginBottom: 6 }}>{item.icon}</div>
              <div style={{ fontSize: 18, fontWeight: 700 }}>{item.val}</div>
              <div className="stat-label">{item.label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Input form */}
      <div className="card">
        <h3 style={{ marginBottom: 16 }}>Ghi nhận chỉ số mới</h3>
        <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <TextField label="Cân nặng (kg) *" type="number" value={weight} onChange={(e) => setWeight(e.target.value)} required placeholder="70.5" step="0.1" min="0" />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <TextField label="% Mỡ" type="number" value={bodyFat} onChange={(e) => setBodyFat(e.target.value)} placeholder="15" step="0.1" min="0" />
            <TextField label="Vòng eo (cm)" type="number" value={waist} onChange={(e) => setWaist(e.target.value)} placeholder="80" min="0" />
            <TextField label="Vòng ngực (cm)" type="number" value={chest} onChange={(e) => setChest(e.target.value)} placeholder="100" min="0" />
            <TextField label="Vòng tay (cm)" type="number" value={arm} onChange={(e) => setArm(e.target.value)} placeholder="35" min="0" />
          </div>

          {notice && <div className="notice notice-success">{notice}</div>}
          {error  && <div className="notice notice-error">{error}</div>}

          <Button type="submit" fullWidth loading={loading}>Cập nhật chỉ số</Button>
        </form>
      </div>

      {/* History */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px 12px', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
          <h3>Lịch sử đo lường</h3>
        </div>
        {metrics.length === 0 ? (
          <div className="empty-state">
            <p className="empty-state-text">Chưa có bản ghi nào. Hãy ghi nhận chỉ số đầu tiên!</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Cân nặng</th>
                <th>Thay đổi</th>
                <th>% Mỡ</th>
                <th>Eo</th>
              </tr>
            </thead>
            <tbody>
              {metrics.map((m) => (
                <tr key={m.id}>
                  <td className="text-secondary" style={{ fontSize: 13 }}>{new Date(m.recordedAt).toLocaleDateString('vi-VN')}</td>
                  <td className="fw-700">{m.weightKg} kg</td>
                  <td>
                    {m.deltaWeightKg != null ? (
                      <span className={m.deltaWeightKg > 0 ? 'text-negative' : 'text-green'}>
                        {m.deltaWeightKg > 0 ? '↑' : '↓'} {Math.abs(m.deltaWeightKg)}
                      </span>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </td>
                  <td className="text-secondary">{m.bodyFatPct != null ? `${m.bodyFatPct}%` : '—'}</td>
                  <td className="text-secondary">{m.waistCm != null ? `${m.waistCm} cm` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
