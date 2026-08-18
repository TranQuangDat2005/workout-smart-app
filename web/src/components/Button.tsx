import type { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'dark' | 'outlined' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
  loading?: boolean;
  icon?: ReactNode;
}

/** Pill button — DESIGN.md §4: uppercase, 1.4px tracking, radius 9999px. */
export default function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  icon,
  className = '',
  children,
  disabled,
  ...rest
}: ButtonProps) {
  const variantClass =
    variant === 'primary' ? 'btn-primary'
    : variant === 'dark' ? 'btn-dark'
    : variant === 'danger' ? 'btn-danger'
    : 'btn-outlined';

  const sizeClass = size === 'sm' ? 'btn-sm' : size === 'lg' ? 'btn-lg' : '';

  return (
    <button
      className={`btn ${variantClass} ${sizeClass} ${fullWidth ? 'btn-full' : ''} ${className}`.trim()}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && <span className="btn-spinner" aria-hidden />}
      {!loading && icon}
      {children}
    </button>
  );
}
