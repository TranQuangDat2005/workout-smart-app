import { useState } from 'react';
import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
}

/* ── Inline SVG icon helpers ── */
const Icon = {
  home: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
      <polyline points="9 22 9 12 15 12 15 22" />
    </svg>
  ),
  target: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="6" /><circle cx="12" cy="12" r="2" />
    </svg>
  ),
  calendar: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  ),
  dumbbell: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 4v16M18 4v16M2 8h4M18 8h4M2 16h4M18 16h4M6 12h12" />
    </svg>
  ),
  book: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </svg>
  ),
  history: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="12 8 12 12 14 14" />
      <path d="M3.05 11a9 9 0 1 0 .5-4H1" /><polyline points="1 3 1 7 5 7" />
    </svg>
  ),
  apple: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 2a3 3 0 0 0-3 3h6a3 3 0 0 0-3-3z" />
      <path d="M4.5 9a7.5 7.5 0 0 0 15 0c0-1.5-1.5-3-3-3h-9c-1.5 0-3 1.5-3 3z" />
    </svg>
  ),
  food: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 11l19-9-9 19-2-8-8-2z" />
    </svg>
  ),
  body: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="5" r="2" /><path d="M12 7v7M9 10h6M9 21l3-7 3 7" />
    </svg>
  ),
  users: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
  globe: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20" />
      <path d="M2 12h20" />
    </svg>
  ),
  trophy: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="8 21 12 17 16 21" /><line x1="12" y1="17" x2="12" y2="11" />
      <path d="M7 4h10v6a5 5 0 0 1-10 0V4z" />
      <path d="M7 4H4v3a3 3 0 0 0 3 3" /><path d="M17 4h3v3a3 3 0 0 1-3 3" />
    </svg>
  ),
  bar: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" /><line x1="2" y1="20" x2="22" y2="20" />
    </svg>
  ),
  shield: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  ),
  exercise: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <path d="M8 12h8M12 8v8" />
    </svg>
  ),
  user: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  ),
  logout: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  ),
  menu: (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
    </svg>
  ),
};

const WORKOUT_NAV: NavItem[] = [
  { to: '/',          label: 'Trang chủ',       icon: Icon.home },
  { to: '/training',  label: 'Luyện tập',        icon: Icon.dumbbell },
  { to: '/exercises', label: 'Thư viện bài tập', icon: Icon.book },
  { to: '/history',   label: 'Lịch sử tập',     icon: Icon.history },
];

const NUTRITION_NAV: NavItem[] = [
  { to: '/body-metrics',   label: 'Chỉ số cơ thể',    icon: Icon.body },
  { to: '/nutrition-needs', label: 'Nhu cầu dinh dưỡng', icon: Icon.target },
  { to: '/foods',          label: 'Kho thực phẩm',    icon: Icon.food },
  { to: '/nutrition',      label: 'Thực đơn',         icon: Icon.apple },
];

const SOCIAL_NAV: NavItem[] = [
  { to: '/community',   label: 'Cộng đồng',      icon: Icon.globe },
  { to: '/friends',     label: 'Bạn bè',         icon: Icon.users },
  { to: '/leaderboard', label: 'Bảng xếp hạng',  icon: Icon.trophy },
];

const STATS_NAV: NavItem[] = [
  { to: '/stats', label: 'Thống kê & Báo cáo', icon: Icon.bar },
];

const ADMIN_NAV: NavItem[] = [
  { to: '/admin/users',     label: 'Quản lý người dùng', icon: Icon.shield },
  { to: '/admin/exercises', label: 'Quản lý bài tập',    icon: Icon.exercise },
];

function NavGroup({ title, items }: { title: string; items: NavItem[] }) {
  return (
    <div style={{ marginBottom: 20 }}>
      <span className="nav-section-label">{title}</span>
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
        >
          {item.icon}
          <span>{item.label}</span>
        </NavLink>
      ))}
    </div>
  );
}

function Brand({ isAdmin }: { isAdmin: boolean }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 12px 20px' }}>
      <div
        style={{
          width: 34,
          height: 34,
          borderRadius: '50%',
          background: 'var(--green)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          boxShadow: '0 0 12px rgba(30,215,96,0.25)',
        }}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
          <rect x="2" y="10" width="3" height="4" rx="1" fill="#000" />
          <rect x="19" y="10" width="3" height="4" rx="1" fill="#000" />
          <rect x="5" y="8" width="3" height="8" rx="1.5" fill="#000" />
          <rect x="16" y="8" width="3" height="8" rx="1.5" fill="#000" />
          <rect x="8" y="11" width="8" height="2" rx="1" fill="#000" />
        </svg>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 700, fontSize: 15, letterSpacing: '-0.2px' }}>WorkoutSmart</div>
      </div>
      {isAdmin && (
        <span className="badge badge-info" style={{ fontSize: 9, letterSpacing: 0.5, padding: '2px 7px' }}>ADMIN</span>
      )}
    </div>
  );
}

/** Bố cục chính: sidebar trái (desktop) + topbar/drawer (mobile) + nội dung cuộn phải. */
export default function AppShell({ children }: { children: ReactNode }) {
  const { logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const onLogout = () => {
    setMenuOpen(false);
    logout();
    navigate('/login');
  };

  const sidebarContent = (
    <>
      <Brand isAdmin={isAdmin} />
      <nav style={{ flex: 1 }}>
        <NavGroup title="Tập luyện"  items={WORKOUT_NAV} />
        <NavGroup title="Dinh dưỡng" items={NUTRITION_NAV} />
        <NavGroup title="Cộng đồng"  items={SOCIAL_NAV} />
        <NavGroup title="Thống kê"   items={STATS_NAV} />
        {isAdmin && <NavGroup title="Quản trị" items={ADMIN_NAV} />}
      </nav>
      <div
        style={{
          borderTop: '1px solid rgba(255,255,255,0.06)',
          paddingTop: 12,
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        <NavLink
          to="/profile"
          onClick={() => setMenuOpen(false)}
          className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
        >
          {Icon.user}
          <span>Hồ sơ cá nhân</span>
        </NavLink>
        <button
          onClick={onLogout}
          className="nav-link"
          style={{
            border: 'none',
            background: 'transparent',
            width: '100%',
            textAlign: 'left',
            color: 'var(--text-negative)',
            cursor: 'pointer',
          }}
        >
          <span style={{ opacity: 0.7 }}>{Icon.logout}</span>
          <span>Đăng xuất</span>
        </button>
      </div>
    </>
  );

  return (
    <div className="app-layout">
      {/* Mobile topbar */}
      <header className="app-topbar">
        <button
          className="btn-icon"
          onClick={() => setMenuOpen((v) => !v)}
          aria-label="Mở điều hướng"
          aria-expanded={menuOpen}
        >
          {Icon.menu}
        </button>
        <span className="brand">WorkoutSmart</span>
        {isAdmin && <span className="badge badge-info" style={{ marginLeft: 'auto', fontSize: 9, letterSpacing: 0.5, padding: '2px 7px' }}>ADMIN</span>}
      </header>

      {/* Sidebar (desktop) / drawer (mobile) */}
      <aside className={`app-sidebar${menuOpen ? ' open' : ''}`} aria-label="Điều hướng chính">
        {sidebarContent}
      </aside>

      {menuOpen && <div className="app-backdrop" onClick={() => setMenuOpen(false)} aria-hidden />}

      {/* Main content */}
      <main className="app-main">{children}</main>
    </div>
  );
}
