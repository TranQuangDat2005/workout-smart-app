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
    <AuthLayout title="Đăng nhập">
      <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <TextField
          label="Email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="ban@example.com"
        />
        <TextField
          label="Mật khẩu"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
        />
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Đang xử lý…' : 'Đăng nhập'}
        </Button>
      </form>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: 14,
          color: 'var(--text-secondary)',
        }}
      >
        <Link to="/forgot-password" style={{ color: 'var(--text-secondary)' }}>
          Quên mật khẩu?
        </Link>
        <Link to="/register" style={{ color: 'var(--green)' }}>
          Tạo tài khoản
        </Link>
      </div>
      <div style={{ textAlign: 'center', fontSize: 14 }}>
        <Link to="/verify-otp" style={{ color: 'var(--text-secondary)' }}>
          Chưa xác thực email? Nhập mã OTP
        </Link>
      </div>
    </AuthLayout>
  );
}
