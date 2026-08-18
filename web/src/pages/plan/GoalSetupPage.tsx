import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import { planApi } from '../../services/planApi';

const GOALS = [
  { value: 'weight_loss', label: 'Giảm cân' },
  { value: 'muscle_gain', label: 'Tăng cơ' },
  { value: 'endurance', label: 'Sức bền' },
];

const LEVELS = [
  { value: 'beginner', label: 'Mới bắt đầu' },
  { value: 'intermediate', label: 'Trung bình' },
  { value: 'advanced', label: 'Nâng cao' },
];

const EQUIPMENT_OPTIONS = ['body_weight', 'dumbbell', 'barbell', 'machine', 'resistance_band'];

export default function GoalSetupPage() {
  const navigate = useNavigate();
  const [goalType, setGoalType] = useState('weight_loss');
  const [fitnessLevel, setFitnessLevel] = useState('beginner');
  const [equipment, setEquipment] = useState<string[]>(['body_weight']);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const toggleEquipment = (value: string) => {
    setEquipment((prev) =>
      prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value],
    );
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    try {
      const res = await planApi.setupGoal({ goalType, fitnessLevel, equipment });
      setNotice(
        res.warning
          ? `${res.warning} Lộ trình mới đã tạo (#${res.planId}).`
          : `Đã tạo lộ trình #${res.planId}.`,
      );
      navigate('/plan');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể tạo lộ trình');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center' }}>
      <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 32, width: '100%', maxWidth: 560, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Thiết lập mục tiêu</h1>
        <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div>
            <label style={{ fontWeight: 700 }}>Mục tiêu</label>
            <select value={goalType} onChange={(e) => setGoalType(e.target.value)} style={{ width: '100%', padding: 12, borderRadius: 8, background: 'var(--mid-dark)', color: 'var(--text-base)' }}>
              {GOALS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontWeight: 700 }}>Trình độ</label>
            <select value={fitnessLevel} onChange={(e) => setFitnessLevel(e.target.value)} style={{ width: '100%', padding: 12, borderRadius: 8, background: 'var(--mid-dark)', color: 'var(--text-base)' }}>
              {LEVELS.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontWeight: 700 }}>Dụng cụ sẵn có</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 8 }}>
              {EQUIPMENT_OPTIONS.map((e) => (
                <button key={e} type="button" onClick={() => toggleEquipment(e)}
                  style={{ padding: '8px 12px', borderRadius: 500, border: '1px solid var(--mid-dark)', background: equipment.includes(e) ? 'var(--text-base)' : 'transparent', color: equipment.includes(e) ? 'var(--near-black)' : 'var(--text-base)' }}>
                  {e}
                </button>
              ))}
            </div>
          </div>
          {notice && <span style={{ color: 'var(--text-announcement)' }}>{notice}</span>}
          {error && <span style={{ color: 'var(--text-negative)' }}>{error}</span>}
          <Button type="submit" fullWidth disabled={loading}>{loading ? 'Đang tạo…' : 'Tạo lộ trình'}</Button>
        </form>
      </div>
    </div>
  );
}
