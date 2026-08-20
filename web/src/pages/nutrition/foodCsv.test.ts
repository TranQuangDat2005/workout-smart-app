import { parseFoodCsv } from './foodCsv';

describe('parseFoodCsv (019c)', () => {
  it('parse CSV dấu phẩy, bỏ qua header', () => {
    const result = parseFoodCsv(
      'Tên Món, Gram/ml, Protein, Carb, Fat, Calo\nYến mạch,100,13,60,7,389\nSữa tươi,250,8,12,9,160',
    );

    expect(result.errors).toEqual([]);
    expect(result.rows).toEqual([
      { name: 'Yến mạch', gram: 100, protein: 13, carb: 60, fat: 7, calories: 389 },
      { name: 'Sữa tươi', gram: 250, protein: 8, carb: 12, fat: 9, calories: 160 },
    ]);
  });

  it('parse CSV dấu tab và dấu chấm phẩy', () => {
    expect(parseFoodCsv('Tên\tGram/ml\tProtein\tCarb\tFat\tCalo\nTrứng\t100\t13\t1\t11\t155').rows).toHaveLength(1);
    expect(parseFoodCsv('Tên;Gram;Protein;Carb;Fat;Calo\nGạo;100;7;77;1;360').rows[0]).toMatchObject({ name: 'Gạo' });
  });

  it('hỗ trợ số thập phân dấu phẩy khi delimiter không phải dấu phẩy', () => {
    const result = parseFoodCsv('Tên;Gram;Protein;Carb;Fat;Calo\nBơ;100;2;9;15;160,5');
    expect(result.rows[0]?.calories).toBe(160.5);
  });

  it('báo lỗi dòng thiếu cột / số liệu sai / gram bằng 0', () => {
    const result = parseFoodCsv(
      'Tên Món, Gram/ml, Protein, Carb, Fat, Calo\nThiếu cột,100,1,2\nSai số,100,x,2,3,4\nGram 0,Cơm,0,1,2,3,4',
    );
    expect(result.rows).toHaveLength(0);
    expect(result.errors).toHaveLength(3);
    expect(result.errors[0]).toContain('thiếu cột');
  });

  it('file rỗng báo lỗi', () => {
    expect(parseFoodCsv('')).toEqual({ rows: [], errors: ['File rỗng'] });
  });
});
