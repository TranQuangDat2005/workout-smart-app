import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { useAuth } from '../../context/useAuth';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Đăng nhập thất bại, vui lòng thử lại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Đăng nhập"
      subtitle="Tiếp tục lộ trình tập luyện của bạn"
    >
      <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <TextField
          label="Email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="ban@example.com"
          autoComplete="email"
        />
        <TextField
          label="Mật khẩu"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          autoComplete="current-password"
        />

        {error && (
          <div className="notice notice-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, marginTop: 1 }}>
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        <Button type="submit" fullWidth loading={loading}>
          Đăng nhập
        </Button>
      </form>

      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
        <Link to="/forgot-password" className="text-secondary" style={{ transition: 'color var(--t-fast)' }}
          onMouseEnter={e => (e.currentTarget.style.color = 'var(--text-base)')}
          onMouseLeave={e => (e.currentTarget.style.color = 'var(--text-secondary)')}
        >
          Quên mật khẩu?
        </Link>
        <Link to="/register" style={{ color: 'var(--green)', fontWeight: 600 }}>
          Tạo tài khoản
        </Link>
      </div>

      <div className="divider-text" style={{ fontSize: 12 }}>hoặc</div>

      <div style={{ textAlign: 'center', fontSize: 13 }}>
        <Link to="/verify-otp" className="text-secondary"
          onMouseEnter={e => (e.currentTarget.style.color = 'var(--text-base)')}
          onMouseLeave={e => (e.currentTarget.style.color = 'var(--text-secondary)')}
        >
          Chưa xác thực email? Nhập mã OTP
        </Link>
      </div>
    </AuthLayout>
  );
}
