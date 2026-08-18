import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import Button from '../components/Button';

const USER_NAV = [
  { to: '/goal-setup', label: 'Thiết lập mục tiêu' },
  { to: '/plan', label: 'Lộ trình tập' },
  { to: '/exercises', label: 'Thư viện bài tập' },
  { to: '/workout', label: 'Bắt đầu tập' },
  { to: '/profile', label: 'Hồ sơ' },
  { to: '/history', label: 'Lịch sử tập' },
  { to: '/nutrition', label: 'Dinh dưỡng' },
  { to: '/foods', label: 'Kho thực phẩm' },
  { to: '/body-metrics', label: 'Chỉ số cơ thể' },
  { to: '/friends', label: 'Bạn bè' },
  { to: '/leaderboard', label: 'Bảng xếp hạng' },
  { to: '/stats', label: 'Thống kê' },
];

const ADMIN_NAV = [
  { to: '/admin/users', label: 'Quản lý người dùng' },
  { to: '/admin/exercises', label: 'Quản lý bài tập' },
];

function NavLinks({ items }: { items: { to: string; label: string }[] }) {
  return (
    <>
      {items.map((item) => (
        <Link
          key={item.to}
          to={item.to}
          style={{
            background: 'var(--dark-surface)',
            color: 'var(--text-base)',
            borderRadius: 9999,
            padding: '10px 18px',
            fontSize: 13,
            fontWeight: 700,
            letterSpacing: '1px',
            textTransform: 'uppercase',
            textDecoration: 'none',
          }}
        >
          {item.label}
        </Link>
      ))}
    </>
  );
}

export default function HomePage() {
  const { logout, isAdmin } = useAuth();
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
      <p style={{ color: 'var(--text-secondary)' }}>
        {isAdmin ? 'Bạn đã đăng nhập với vai trò Quản trị viên.' : 'Bạn đã đăng nhập thành công.'}
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, maxWidth: 640, justifyContent: 'center' }}>
        <NavLinks items={USER_NAV} />
      </div>
      {isAdmin && (
        <>
          <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-announcement)' }}>
            Khu vực quản trị
          </h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, maxWidth: 640, justifyContent: 'center' }}>
            <NavLinks items={ADMIN_NAV} />
          </div>
        </>
      )}
      <Button variant="dark" onClick={logout}>
        Đăng xuất
      </Button>
    </div>
  );
}
