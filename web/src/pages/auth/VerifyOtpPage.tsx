import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { authApi } from '../../services/authApi';

export default function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const initialEmail = (location.state as { email?: string } | null)?.email ?? '';
  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    try {
      const res = await authApi.verifyOtp(email, code);
      if (res.restoreRequired) {
        // Luồng khôi phục tài khoản soft-delete → chuyển sang đặt mật khẩu mới
        navigate('/reset-password', { state: { email, code, restore: true } });
        return;
      }
      setNotice(res.message);
      navigate('/login', { state: { verified: true } });
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Xác thực thất bại');
    } finally {
      setLoading(false);
    }
  };

  const onResend = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await authApi.resendOtp(email);
      setNotice(res.message);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Gửi lại thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Xác thực email">
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
        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Đang xử lý…' : 'Xác nhận'}
        </Button>
        <Button type="button" variant="outlined" fullWidth onClick={onResend} disabled={loading}>
          Gửi lại mã
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
