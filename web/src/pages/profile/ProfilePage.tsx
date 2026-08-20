import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import type { IconName } from '../../components/Icon';
import Modal from '../../components/Modal';
import TextField from '../../components/TextField';
import { useAuth } from '../../context/useAuth';
import { profileApi } from '../../services/profileApi';
import type { Profile } from '../../services/profileApi';

const GOALS: { value: string; label: string; icon: IconName; desc: string }[] = [
  { value: 'weight_loss', label: 'Giảm cân', icon: 'zap', desc: 'Cardio + full-body, 4–5 ngày/tuần' },
  { value: 'muscle_gain', label: 'Tăng cơ',  icon: 'strength', desc: 'Push/Pull/Legs split, 4 ngày/tuần' },
  { value: 'endurance',   label: 'Sức bền',  icon: 'activity', desc: 'Circuit toàn thân, 3–4 ngày/tuần' },
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
  const [goalDialogOpen, setGoalDialogOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletePassword, setDeletePassword] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);

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

  const doSave = async (mode?: 'now' | 'tomorrow') => {
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
      if (res.goalChanged) {
        setNotice(mode === 'tomorrow' ? 'Đã lưu. Lộ trình mới sẽ bắt đầu từ ngày mai.' : 'Đã lưu. Lộ trình mới đã được tạo.');
      } else {
        setNotice('Đã lưu hồ sơ.');
      }
      setGoalDialogOpen(false);
      if (res.goalChanged && mode === 'now') navigate('/training');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu thất bại');
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (profile?.goalType && goalType !== profile.goalType) {
      setGoalDialogOpen(true);
    } else {
      void doSave();
    }
  };

  const onDelete = () => {
    setDeletePassword('');
    setDeleteOpen(true);
  };

  const confirmDelete = async () => {
    if (!deletePassword) {
      setError('Vui lòng nhập mật khẩu để xác nhận.');
      return;
    }
    setDeleteLoading(true);
    setError('');
    try {
      await profileApi.deleteAccount(deletePassword);
      logout();
      navigate('/login');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Xóa tài khoản thất bại');
    } finally {
      setDeleteLoading(false);
    }
  };

  const initials = displayName
    ? displayName.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)
    : '?';

  return (
    <div className="page-container" style={{ maxWidth: 640, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
        <div
          className="avatar avatar-lg"
          style={{
            background: 'linear-gradient(135deg, var(--green) 0%, var(--green-border) 100%)',
            color: '#000',
            fontSize: 22,
            fontWeight: 800,
          }}
        >
          {avatarUrl ? (
            <img src={avatarUrl} alt={displayName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            initials
          )}
        </div>
        <div>
          <h1 style={{ fontSize: 22 }}>{displayName || 'Hồ sơ cá nhân'}</h1>
          {profile && (
            <p className="text-secondary text-sm" style={{ marginTop: 2 }}>
              {profile.email}
              {profile.weightKg != null && ` · ${profile.weightKg} kg`}
            </p>
          )}
        </div>
      </div>

      {/* Form */}
      <div className="card" style={{ padding: 28 }}>
        <h2 className="section-title" style={{ marginBottom: 20 }}>Thông tin cá nhân</h2>
        <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <TextField label="Tên hiển thị" value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="Tên của bạn" />
            <TextField label="Ảnh đại diện (URL)" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} placeholder="https://..." />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <TextField label="Tuổi" type="number" value={age} onChange={(e) => setAge(e.target.value)} placeholder="25" min="10" max="120" />
            <TextField label="Chiều cao (cm)" type="number" value={heightCm} onChange={(e) => setHeightCm(e.target.value)} placeholder="170" min="100" max="250" />
          </div>

          {/* Goal type chips */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <label className="input-label">Mục tiêu tập luyện</label>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              {GOALS.map((g) => (
                <button
                  key={g.value}
                  type="button"
                  className={`chip ${goalType === g.value ? 'selected' : ''}`}
                  onClick={() => setGoalType(g.value)}
                >
                  <Icon name={g.icon} size={16} style={{ verticalAlign: '-2px' }} />
                  <span>{g.label}</span>
                </button>
              ))}
            </div>
            {GOALS.find((g) => g.value === goalType) && (
              <p className="text-muted" style={{ fontSize: 12, marginTop: 2 }}>
                {GOALS.find((g) => g.value === goalType)?.desc}
              </p>
            )}
          </div>

          {notice && (
            <div className="notice notice-success" role="status">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                <polyline points="20 6 9 17 4 12" />
              </svg>
              {notice}
            </div>
          )}
          {error && (
            <div className="notice notice-error" role="alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              {error}
            </div>
          )}

          <Button type="submit" fullWidth loading={loading}>
            Lưu hồ sơ
          </Button>
        </form>
      </div>

      {/* Danger zone */}
      <div className="card" style={{ borderColor: 'rgba(243,114,127,0.2)', border: '1px solid rgba(243,114,127,0.15)' }}>
        <h3 style={{ marginBottom: 6, color: 'var(--text-negative)' }}>Khu vực nguy hiểm</h3>
        <p className="text-secondary text-sm" style={{ marginBottom: 14 }}>
          Tài khoản sẽ bị xóa mềm — bạn có 30 ngày để khôi phục đầy đủ dữ liệu.
        </p>
        <Button variant="danger" onClick={onDelete} size="sm">
          Xóa tài khoản
        </Button>
      </div>

      {/* Goal change dialog */}
      <Modal
        open={goalDialogOpen}
        title="Đổi mục tiêu tập luyện"
        onClose={() => setGoalDialogOpen(false)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setGoalDialogOpen(false)}>Hủy</Button>
            <Button variant="dark" onClick={() => void doSave('tomorrow')}>Bắt đầu từ ngày mai</Button>
            <Button onClick={() => void doSave('now')}>Tạo ngay</Button>
          </>
        }
      >
        <p className="text-secondary text-sm" style={{ lineHeight: 1.7 }}>
          Việc tạo lại lộ trình mới sẽ làm mất tiến trình tập của ngày hôm nay. Bạn muốn tạo lộ trình mới ngay hay bắt đầu từ ngày mai?
        </p>
      </Modal>

      {/* Delete account password confirmation */}
      <Modal
        open={deleteOpen}
        title="Xóa tài khoản"
        onClose={() => setDeleteOpen(false)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setDeleteOpen(false)}>Hủy</Button>
            <Button variant="danger" onClick={() => void confirmDelete()} loading={deleteLoading}>Xác nhận xóa</Button>
          </>
        }
      >
        <p className="text-secondary text-sm" style={{ marginBottom: 14, lineHeight: 1.7 }}>
          Tài khoản sẽ bị xóa mềm và có thể khôi phục trong 30 ngày. Nhập mật khẩu để xác nhận.
        </p>
        <TextField
          label="Mật khẩu"
          type="password"
          value={deletePassword}
          onChange={(e) => setDeletePassword(e.target.value)}
          autoComplete="current-password"
        />
      </Modal>
    </div>
  );
}
