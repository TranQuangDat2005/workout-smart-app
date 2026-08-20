/** Parse CSV nhập kho thực phẩm — 019c. Định dạng: Tên Món, Gram/ml, Protein, Carb, Fat, Calo. */

export interface FoodCsvRow {
  name: string;
  gram: number;
  protein: number;
  carb: number;
  fat: number;
  calories: number;
}

export interface FoodCsvParseResult {
  rows: FoodCsvRow[];
  errors: string[];
}

const HEADER_HINTS = ['tên', 'ten', 'name', 'gram', 'protein', 'carb', 'fat', 'calo', 'kcal'];

function toNumber(raw: string): number | null {
  let s = raw.trim();
  if (!s) return null;
  // Hỗ trợ số thập phân dùng dấu phẩy (vd "12,5") khi không xung đột với delimiter.
  if (s.includes(',') && !s.includes('.')) s = s.replace(',', '.');
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

export function parseFoodCsv(text: string): FoodCsvParseResult {
  const lines = text
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 0);
  if (lines.length === 0) {
    return { rows: [], errors: ['File rỗng'] };
  }

  // Tự nhận diện delimiter: tab → ; → ,
  const first = lines[0];
  const delimiter = first.includes('\t') ? '\t' : first.includes(';') ? ';' : ',';

  const rows: FoodCsvRow[] = [];
  const errors: string[] = [];

  lines.forEach((line, lineIndex) => {
    const cells = line.split(delimiter).map((c) => c.trim());
    // Bỏ qua dòng header (chứa từ khóa tên cột).
    if (lineIndex === 0 && HEADER_HINTS.some((h) => cells.join(' ').toLowerCase().includes(h))) {
      return;
    }
    if (cells.length < 6) {
      errors.push(`Dòng ${lineIndex + 1}: thiếu cột (cần: Tên Món, Gram/ml, Protein, Carb, Fat, Calo)`);
      return;
    }
    const name = cells[0];
    const gram = toNumber(cells[1]);
    const protein = toNumber(cells[2]);
    const carb = toNumber(cells[3]);
    const fat = toNumber(cells[4]);
    const calories = toNumber(cells[5]);
    if (!name) {
      errors.push(`Dòng ${lineIndex + 1}: thiếu tên món`);
      return;
    }
    if (gram == null || gram <= 0) {
      errors.push(`Dòng ${lineIndex + 1}: Gram/ml không hợp lệ`);
      return;
    }
    if (protein == null || carb == null || fat == null || calories == null
        || protein < 0 || carb < 0 || fat < 0 || calories < 0) {
      errors.push(`Dòng ${lineIndex + 1}: số liệu Protein/Carb/Fat/Calo không hợp lệ`);
      return;
    }
    rows.push({ name, gram, protein, carb, fat, calories });
  });

  return { rows, errors };
}
