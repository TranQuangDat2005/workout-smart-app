import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

interface NavItem {
  to: string;
  label: string;
  icon: string;
}

const WORKOUT_NAV: NavItem[] = [
  { to: '/', label: 'Trang chủ', icon: '🏠' },
  { to: '/goal-setup', label: 'Thiết lập mục tiêu', icon: '🎯' },
  { to: '/plan', label: 'Lộ trình tập', icon: '🗓' },
  { to: '/workout', label: 'Bắt đầu tập', icon: '▶' },
  { to: '/exercises', label: 'Thư viện bài tập', icon: '🏋️' },
  { to: '/history', label: 'Lịch sử tập', icon: '📜' },
];

const NUTRITION_NAV: NavItem[] = [
  { to: '/nutrition', label: 'Tổng quan', icon: '🍽' },
  { to: '/foods', label: 'Kho thực phẩm', icon: '🥗' },
  { to: '/body-metrics', label: 'Chỉ số cơ thể', icon: '📏' },
];

const SOCIAL_NAV: NavItem[] = [
  { to: '/friends', label: 'Bạn bè', icon: '👥' },
  { to: '/leaderboard', label: 'Bảng xếp hạng', icon: '🏆' },
];

const STATS_NAV: NavItem[] = [{ to: '/stats', label: 'Thống kê', icon: '📊' }];

const ADMIN_NAV: NavItem[] = [
  { to: '/admin/users', label: 'Quản lý người dùng', icon: '👤' },
  { to: '/admin/exercises', label: 'Quản lý bài tập', icon: '⚙️' },
];

function NavGroup({ title, items }: { title: string; items: NavItem[] }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <p
        style={{
          fontSize: 11,
          fontWeight: 700,
          letterSpacing: 1,
          textTransform: 'uppercase',
          color: 'var(--text-secondary)',
          margin: '0 0 6px 12px',
        }}
      >
        {title}
      </p>
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          style={({ isActive }) => ({
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '8px 12px',
            borderRadius: 8,
            fontSize: 14,
            color: isActive ? 'var(--text-base)' : 'var(--text-secondary)',
            background: isActive ? 'var(--mid-dark)' : 'transparent',
            textDecoration: 'none',
            fontWeight: isActive ? 700 : 400,
          })}
        >
          <span>{item.icon}</span>
          <span>{item.label}</span>
        </NavLink>
      ))}
    </div>
  );
}

/** Bố cục chính: sidebar trái + nội dung phải. */
export default function AppShell({ children }: { children: ReactNode }) {
  const { logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const onLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--near-black)' }}>
      <aside
        style={{
          width: 240,
          flexShrink: 0,
          background: 'var(--dark-surface)',
          padding: '20px 12px',
          display: 'flex',
          flexDirection: 'column',
          height: '100vh',
          position: 'sticky',
          top: 0,
          overflowY: 'auto',
          boxShadow: 'var(--shadow-heavy)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '0 12px 20px' }}>
          <span
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: 'var(--green)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20,
              fontWeight: 700,
              color: '#000',
            }}
          >
            W
          </span>
          <span style={{ fontWeight: 700, fontSize: 16 }}>WorkoutSmart</span>
          {isAdmin && (
            <span
              style={{
                background: 'var(--text-announcement)',
                color: '#000',
                fontSize: 10,
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: 9999,
                letterSpacing: 1,
              }}
            >
              ADMIN
            </span>
          )}
        </div>

        <NavGroup title="Tập luyện" items={WORKOUT_NAV} />
        <NavGroup title="Dinh dưỡng" items={NUTRITION_NAV} />
        <NavGroup title="Xã hội" items={SOCIAL_NAV} />
        <NavGroup title="Thống kê" items={STATS_NAV} />
        {isAdmin && <NavGroup title="Quản trị" items={ADMIN_NAV} />}

        <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <NavLink
            to="/profile"
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '8px 12px',
              borderRadius: 8,
              fontSize: 14,
              color: isActive ? 'var(--text-base)' : 'var(--text-secondary)',
              background: isActive ? 'var(--mid-dark)' : 'transparent',
              textDecoration: 'none',
            })}
          >
            <span>👤</span>
            <span>Hồ sơ</span>
          </NavLink>
          <button
            onClick={onLogout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '8px 12px',
              borderRadius: 8,
              fontSize: 14,
              background: 'transparent',
              border: 'none',
              color: 'var(--text-negative)',
              cursor: 'pointer',
            }}
          >
            <span>🚪</span>
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      <main style={{ flex: 1, padding: 32, minWidth: 0 }}>{children}</main>
    </div>
  );
}
