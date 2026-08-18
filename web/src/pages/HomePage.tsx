import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import Button from '../components/Button';
import UserDashboard from './UserDashboard';
import AdminDashboard from './AdminDashboard';

const USER_NAV = [
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
            padding: '10px 16px',
            fontSize: 12,
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
        background: 'var(--near-black)',
        padding: 32,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 24,
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 960,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span
            style={{
              width: 44,
              height: 44,
              borderRadius: '50%',
              background: 'var(--green)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 24,
              fontWeight: 700,
              color: '#000',
            }}
          >
            W
          </span>
          <span style={{ fontWeight: 700, fontSize: 18 }}>WorkoutSmartApp</span>
          {isAdmin && (
            <span
              style={{
                background: 'var(--text-announcement)',
                color: '#000',
                fontSize: 11,
                fontWeight: 700,
                padding: '4px 10px',
                borderRadius: 9999,
                letterSpacing: 1,
              }}
            >
              ADMIN
            </span>
          )}
        </div>
        <Button variant="dark" onClick={logout}>
          Đăng xuất
        </Button>
      </div>

      {isAdmin ? <AdminDashboard /> : <UserDashboard />}

      <div
        style={{
          width: '100%',
          maxWidth: 960,
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}
      >
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          <NavLinks items={USER_NAV} />
        </div>
        {isAdmin && (
          <>
            <p style={{ fontSize: 12, color: 'var(--text-announcement)', fontWeight: 700, letterSpacing: 1, margin: 0 }}>
              KHU VỰC QUẢN TRỊ
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              <NavLinks items={ADMIN_NAV} />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
