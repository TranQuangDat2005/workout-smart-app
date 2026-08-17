import type { ReactNode } from 'react';

/** Khung trang auth — card tối #181818, shadow heavy, logo xanh (DESIGN.md). */
export default function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--near-black)',
        padding: 16,
      }}
    >
      <div
        style={{
          background: 'var(--dark-surface)',
          borderRadius: 8,
          boxShadow: 'var(--shadow-heavy)',
          padding: '40px 32px',
          width: '100%',
          maxWidth: 420,
          display: 'flex',
          flexDirection: 'column',
          gap: 24,
        }}
      >
        <div style={{ textAlign: 'center' }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 48,
              height: 48,
              borderRadius: '50%',
              background: 'var(--green)',
              color: '#000',
              fontWeight: 700,
              fontSize: 24,
            }}
          >
            W
          </span>
          <h1 style={{ marginTop: 16 }}>{title}</h1>
        </div>
        {children}
      </div>
    </div>
  );
}
