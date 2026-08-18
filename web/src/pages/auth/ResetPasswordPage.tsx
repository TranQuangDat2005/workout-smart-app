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
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = isRestore
        ? await authApi.restorePassword(email, code, newPassword)
        : await authApi.resetPassword(email, code, newPassword);
      void res;
      navigate('/login');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Đặt lại mật khẩu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title={isRestore ? 'Khôi phục tài khoản' : 'Đặt lại mật khẩu'}
      subtitle={isRestore ? 'Tài khoản của bạn sẽ được khôi phục đầy đủ' : 'Tạo mật khẩu mới cho tài khoản'}
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
          label="Mã OTP (6 chữ số)"
          required
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="123456"
          maxLength={6}
          inputMode="numeric"
        />
        <TextField
          label="Mật khẩu mới"
          type="password"
          required
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="Ít nhất 8 ký tự"
          autoComplete="new-password"
        />

        {error && (
          <div className="notice notice-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        <Button type="submit" fullWidth loading={loading}>
          {isRestore ? 'Khôi phục tài khoản' : 'Xác nhận'}
        </Button>
      </form>

      <div style={{ textAlign: 'center', fontSize: 13 }}>
        <Link to="/login" className="text-secondary">← Quay lại đăng nhập</Link>
      </div>
    </AuthLayout>
  );
}
