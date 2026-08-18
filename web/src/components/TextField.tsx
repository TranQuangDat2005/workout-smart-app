import type { InputHTMLAttributes } from 'react';

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  pill?: boolean;
}

/** Input — DESIGN.md §4: bg #1f1f1f, inset border, green focus ring. */
export default function TextField({ label, error, pill = false, className = '', id, ...rest }: TextFieldProps) {
  const inputId = id ?? `field-${label.toLowerCase().replace(/\s+/g, '-')}`;
  return (
    <div className="input-group">
      <label className="input-label" htmlFor={inputId}>{label}</label>
      <input
        id={inputId}
        className={`input-field ${pill ? 'input-field-pill' : ''} ${error ? 'is-error' : ''} ${className}`.trim()}
        {...rest}
      />
      {error && <span className="input-error" role="alert">{error}</span>}
    </div>
  );
}
