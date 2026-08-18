import { useRef, useState, KeyboardEvent } from 'react';
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
  const [digits, setDigits] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const code = digits.join('');

  const handleDigit = (index: number, value: string) => {
    const v = value.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = v;
    setDigits(next);
    if (v && index < 5) inputRefs.current[index + 1]?.focus();
  };

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    try {
      const res = await authApi.verifyOtp(email, code);
      if (res.restoreRequired) {
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
    <AuthLayout
      title="Xác thực email"
      subtitle="Nhập mã 6 chữ số được gửi vào email của bạn"
    >
      <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <TextField
          label="Email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="ban@example.com"
        />

        {/* 6-digit OTP boxes */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <label className="input-label">Mã OTP</label>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
            {digits.map((d, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                aria-label={`Chữ số ${i + 1}`}
                value={d}
                onChange={(e) => handleDigit(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                style={{
                  width: 46,
                  height: 56,
                  textAlign: 'center',
                  fontSize: 22,
                  fontWeight: 700,
                  background: 'var(--mid-dark)',
                  color: 'var(--text-base)',
                  border: d ? '2px solid var(--green)' : '2px solid var(--border-dark)',
                  borderRadius: 10,
                  outline: 'none',
                  transition: 'border-color var(--t-fast)',
                  caretColor: 'var(--green)',
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = 'var(--green)';
                }}
                onBlur={(e) => {
                  if (!e.currentTarget.value) e.currentTarget.style.borderColor = 'var(--border-dark)';
                }}
              />
            ))}
          </div>
        </div>

        {error && (
          <div className="notice notice-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}
        {notice && (
          <div className="notice notice-success" role="status">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
              <polyline points="20 6 9 17 4 12" />
            </svg>
            {notice}
          </div>
        )}

        <Button type="submit" fullWidth loading={loading} disabled={code.length < 6}>
          Xác nhận
        </Button>
        <Button type="button" variant="outlined" fullWidth onClick={onResend} disabled={loading}>
          Gửi lại mã
        </Button>
      </form>

      <div style={{ textAlign: 'center', fontSize: 13 }}>
        <Link to="/login" className="text-secondary">← Quay lại đăng nhập</Link>
      </div>
    </AuthLayout>
  );
}
