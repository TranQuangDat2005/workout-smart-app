/** Bản đồ enum → nhãn tiếng Việt, dùng chung cho các màn hình web. */

export const ACCOUNT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  BANNED: 'Bị khóa',
  DELETED: 'Đã xóa',
  active: 'Hoạt động',
  banned: 'Bị khóa',
  deleted: 'Đã xóa',
};

export const EXERCISE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Đã ẩn',
  active: 'Hoạt động',
  inactive: 'Đã ẩn',
};

export const CHALLENGE_STATUS_LABELS: Record<string, string> = {
  upcoming: 'Sắp diễn ra',
  open: 'Đang mở',
  active: 'Đang diễn ra',
  closed: 'Đã đóng',
  finished: 'Đã kết thúc',
  completed: 'Đã kết thúc',
};

export const SESSION_STATUS_LABELS: Record<string, string> = {
  active: 'Đang tập',
  completed: 'Hoàn thành',
  interrupted: 'Bị gián đoạn',
  expired: 'Hết hạn',
};

/** Lấy nhãn từ bản đồ; nếu không có thì trả về giá trị gốc. */
export function label(map: Record<string, string>, value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback;
  return map[value] ?? value;
}
