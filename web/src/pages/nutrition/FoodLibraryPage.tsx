import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { nutritionApi } from '../../services/nutritionApi';
import type { Food } from '../../services/nutritionApi';

export default function FoodLibraryPage() {
  const [foods, setFoods] = useState<Food[]>([]);
  const [query, setQuery] = useState('');
  const [name, setName] = useState('');
  const [calories, setCalories] = useState('');
  const [protein, setProtein] = useState('0');
  const [carb, setCarb] = useState('0');
  const [fat, setFat] = useState('0');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = () => nutritionApi.searchFoods(query).then((page) => setFoods(page.content));

  useEffect(() => {
    load().catch(() => setError('Không thể tải kho thực phẩm'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const resetForm = () => {
    setName(''); setCalories(''); setProtein('0'); setCarb('0'); setFat('0'); setEditingId(null);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    const body = {
      name,
      caloriesPer100g: Number(calories),
      proteinPer100g: Number(protein),
      carbPer100g: Number(carb),
      fatPer100g: Number(fat),
    };
    try {
      if (editingId != null) {
        await nutritionApi.updateFood(editingId, body);
        setNotice('Đã cập nhật thực phẩm.');
      } else {
        await nutritionApi.createFood(body);
        setNotice('Đã thêm thực phẩm vào kho cá nhân.');
      }
      resetForm();
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu thất bại');
    }
  };

  const startEdit = (food: Food) => {
    setEditingId(food.id);
    setName(food.name);
    setCalories(String(food.caloriesPer100g));
    setProtein(String(food.proteinPer100g));
    setCarb(String(food.carbPer100g));
    setFat(String(food.fatPer100g));
  };

  const removeFood = async (food: Food) => {
    if (!window.confirm(`Xóa "${food.name}" khỏi kho cá nhân?`)) return;
    try {
      await nutritionApi.deleteFood(food.id);
      setNotice('Đã xóa (giữ hiển thị trong lịch sử 1 tuần).');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Xóa thất bại');
    }
  };

  return (
    <div style={{  }}>
      <div style={{ maxWidth: 860, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Kho thực phẩm</h1>

        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <TextField label="Tìm kiếm" value={query} onChange={(e) => setQuery(e.target.value)} />
          </div>
          <Button variant="dark" onClick={() => load()}>Tìm</Button>
        </div>

        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {foods.map((food) => (
            <div key={food.id} style={{
              background: 'var(--dark-surface)', borderRadius: 6, padding: '12px 16px',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>
                  {food.name} {food.source === 'user_custom' && <span style={{ color: 'var(--green)', fontSize: 12 }}>(của bạn)</span>}
                </div>
                <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                  {food.caloriesPer100g} kcal · P {food.proteinPer100g}g · C {food.carbPer100g}g · F {food.fatPer100g}g /100g
                </div>
              </div>
              {food.source === 'user_custom' && (
                <div style={{ display: 'flex', gap: 8 }}>
                  <Button variant="dark" onClick={() => startEdit(food)}>Sửa</Button>
                  <Button variant="outlined" onClick={() => removeFood(food)}>Xóa</Button>
                </div>
              )}
            </div>
          ))}
          {foods.length === 0 && (
            <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Không tìm thấy thực phẩm nào.</p>
          )}
        </div>

        <form onSubmit={submit} style={{
          background: 'var(--dark-surface)', borderRadius: 8, padding: 16,
          display: 'flex', flexDirection: 'column', gap: 12,
        }}>
          <b style={{ fontSize: 16 }}>{editingId != null ? 'Sửa thực phẩm' : 'Thêm thực phẩm mới (kho cá nhân)'}</b>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <div style={{ flex: 2, minWidth: 180 }}>
              <TextField label="Tên" value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <TextField label="Calo/100g" type="number" value={calories} onChange={(e) => setCalories(e.target.value)} required />
            <TextField label="Protein/100g" type="number" value={protein} onChange={(e) => setProtein(e.target.value)} required />
            <TextField label="Carb/100g" type="number" value={carb} onChange={(e) => setCarb(e.target.value)} required />
            <TextField label="Fat/100g" type="number" value={fat} onChange={(e) => setFat(e.target.value)} required />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <Button type="submit">{editingId != null ? 'Lưu thay đổi' : 'Thêm vào kho'}</Button>
            {editingId != null && <Button variant="outlined" onClick={resetForm}>Hủy</Button>}
          </div>
        </form>
      </div>
    </div>
  );
}
