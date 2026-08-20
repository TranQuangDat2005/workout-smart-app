import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Spinner from '../../components/Spinner';
import TextField from '../../components/TextField';
import { nutritionApi } from '../../services/nutritionApi';
import { profileApi } from '../../services/profileApi';
import type { NutritionNeeds } from '../../services/nutritionApi';

const GOAL_LABELS: Record<string, string> = {
  weight_loss: 'Giảm cân',
  muscle_gain: 'Tăng cơ',
  endurance: 'Duy trì',
};

const CALORIE_GOAL_LABELS: Record<string, string> = {
  maintain: 'Duy trì cân (giữ nguyên TDEE)',
  cut_light: 'Giảm cân nhẹ (−300 kcal)',
  cut_fast: 'Giảm cân nhanh (−500 kcal)',
  bulk_light: 'Tăng cân nhẹ (+300 kcal)',
  bulk_fast: 'Tăng cân nhanh (+500 kcal)',
  custom: 'Tùy chỉnh',
};

function fmtKcal(value: number): string {
  return `${Math.round(value).toLocaleString('vi-VN')} kcal`;
}

/**
 * 019: Nhu cầu dinh dưỡng — chỉ nhập MỨC VẬN ĐỘNG + MỨC ĐIỀU CHỈNH CALO ở đây;
 * giới tính/tuổi/chiều cao/cân nặng nhập tại Chỉ số cơ thể (đồng bộ hồ sơ, không nhập 2 nơi).
 */
