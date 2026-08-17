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
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    try {
      const res = await authApi.register(email, password);
      setNotice(res.message);
      navigate('/verify-otp', { state: { email } });
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Đăng ký thất bại, vui lòng thử lại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Tạo tài khoản">
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
          placeholder="Ít nhất 8 ký tự, gồm hoa, thường, số"
        />
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Đang xử lý…' : 'Đăng ký'}
        </Button>
      </form>
      <div style={{ textAlign: 'center', fontSize: 14, color: 'var(--text-secondary)' }}>
        Đã có tài khoản?{' '}
        <Link to="/login" style={{ color: 'var(--green)' }}>
          Đăng nhập
        </Link>
      </div>
    </AuthLayout>
  );
}
