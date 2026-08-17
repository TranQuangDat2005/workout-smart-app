import type { InputHTMLAttributes } from 'react';

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

/** Input pill — DESIGN.md §4: bg #1f1f1f, radius 500px, inset border. */
export default function TextField({ label, error, ...rest }: TextFieldProps) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <label style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-base)' }}>{label}</label>
      <input
        style={{
          background: 'var(--mid-dark)',
          color: 'var(--text-base)',
          border: error ? '1px solid var(--text-negative)' : '1px solid transparent',
          borderRadius: 500,
          padding: '12px 16px',
          fontSize: 16,
          outline: 'none',
          boxShadow: 'rgb(18,18,18) 0px 1px 0px, rgb(124,124,124) 0px 0px 0px 1px inset',
        }}
        {...rest}
      />
      {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
    </div>
  );
}
