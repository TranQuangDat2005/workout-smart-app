import { useState } from 'react';
import type { FormEvent } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import type { IconName } from '../../components/Icon';
import { planApi } from '../../services/planApi';

const GOALS: { value: string; label: string; icon: IconName; desc: string }[] = [
  { value: 'weight_loss', label: 'Giảm cân',  icon: 'zap', desc: 'Cardio + full-body · 4–5 ngày/tuần · 12–15 reps · nghỉ 45–60s' },
  { value: 'muscle_gain', label: 'Tăng cơ',   icon: 'strength', desc: 'Push/Pull/Legs split · 4 ngày/tuần · 8–12 reps · nghỉ 60–90s' },
  { value: 'endurance',   label: 'Sức bền',   icon: 'activity', desc: 'Circuit toàn thân · 3–4 ngày/tuần · 15–20 reps · nghỉ 30–45s' },
];

const LEVELS: { value: string; label: string; icon: IconName; desc: string }[] = [
  { value: 'beginner',     label: 'Mới bắt đầu', icon: 'leaf', desc: '2–3 bài/ngày × 3 sets' },
  { value: 'intermediate', label: 'Trung bình',   icon: 'trendingUp', desc: '4–5 bài/ngày × 4 sets' },
  { value: 'advanced',     label: 'Nâng cao',     icon: 'flame', desc: '5–6 bài/ngày × 4–5 sets' },
];

const EQUIPMENT_OPTIONS: { value: string; label: string; icon: IconName }[] = [
  { value: 'body_weight',     label: 'Cân nặng cơ thể', icon: 'activity' },
  { value: 'dumbbell',        label: 'Tạ đơn',          icon: 'dumbbell' },
  { value: 'barbell',         label: 'Tạ đòn',          icon: 'strength' },
  { value: 'machine',         label: 'Máy tập',         icon: 'tool' },
  { value: 'resistance_band', label: 'Dây kháng lực',   icon: 'link' },
];

export default function GoalSetupForm({ onDone }: { onDone: () => void }) {
  const [goalType, setGoalType] = useState('weight_loss');
  const [fitnessLevel, setFitnessLevel] = useState('beginner');
  const [equipment, setEquipment] = useState<string[]>(['body_weight']);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const toggleEquipment = (value: string) => {
    setEquipment((prev) =>
      prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value],
    );
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await planApi.setupGoal({ goalType, fitnessLevel, equipment });
      onDone();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể tạo lộ trình');
    } finally {
      setLoading(false);
    }
  };

  const selectedGoal = GOALS.find((g) => g.value === goalType);
  const selectedLevel = LEVELS.find((l) => l.value === fitnessLevel);

  return (
    <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Goal type */}
      <div className="card" style={{ padding: 24 }}>
        <label className="input-label" style={{ display: 'block', marginBottom: 14 }}>Mục tiêu tập luyện</label>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {GOALS.map((g) => (
            <button
              key={g.value}
              type="button"
              onClick={() => setGoalType(g.value)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 14,
                padding: '14px 18px',
                borderRadius: 10,
                border: `1.5px solid ${goalType === g.value ? 'var(--green)' : 'var(--border-dark)'}`,
                background: goalType === g.value ? 'rgba(30,215,96,0.07)' : 'var(--mid-dark)',
                cursor: 'pointer',
                textAlign: 'left',
                transition: 'all var(--t-fast)',
              }}
            >
              <Icon name={g.icon} size={28} />
              <div>
                <div style={{ fontWeight: 700, color: goalType === g.value ? 'var(--green)' : 'var(--text-base)' }}>{g.label}</div>
                <div className="text-muted" style={{ fontSize: 12, marginTop: 2 }}>{g.desc}</div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Fitness level */}
      <div className="card" style={{ padding: 24 }}>
        <label className="input-label" style={{ display: 'block', marginBottom: 14 }}>Trình độ hiện tại</label>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {LEVELS.map((l) => (
            <button
              key={l.value}
              type="button"
              className={`chip ${fitnessLevel === l.value ? 'selected' : ''}`}
              onClick={() => setFitnessLevel(l.value)}
            >
              <Icon name={l.icon} size={16} style={{ verticalAlign: '-2px' }} />
              <span>{l.label}</span>
            </button>
          ))}
        </div>
        {selectedLevel && (
          <p className="text-muted" style={{ fontSize: 12, marginTop: 10 }}>{selectedLevel.desc}</p>
        )}
      </div>

      {/* Equipment */}
      <div className="card" style={{ padding: 24 }}>
        <label className="input-label" style={{ display: 'block', marginBottom: 14 }}>Dụng cụ sẵn có <span style={{ fontWeight: 400, textTransform: 'none' }}>(chọn tất cả phù hợp)</span></label>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {EQUIPMENT_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={`chip ${equipment.includes(opt.value) ? 'selected' : ''}`}
              onClick={() => toggleEquipment(opt.value)}
            >
              <Icon name={opt.icon} size={16} style={{ verticalAlign: '-2px' }} />
              <span>{opt.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Summary */}
      {selectedGoal && (
        <div className="notice notice-info">
          <Icon name={selectedGoal.icon} size={18} />
          <div>
            <div className="fw-600">{selectedGoal.label} · {selectedLevel?.label}</div>
            <div style={{ fontSize: 12, marginTop: 2 }}>{selectedGoal.desc}</div>
          </div>
        </div>
      )}

      {/* Kiểu hiệp */}
      <div className="card" style={{ padding: 20 }}>
        <div className="input-label" style={{ display: 'block', marginBottom: 12 }}>Kiểu hiệp khi tập</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="badge badge-neutral">Bình thường</span>
            <span className="text-secondary" style={{ fontSize: 12 }}>Hiệp tập chuẩn, tính vào tổng khối lượng.</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="badge badge-info">Khởi động</span>
            <span className="text-secondary" style={{ fontSize: 12 }}>Warm-up, không tính vào tổng khối lượng.</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="badge badge-warning">Drop-set</span>
            <span className="text-secondary" style={{ fontSize: 12 }}>Giảm tạ liền sau hiệp chính, nghỉ 0 giây.</span>
          </div>
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      <Button type="submit" fullWidth loading={loading} size="lg">
        Tạo lộ trình tập luyện
      </Button>
    </form>
  );
}
