import { useAuth } from '../context/useAuth';
import Button from '../components/Button';

/** Trang chủ tạm — chứng minh auth hoạt động end-to-end. */
export default function HomePage() {
  const { logout } = useAuth();
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 24,
        background: 'var(--near-black)',
      }}
    >
      <span
        style={{
          width: 72,
          height: 72,
          borderRadius: '50%',
          background: 'var(--green)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 36,
          fontWeight: 700,
          color: '#000',
        }}
      >
        W
      </span>
      <h1>Chào mừng đến WorkoutSmartApp</h1>
      <p style={{ color: 'var(--text-secondary)' }}>Bạn đã đăng nhập thành công.</p>
      <Button variant="dark" onClick={logout}>
        Đăng xuất
      </Button>
    </div>
  );
}
