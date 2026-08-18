import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { authApi } from '../../services/authApi';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await authApi.register(email, password);
      void res;
      navigate('/verify-otp', { state: { email } });
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Đăng ký thất bại, vui lòng thử lại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Tạo tài khoản"
      subtitle="Bắt đầu hành trình tập luyện thông minh"
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
          placeholder="Ít nhất 8 ký tự, gồm hoa, thường, số"
          autoComplete="new-password"
        />
        <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: -8 }}>
          Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số.
        </p>

        {error && (
          <div className="notice notice-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, marginTop: 1 }}>
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        <Button type="submit" fullWidth loading={loading}>
          Đăng ký
        </Button>
      </form>

      <div style={{ textAlign: 'center', fontSize: 13, color: 'var(--text-secondary)' }}>
        Đã có tài khoản?{' '}
        <Link to="/login" style={{ color: 'var(--green)', fontWeight: 600 }}>Đăng nhập</Link>
      </div>
    </AuthLayout>
  );
}
