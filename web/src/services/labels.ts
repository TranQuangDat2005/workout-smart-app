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

/** Category kho dữ liệu (index.html) — 10 giá trị, trùng body_part. */
export const EXERCISE_CATEGORY_LABELS: Record<string, string> = {
  back: 'Lưng',
  cardio: 'Cardio',
  chest: 'Ngực',
  'lower arms': 'Cẳng tay',
  'lower legs': 'Cẳng chân',
  neck: 'Cổ',
  shoulders: 'Vai',
  'upper arms': 'Cánh tay',
  'upper legs': 'Đùi',
  waist: 'Eo / Bụng',
};

/** Equipment kho dữ liệu sau chuẩn hóa snake_case — 28 giá trị. */
export const EXERCISE_EQUIPMENT_LABELS: Record<string, string> = {
  assisted: 'Trợ lực',
  band: 'Band',
  barbell: 'Tạ đòn',
  body_weight: 'Cân nặng cơ thể',
  bosu_ball: 'Bóng Bosu',
  cable: 'Cáp',
  dumbbell: 'Tạ đơn',
  elliptical_machine: 'Máy elip',
  ez_barbell: 'Tạ EZ',
  hammer: 'Búa',
  kettlebell: 'Tạ ấm',
  leverage_machine: 'Máy đòn bẩy',
  medicine_ball: 'Bóng tập',
  olympic_barbell: 'Tạ Olympic',
  resistance_band: 'Dây kháng lực',
  roller: 'Con lăn',
  rope: 'Dây thừng',
  skierg_machine: 'Máy SkiErg',
  sled_machine: 'Máy trượt',
  smith_machine: 'Khung Smith',
  stability_ball: 'Bóng ổn định',
  stationary_bike: 'Xe đạp tại chỗ',
  stepmill_machine: 'Máy bước',
  tire: 'Lốp',
  trap_bar: 'Tạ trap bar',
  upper_body_ergometer: 'Máy tay',
  weighted: 'Có tạ',
  wheel_roller: 'Con lăn bụng',
};

export const EXERCISE_MUSCLE_GROUP_LABELS: Record<string, string> = {
  chest: 'Ngực',
  back: 'Lưng',
  shoulders: 'Vai',
  arms: 'Tay',
  legs: 'Chân',
  core: 'Bụng / Core',
};

export const EXERCISE_CATEGORIES = Object.keys(EXERCISE_CATEGORY_LABELS);
export const EXERCISE_EQUIPMENTS = Object.keys(EXERCISE_EQUIPMENT_LABELS);
export const EXERCISE_MUSCLE_GROUPS = Object.keys(EXERCISE_MUSCLE_GROUP_LABELS);
