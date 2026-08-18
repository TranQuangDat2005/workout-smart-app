/** Spinner loading dùng chung, đồng nhất với design system. */
export default function Spinner({ size = 28 }: { size?: number }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
      <div
        className="btn-spinner"
        style={{ width: size, height: size, borderWidth: 3, color: 'var(--green)' }}
        aria-label="Đang tải"
      />
    </div>
  );
}
