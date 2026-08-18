import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import Spinner from '../../components/Spinner';
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
  const [loading, setLoading] = useState(false);
  const [deletingFood, setDeletingFood] = useState<Food | null>(null);
  const [listLoading, setListLoading] = useState(true);

  const load = () => nutritionApi.searchFoods(query).then((page) => setFoods(page.content));
  useEffect(() => {
    setListLoading(true);
    nutritionApi
      .searchFoods('')
      .then((page) => setFoods(page.content))
      .catch(() => setError('Không thể tải kho thực phẩm'))
      .finally(() => setListLoading(false));
  }, []);

  const resetForm = () => {
    setName(''); setCalories(''); setProtein('0'); setCarb('0'); setFat('0'); setEditingId(null);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice(''); setLoading(true);
    const body = {
      name, caloriesPer100g: Number(calories),
      proteinPer100g: Number(protein), carbPer100g: Number(carb), fatPer100g: Number(fat),
    };
    try {
      if (editingId != null) {
        await nutritionApi.updateFood(editingId, body);
        setNotice('Đã cập nhật thực phẩm.');
      } else {
        await nutritionApi.createFood(body);
        setNotice('Đã thêm thực phẩm vào kho cá nhân.');
      }
      resetForm(); load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu thất bại');
    } finally {
      setLoading(false);
    }
  };

  const startEdit = (food: Food) => {
    setEditingId(food.id); setName(food.name); setCalories(String(food.caloriesPer100g));
    setProtein(String(food.proteinPer100g)); setCarb(String(food.carbPer100g)); setFat(String(food.fatPer100g));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const openDelete = (food: Food) => setDeletingFood(food);

  const confirmDelete = async () => {
    if (!deletingFood) return;
    try {
      await nutritionApi.deleteFood(deletingFood.id);
      setNotice('Đã xóa (giữ hiển thị trong lịch sử 1 tuần).');
      setDeletingFood(null);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Xóa thất bại');
    }
  };

  return (
    <div className="page-container" style={{ maxWidth: 860, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <h1>🥗 Kho thực phẩm</h1>

      {/* Add/edit form */}
      <div className="card">
        <h3 style={{ marginBottom: 16 }}>
          {editingId != null ? '✏️ Chỉnh sửa thực phẩm' : '➕ Thêm thực phẩm vào kho cá nhân'}
        </h3>
        <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <TextField label="Tên thực phẩm *" value={name} onChange={(e) => setName(e.target.value)} required placeholder="Cơm trắng, ức gà, ..." />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
            <TextField label="Calo/100g *" type="number" value={calories} onChange={(e) => setCalories(e.target.value)} required placeholder="130" min="0" />
            <TextField label="Protein/100g" type="number" value={protein} onChange={(e) => setProtein(e.target.value)} placeholder="3" step="0.1" min="0" />
            <TextField label="Carb/100g" type="number" value={carb} onChange={(e) => setCarb(e.target.value)} placeholder="28" step="0.1" min="0" />
            <TextField label="Fat/100g" type="number" value={fat} onChange={(e) => setFat(e.target.value)} placeholder="0.3" step="0.1" min="0" />
          </div>

          {notice && <div className="notice notice-success">{notice}</div>}
          {error  && <div className="notice notice-error">{error}</div>}

          <div style={{ display: 'flex', gap: 10 }}>
            <Button type="submit" style={{ flex: 1 }} loading={loading}>
              {editingId != null ? 'Lưu thay đổi' : 'Thêm vào kho'}
            </Button>
            {editingId != null && (
              <Button variant="outlined" onClick={resetForm}>Hủy</Button>
            )}
          </div>
        </form>
      </div>

      {/* Search */}
      <div style={{ display: 'flex', gap: 10 }}>
        <div style={{ flex: 1 }}>
          <TextField
            label="Tìm kiếm"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && load()}
            placeholder="Tên thực phẩm..."
          />
        </div>
        <Button variant="dark" onClick={() => load()} style={{ marginTop: 'auto' }}>Tìm</Button>
      </div>

      {listLoading && foods.length === 0 && <Spinner />}

      {/* Food list */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {foods.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🥗</div>
            <p className="empty-state-text">Không tìm thấy thực phẩm nào.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Tên</th>
                <th>Calo/100g</th>
                <th>P / C / F</th>
                <th>Nguồn</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {foods.map((food) => (
                <tr key={food.id}>
                  <td className="fw-600">{food.name}</td>
                  <td>{food.caloriesPer100g} kcal</td>
                  <td className="text-secondary" style={{ fontSize: 12 }}>
                    {food.proteinPer100g}g / {food.carbPer100g}g / {food.fatPer100g}g
                  </td>
                  <td>
                    <span className={`badge ${food.source === 'user_custom' ? 'badge-green' : 'badge-neutral'}`}>
                      {food.source === 'user_custom' ? 'Của bạn' : 'Hệ thống'}
                    </span>
                  </td>
                  <td>
                    {food.source === 'user_custom' && (
                      <div style={{ display: 'flex', gap: 8 }}>
                        <Button variant="dark" size="sm" onClick={() => startEdit(food)}>Sửa</Button>
                        <Button variant="danger" size="sm" onClick={() => openDelete(food)}>Xóa</Button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal
        open={deletingFood != null}
        title="Xóa thực phẩm"
        onClose={() => setDeletingFood(null)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setDeletingFood(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => void confirmDelete()}>Xóa</Button>
          </>
        }
      >
        <p className="text-secondary text-sm" style={{ lineHeight: 1.7 }}>
          Xóa &quot;{deletingFood?.name}&quot; khỏi kho cá nhân? Thực phẩm sẽ bị ẩn khỏi tìm kiếm nhưng vẫn hiển thị trong lịch sử 1 tuần.
        </p>
      </Modal>
    </div>
  );
}
