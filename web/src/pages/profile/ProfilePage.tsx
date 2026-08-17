import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { useAuth } from '../../context/useAuth';
import { profileApi } from '../../services/profileApi';
import type { Profile } from '../../services/profileApi';

const GOALS = [
  { value: 'weight_loss', label: 'Giảm cân' },
  { value: 'muscle_gain', label: 'Tăng cơ' },
  { value: 'endurance', label: 'Sức bền' },
];

export default function ProfilePage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [displayName, setDisplayName] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [age, setAge] = useState('');
  const [heightCm, setHeightCm] = useState('');
  const [goalType, setGoalType] = useState('weight_loss');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    profileApi
      .getProfile()
      .then((p) => {
        setProfile(p);
        setDisplayName(p.displayName ?? '');
        setAvatarUrl(p.avatarUrl ?? '');
        setAge(p.age != null ? String(p.age) : '');
        setHeightCm(p.heightCm != null ? String(p.heightCm) : '');
        setGoalType(p.goalType ?? 'weight_loss');
      })
      .catch(() => setError('Không thể tải hồ sơ'));
  }, []);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    try {
      const res = await profileApi.updateProfile({
        displayName,
        avatarUrl,
        age: age ? Number(age) : undefined,
        heightCm: heightCm ? Number(heightCm) : undefined,
        goalType,
      });
      setProfile(res.profile);
      setNotice(
        res.goalChanged
          ? 'Đã lưu. Mục tiêu đã thay đổi — hệ thống sẽ gợi ý tạo lại lộ trình tập.'
          : 'Đã lưu hồ sơ.',
      );
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu thất bại');
    } finally {
      setLoading(false);
    }
  };

  const onDelete = async () => {
    if (!window.confirm('Bạn chắc chắn muốn xóa tài khoản? Có thể khôi phục trong 30 ngày.')) return;
    try {
      await profileApi.deleteAccount();
      logout();
      navigate('/login');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Xóa tài khoản thất bại');
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'var(--near-black)',
        padding: 32,
        display: 'flex',
        justifyContent: 'center',
      }}
    >
      <div
        style={{
          background: 'var(--dark-surface)',
          borderRadius: 8,
          boxShadow: 'var(--shadow-heavy)',
          padding: 32,
          width: '100%',
          maxWidth: 560,
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
        }}
      >
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Hồ sơ cá nhân</h1>
        {profile && (
          <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
            Email: {profile.email} · Cân nặng: {profile.weightKg ?? '—'} kg (cập nhật qua chỉ số cơ thể)
          </p>
        )}
        <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <TextField label="Tên hiển thị" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          <TextField label="Ảnh đại diện (URL)" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} />
          <TextField label="Tuổi" type="number" value={age} onChange={(e) => setAge(e.target.value)} />
          <TextField label="Chiều cao (cm)" type="number" value={heightCm} onChange={(e) => setHeightCm(e.target.value)} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label style={{ fontSize: 14, fontWeight: 700 }}>Mục tiêu</label>
            <select
              value={goalType}
              onChange={(e) => setGoalType(e.target.value)}
              style={{
                background: 'var(--mid-dark)',
                color: 'var(--text-base)',
                border: '1px solid transparent',
                borderRadius: 500,
                padding: '12px 16px',
                fontSize: 16,
              }}
            >
              {GOALS.map((g) => (
                <option key={g.value} value={g.value}>
                  {g.label}
                </option>
              ))}
            </select>
          </div>
          {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}
          {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
          <Button type="submit" fullWidth disabled={loading}>
            {loading ? 'Đang lưu…' : 'Lưu hồ sơ'}
          </Button>
        </form>
        <Button variant="outlined" fullWidth onClick={onDelete} style={{ color: 'var(--text-negative)' }}>
          Xóa tài khoản
        </Button>
      </div>
    </div>
  );
}
