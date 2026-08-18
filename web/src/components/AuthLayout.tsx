import type { ReactNode } from 'react';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

/** Auth card — DESIGN.md: #181818 card, green logo, slide-up animation. */
export default function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background:
          'radial-gradient(ellipse at 50% 0%, rgba(30,215,96,0.07) 0%, transparent 60%), var(--near-black)',
        padding: 16,
      }}
    >
      <div
        className="animate-slide-up"
        style={{
          background: 'var(--dark-surface)',
          borderRadius: 16,
          boxShadow: 'var(--shadow-heavy)',
          padding: '40px 36px',
          width: '100%',
          maxWidth: 420,
          display: 'flex',
          flexDirection: 'column',
          gap: 28,
        }}
      >
        {/* Logo + heading */}
        <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: '50%',
              background: 'var(--green)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 24px rgba(30,215,96,0.3)',
            }}
          >
            {/* Dumbbell icon */}
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden>
              <rect x="2" y="10" width="3" height="4" rx="1" fill="#000" />
              <rect x="19" y="10" width="3" height="4" rx="1" fill="#000" />
              <rect x="5" y="8" width="3" height="8" rx="1.5" fill="#000" />
              <rect x="16" y="8" width="3" height="8" rx="1.5" fill="#000" />
              <rect x="8" y="11" width="8" height="2" rx="1" fill="#000" />
            </svg>
          </div>
          <div>
            <h1 style={{ fontSize: 22, marginBottom: 4 }}>{title}</h1>
            {subtitle && (
              <p style={{ fontSize: 14, color: 'var(--text-secondary)', margin: 0 }}>{subtitle}</p>
            )}
          </div>
        </div>

        {children}
      </div>
    </div>
  );
}
