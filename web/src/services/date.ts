/** Trả về ngày hôm nay theo múi giờ địa phương dạng YYYY-MM-DD (tránh lệch ngày do UTC). */
export function todayLocalISO(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
