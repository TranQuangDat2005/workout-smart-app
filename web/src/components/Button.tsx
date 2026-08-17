import type { ButtonHTMLAttributes } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'dark' | 'outlined';
  fullWidth?: boolean;
}

/** Pill button — DESIGN.md §4: uppercase + letter-spacing, radius 9999px. */
export default function Button({
  variant = 'primary',
  fullWidth = false,
  className = '',
  children,
  ...rest
}: ButtonProps) {
  const base: React.CSSProperties = {
    borderRadius: 9999,
    padding: '14px 32px',
    border: 'none',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '1.4px',
    width: fullWidth ? '100%' : undefined,
  };
  const styles: Record<string, React.CSSProperties> = {
    primary: { ...base, background: 'var(--green)', color: '#000000' },
    dark: { ...base, background: 'var(--mid-dark)', color: 'var(--text-base)' },
    outlined: {
      ...base,
      background: 'transparent',
      color: 'var(--text-base)',
      border: '1px solid var(--border-light)',
    },
  };
  return (
    <button style={styles[variant]} className={className} {...rest}>
      {children}
    </button>
  );
}