export default function NutritionNeedsPage() {
  const [needs, setNeeds] = useState<NutritionNeeds | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [activityLevel, setActivityLevel] = useState('moderate');
  const [calorieGoal, setCalorieGoal] = useState('maintain');
  const [customOffset, setCustomOffset] = useState('');

  const loadNeeds = () =>
    nutritionApi
      .getNeeds()
      .then((n) => {
        setNeeds(n);
        setError('');
      })
      .catch((err: unknown) => {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
        setNeeds(null);
        setError(msg ?? 'Không thể tính nhu cầu dinh dưỡng');
      })
      .finally(() => setLoading(false));

  useEffect(() => {
    profileApi
      .getProfile()
      .then((p) => {
        if (p.activityLevel) setActivityLevel(p.activityLevel);
        if (p.calorieGoal) setCalorieGoal(p.calorieGoal);
        if (p.customCalorieOffset != null) setCustomOffset(String(p.customCalorieOffset));
      })
      .catch(() => {});
    void loadNeeds();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const saveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    if (calorieGoal === 'custom' && customOffset.trim() === '') {
      setError('Nhập số kcal tùy chỉnh (vd −350 để giảm, +250 để tăng)');
      return;
    }
    setSaving(true);
    setNotice('');
    setError('');
    try {
      await profileApi.updateProfile({
        activityLevel,
        calorieGoal,
        customCalorieOffset: calorieGoal === 'custom' ? Number(customOffset) : undefined,
      });
      setNotice('Đã lưu cài đặt calo.');
      setLoading(true);
      await loadNeeds();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  };

  const deltaPerDay = needs ? Math.round(needs.targetCalories - needs.tdee) : 0;
  const deltaPerMeal = needs ? Math.round(deltaPerDay / needs.mealsPerDay) : 0;
  const macroRows = needs
    ? [
        { name: 'Protein', grams: needs.proteinG, kcal: Math.round(needs.proteinG * 4), color: 'var(--green)' },
        { name: 'Carb', grams: needs.carbG, kcal: Math.round(needs.carbG * 4), color: 'var(--announcement)' },
        { name: 'Fat', grams: needs.fatG, kcal: Math.round(needs.fatG * 9), color: 'var(--text-warning)' },
      ]
    : [];
  const totalMacroKcal = needs
    ? Math.max(1, macroRows.reduce((sum, m) => sum + m.kcal, 0))
    : 1;

  return (
    <div className="page-container" style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1><Icon name="target" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Nhu cầu dinh dưỡng</h1>
      </div>

      {/* Cài đặt calo — chỉ mức vận động + mức điều chỉnh calo */}
      <div className="card">
        <h3 style={{ marginBottom: 12 }}>Cài đặt calo</h3>
        <form onSubmit={saveSettings} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <label className="input-group" style={{ display: 'block' }}>
            <span className="input-label">Mức vận động</span>
            <select className="input-field" value={activityLevel} onChange={(e) => setActivityLevel(e.target.value)} style={{ width: '100%' }}>
              <option value="sedentary">Ít vận động</option>
              <option value="light">Nhẹ (1-3 buổi/tuần)</option>
              <option value="moderate">Vừa (3-5 buổi/tuần)</option>
              <option value="active">Nhiều (6-7 buổi/tuần)</option>
              <option value="very_active">Rất nhiều (2 lần/ngày)</option>
            </select>
          </label>
          <label className="input-group" style={{ display: 'block' }}>
            <span className="input-label">Mức điều chỉnh calo</span>
            <select className="input-field" value={calorieGoal} onChange={(e) => setCalorieGoal(e.target.value)} style={{ width: '100%' }}>
              <option value="maintain">Duy trì cân (giữ nguyên TDEE)</option>
              <option value="cut_light">Giảm cân nhẹ (−300 kcal)</option>
              <option value="cut_fast">Giảm cân nhanh (−500 kcal)</option>
              <option value="bulk_light">Tăng cân nhẹ (+300 kcal)</option>
              <option value="bulk_fast">Tăng cân nhanh (+500 kcal)</option>
              <option value="custom">Tùy chỉnh (nhập kcal)</option>
            </select>
          </label>
          {calorieGoal === 'custom' && (
            <TextField
              label="Tùy chỉnh (kcal/ngày — âm để giảm, dương để tăng)"
              type="number"
              value={customOffset}
              onChange={(e) => setCustomOffset(e.target.value)}
              placeholder="−350"
              min="−2000"
              max="2000"
              step="50"
            />
          )}
          <Button type="submit" fullWidth loading={saving}>Lưu cài đặt</Button>
        </form>
      </div>

      {notice && <div className="notice notice-success">{notice}</div>}
      {error && !needs && <div className="notice notice-error">{error}</div>}

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 32 }}>
          <Spinner />
        </div>
      ) : !needs ? (
        <div className="card" style={{ textAlign: 'center', padding: '40px 24px' }}>
          <div style={{ marginBottom: 12 }}><Icon name="alert" size={48} /></div>
          <p className="text-secondary" style={{ fontSize: 14, marginBottom: 8 }}>
            Chưa đủ thông tin cơ thể để tính.
          </p>
          <p className="text-muted" style={{ fontSize: 13, marginBottom: 16 }}>
            Vào Chỉ số cơ thể nhập đầy đủ giới tính, tuổi, chiều cao và cân nặng rồi quay lại đây.
          </p>
          <Link to="/body-metrics">
            <Button>Điền thông tin → Chỉ số cơ thể</Button>
          </Link>
        </div>
      ) : (
        <>
          {/* Các chỉ số chính */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
            <div className="stat-card">
              <div className="stat-label">BMR (nghỉ ngơi)</div>
              <div style={{ fontSize: 20, fontWeight: 700 }}>{fmtKcal(needs.bmr)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">TDEE (duy trì)</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--green)' }}>{fmtKcal(needs.tdee)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Mục tiêu calo/ngày</div>
              <div style={{ fontSize: 20, fontWeight: 700 }}>{fmtKcal(needs.targetCalories)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Mỗi bữa ({needs.mealsPerDay} bữa)</div>
              <div style={{ fontSize: 20, fontWeight: 700 }}>{fmtKcal(needs.perMealCalories)}</div>
            </div>
          </div>

          {/* Điều chỉnh theo mục tiêu */}
          <div className="card">
            <h3 style={{ marginBottom: 10 }}>
              Mục tiêu tập: {GOAL_LABELS[needs.goalType] ?? needs.goalType} · {needs.calorieGoal ? CALORIE_GOAL_LABELS[needs.calorieGoal] ?? needs.calorieGoal : ''}
            </h3>
            <p className="text-secondary" style={{ fontSize: 14 }}>
              {deltaPerDay === 0 ? (
                <>Giữ nguyên mức TDEE để duy trì cân nặng.</>
              ) : deltaPerDay < 0 ? (
                <>
                  Giảm <strong style={{ color: '#f87171' }}>{Math.abs(deltaPerDay)} kcal/ngày</strong> so với TDEE
                  {' '}≈ <strong>{Math.abs(deltaPerMeal)} kcal/bữa</strong> ({needs.mealsPerDay} bữa).
                </>
              ) : (
                <>
                  Tăng <strong style={{ color: 'var(--green)' }}>+{deltaPerDay} kcal/ngày</strong> so với TDEE
                  {' '}≈ <strong>+{deltaPerMeal} kcal/bữa</strong> ({needs.mealsPerDay} bữa).
                </>
              )}
            </p>
          </div>

          {/* Bảng macro */}
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <div className="section-header" style={{ padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
              <h2 className="section-title" style={{ margin: 0 }}>Phân bổ macro</h2>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Chất</th>
                  <th>Số gam/ngày</th>
                  <th>Năng lượng</th>
                  <th>Tỷ lệ</th>
                </tr>
              </thead>
              <tbody>
                {macroRows.map((m) => (
                  <tr key={m.name}>
                    <td className="fw-700" style={{ color: m.color }}>{m.name}</td>
                    <td className="fw-600">{m.grams}g</td>
                    <td className="text-secondary">{m.kcal} kcal</td>
                    <td className="text-secondary">{Math.round((m.kcal / totalMacroKcal) * 100)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="text-secondary" style={{ fontSize: 12, padding: '10px 20px' }}>
              Công thức: Protein 2g/kg cân nặng · Fat 25% calo mục tiêu · Carb = phần còn lại (tối thiểu 0).
            </div>
          </div>

          {/* Thông tin cơ thể (nhập tại Chỉ số cơ thể) */}
          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
              <h3 style={{ margin: 0 }}>Thông tin cơ thể</h3>
              <Link to="/body-metrics">
                <Button variant="outlined" size="sm">Sửa → Chỉ số cơ thể</Button>
              </Link>
            </div>
            <div className="text-secondary" style={{ fontSize: 13, display: 'flex', flexDirection: 'column', gap: 4 }}>
              <span>Giới tính: <strong className="text-base">{needs.sex === 'male' ? 'Nam' : needs.sex === 'female' ? 'Nữ' : '—'}</strong></span>
              <span>Tuổi: <strong className="text-base">{needs.age ?? '—'}</strong></span>
              <span>Chiều cao: <strong className="text-base">{needs.heightCm ?? '—'} cm</strong></span>
              <span>Cân nặng: <strong className="text-base">{needs.weightKg ?? '—'} kg</strong></span>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
