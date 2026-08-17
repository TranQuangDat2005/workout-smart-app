import { useEffect, useState } from 'react';
import Button from '../../components/Button';
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

  const load = () => nutritionApi.getBodyMetrics().then(setMetrics).catch(() => setError('Không thể tải chỉ số'));

  useEffect(() => {
    load();
  }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    try {
      await nutritionApi.createBodyMetric({
        weightKg: Number(weight),
        bodyFatPct: bodyFat ? Number(bodyFat) : undefined,
        waistCm: waist ? Number(waist) : undefined,
        chestCm: chest ? Number(chest) : undefined,
        armCm: arm ? Number(arm) : undefined,
      });
      setNotice('Đã cập nhật chỉ số.');
      setWeight(''); setBodyFat(''); setWaist(''); setChest(''); setArm('');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Cập nhật thất bại');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32 }}>
      <div style={{ maxWidth: 640, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Chỉ số cơ thể</h1>
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}

        <form onSubmit={submit} style={{
          background: 'var(--dark-surface)', borderRadius: 8, padding: 16,
          display: 'flex', flexDirection: 'column', gap: 12,
        }}>
          <TextField label="Cân nặng (kg) *" type="number" value={weight} onChange={(e) => setWeight(e.target.value)} required />
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <TextField label="% mỡ" type="number" value={bodyFat} onChange={(e) => setBodyFat(e.target.value)} />
            <TextField label="Vòng eo (cm)" type="number" value={waist} onChange={(e) => setWaist(e.target.value)} />
            <TextField label="Vòng ngực (cm)" type="number" value={chest} onChange={(e) => setChest(e.target.value)} />
            <TextField label="Vòng tay (cm)" type="number" value={arm} onChange={(e) => setArm(e.target.value)} />
          </div>
          <Button type="submit">Cập nhật</Button>
        </form>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {metrics.map((m) => (
            <div key={m.id} style={{
              background: 'var(--dark-surface)', borderRadius: 6, padding: '12px 16px',
              display: 'flex', justifyContent: 'space-between', fontSize: 14,
            }}>
              <span>{new Date(m.recordedAt).toLocaleString('vi-VN')}</span>
              <span style={{ fontWeight: 700 }}>{m.weightKg} kg</span>
              <span style={{ color: m.deltaWeightKg != null && m.deltaWeightKg > 0 ? 'var(--text-negative)' : 'var(--green)' }}>
                {m.deltaWeightKg != null ? `${m.deltaWeightKg > 0 ? '↑' : '↓'} ${Math.abs(m.deltaWeightKg)}` : '—'}
              </span>
            </div>
          ))}
          {metrics.length === 0 && <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Chưa có bản ghi nào.</p>}
        </div>
      </div>
    </div>
  );
}
