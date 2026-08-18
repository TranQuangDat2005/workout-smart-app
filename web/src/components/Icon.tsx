import type { ReactNode } from 'react';

/**
 * Icon SVG monochrome dùng chung — kế thừa `currentColor` để khớp DESIGN.md
 * (UI achromatic + accent xanh), thay thế emoji màu ở các vị trí chức năng.
 */
const paths: Record<string, ReactNode> = {
  flame: (
    <path d="M12 22c4.4 0 7-3.2 7-7 0-2.5-1.3-4.6-3-6.2-.4 1.3-1.3 2.3-2.6 2.7C13.1 9.4 12 7.7 12 5.5 9.5 7.2 8 9.7 8 12c0 3.8 2.6 7 4 10z" />
  ),
  chart: (
    <>
      <line x1="18" y1="20" x2="18" y2="10" />
      <line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" />
      <line x1="2" y1="20" x2="22" y2="20" />
    </>
  ),
  apple: (
    <>
      <path d="M12 6c-1-1.5-2.5-2.5-4-2.5C5 3.5 4 6 4 8c0 3 2.5 6.5 8 12 5.5-5.5 8-9 8-12 0-2-1-4.5-4-4.5-1.5 0-3 1-4 2.5z" />
      <path d="M12 6c0-1.5.5-3 1.5-3.5" />
    </>
  ),
  strength: (
    <path d="M6 4v16M18 4v16M2 8h4M18 8h4M2 16h4M18 16h4M6 12h12" />
  ),
  users: (
    <>
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </>
  ),
  trophy: (
    <>
      <polyline points="8 21 12 17 16 21" />
      <line x1="12" y1="17" x2="12" y2="11" />
      <path d="M7 4h10v6a5 5 0 0 1-10 0V4z" />
      <path d="M7 4H4v3a3 3 0 0 0 3 3" />
      <path d="M17 4h3v3a3 3 0 0 1-3 3" />
    </>
  ),
  book: (
    <>
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </>
  ),
  box: (
    <>
      <path d="M21 8l-9-5-9 5v8l9 5 9-5V8z" />
      <path d="M3 8l9 5 9-5" />
      <path d="M12 13v8" />
    </>
  ),
  block: (
    <>
      <circle cx="12" cy="12" r="10" />
      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </>
  ),
};

export type IconName = keyof typeof paths;

export default function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      {paths[name]}
    </svg>
  );
}
