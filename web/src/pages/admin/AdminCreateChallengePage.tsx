import { useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import TextField from '../../components/TextField';
import { socialApi } from '../../services/socialApi';

const GOAL_TYPES = [
  { value: 'weight_loss', label: 'Giảm cân' },
  { value: 'muscle_gain', label: 'Tăng cơ' },
  { value: 'endurance', label: 'Sức bền' },
  { value: 'strength', label: 'Sức mạnh' },
  { value: 'flexibility', label: 'Sự dẻo dai' },
  { value: 'general', label: 'Tổng quát' },
];

export default function CreateChallengePage() {
  const [name, setName] = useState('');
  const [goalType, setGoalType] = useState('general');
  const [durationDays, setDurationDays] = useState('7');
  const [startDate, setStartDate] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    if (!name.trim()) { setError('Tên thử thách không được để trống'); return; }
    const days = parseInt(durationDays, 10);
    if (isNaN(days) || days < 1) { setError('Số ngày phải >= 1'); return; }

    setLoading(true);
    try {
      await socialApi.createChallenge({
        name: name.trim(),
        goalType,
        durationDays: days,
        startDate: startDate || undefined,
      });
      setNotice('Tạo thử thách thành công!');
      setName(''); setDurationDays('7'); setStartDate('');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Tạo thử thách thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container" style={{ maxWidth: 560, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1><Icon name="zap" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Tạo thử thách mới</h1>
      </div>

      {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
      {error && <div className="notice notice-error">{error}</div>}

      <form onSubmit={onSubmit} className="card" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <TextField label="Tên thử thách" value={name} onChange={(e) => setName(e.target.value)} placeholder="VD: 30 ngày plank challenge" />

        <div>
          <label className="text-sm fw-600" style={{ display: 'block', marginBottom: 4 }}>Loại mục tiêu</label>
          <select
            className="input"
            value={goalType}
            onChange={(e) => setGoalType(e.target.value)}
            style={{ width: '100%', padding: '8px 12px', borderRadius: 6, border: '1px solid var(--border)' }}
          >
            {GOAL_TYPES.map((g) => (
              <option key={g.value} value={g.value}>{g.label}</option>
            ))}
          </select>
        </div>

        <TextField label="Số ngày" value={durationDays} onChange={(e) => setDurationDays(e.target.value)} type="number" />

        <TextField label="Ngày bắt đầu (để trống = hôm nay)" value={startDate} onChange={(e) => setStartDate(e.target.value)} type="date" />

        <Button type="submit" variant="primary" disabled={loading}>
          {loading ? 'Đang tạo...' : 'Tạo thử thách'}
        </Button>
      </form>
    </div>
  );
}
