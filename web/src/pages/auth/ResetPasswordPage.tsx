import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { authApi } from '../../services/authApi';

interface ResetState {
  email?: string;
  code?: string;
  restore?: boolean;
}

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = (location.state as ResetState | null) ?? {};
  const isRestore = state.restore === true;

  const [email, setEmail] = useState(state.email ?? '');
  const [code, setCode] = useState(state.code ?? '');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    try {
      const res = isRestore
        ? await authApi.restorePassword(email, code, newPassword)
        : await authApi.resetPassword(email, code, newPassword);
      setNotice(res.message);
      navigate('/login');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Đặt lại mật khẩu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title={isRestore ? 'Khôi phục tài khoản' : 'Đặt lại mật khẩu'}>
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
          label="Mã OTP (6 chữ số)"
          required
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="123456"
          maxLength={6}
        />
        <TextField
          label="Mật khẩu mới"
          type="password"
          required
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="Ít nhất 8 ký tự, gồm hoa, thường, số"
        />
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Đang xử lý…' : 'Xác nhận'}
        </Button>
      </form>
      <div style={{ textAlign: 'center', fontSize: 14 }}>
        <Link to="/login" style={{ color: 'var(--text-secondary)' }}>
          Quay lại đăng nhập
        </Link>
      </div>
    </AuthLayout>
  );
}
