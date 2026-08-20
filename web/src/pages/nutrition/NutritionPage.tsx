import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Modal from '../../components/Modal';
import Spinner from '../../components/Spinner';
import TextField from '../../components/TextField';
import { nutritionApi } from '../../services/nutritionApi';
import type { Food, Meal, NutritionSummary } from '../../services/nutritionApi';
import { todayLocalISO } from '../../services/date';

interface DraftEntry {
  foodItemId: number;
  foodName: string;
  portionGrams: number;
  calories: number;
  protein: number;
  carb: number;
  fat: number;
}

function FoodChip({ food, onAdd }: { food: Food; onAdd: (grams: number) => void }) {
  const [grams, setGrams] = useState('100');
  return (
    <div
      style={{
        background: 'var(--mid-dark)',
        borderRadius: 8,
        padding: '8px 12px',
        fontSize: 13,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        border: '1px solid var(--border-dark)',
      }}
    >
      <span className="fw-600">{food.name}</span>
      <input
        type="number"
        value={grams}
        onChange={(e) => setGrams(e.target.value)}
        min="0"
        step="any"
        style={{
          width: 56,
          background: 'var(--near-black)',
          color: 'var(--text-base)',
          border: 'none',
          borderRadius: 4,
          padding: '4px 6px',
          fontSize: 13,
          outline: 'none',
        }}
      />
      <span className="text-muted">g</span>
      <button
        className="btn btn-primary btn-sm"
        style={{ padding: '4px 12px', fontSize: 12, letterSpacing: 0 }}
        onClick={() => onAdd(Number(grams))}
      >
        +
      </button>
    </div>
  );
}

export default function NutritionPage() {
  const [date, setDate] = useState(todayLocalISO());
  const [meals, setMeals] = useState<Meal[]>([]);
  const [summary, setSummary] = useState<NutritionSummary | null>(null);
  const [foods, setFoods] = useState<Food[]>([]);
  const [query, setQuery] = useState('');
  const [mealNumber, setMealNumber] = useState('1');
  const [entries, setEntries] = useState<DraftEntry[]>([]);
  const [editingMealId, setEditingMealId] = useState<number | null>(null);
  const [deletingMeal, setDeletingMeal] = useState<Meal | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    void Promise.allSettled([
      nutritionApi.getMeals(date).then(setMeals).catch(() => setError('Không thể tải bữa ăn')),
      nutritionApi.getSummary(date).then(setSummary).catch(() => setSummary(null)),
    ]).finally(() => setLoading(false));
  };

  useEffect(load, [date]);

  const searchFoods = () => {
    nutritionApi.searchFoods(query).then((page) => setFoods(page.content));
  };

  const addEntry = (food: Food, grams: number) => {
    if (grams <= 0) return;
    const factor = grams / 100;
    setEntries((prev) => [
      ...prev,
      {
        foodItemId: food.id,
        foodName: food.name,
        portionGrams: grams,
        calories: food.caloriesPer100g * factor,
        protein: food.proteinPer100g * factor,
        carb: food.carbPer100g * factor,
        fat: food.fatPer100g * factor,
      },
    ]);
  };

  const removeEntry = (index: number) => setEntries((prev) => prev.filter((_, i) => i !== index));

  const saveMeal = async () => {
    setError(''); setNotice('');
    if (entries.length === 0) { setError('Cần ít nhất 1 món'); return; }
    const body = {
      mealNumber: Number(mealNumber),
      logDate: date,
      entries: entries.map(({ foodItemId, portionGrams }) => ({ foodItemId, portionGrams })),
    };
    try {
      if (editingMealId != null) {
        await nutritionApi.updateMeal(editingMealId, body);
        setNotice('Đã cập nhật bữa ăn.');
      } else {
        await nutritionApi.createMeal(body);
        setNotice('Đã thêm bữa ăn.');
      }
      setEntries([]); setEditingMealId(null); setMealNumber('1');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu bữa ăn thất bại');
    }
  };

  const confirmDeleteMeal = async () => {
    if (!deletingMeal) return;
    try {
      await nutritionApi.deleteMeal(deletingMeal.id);
      setNotice(`Đã xóa bữa ${deletingMeal.mealNumber}.`);
      setDeletingMeal(null);
      // Đang sửa bữa vừa xóa → thoát chế độ sửa.
      if (editingMealId === deletingMeal.id) {
        setEditingMealId(null);
        setEntries([]);
        setMealNumber('1');
      }
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Xóa bữa thất bại');
    }
  };

  const startEdit = (meal: Meal) => {
    setEditingMealId(meal.id);
    setMealNumber(String(meal.mealNumber));
    setEntries(
      meal.entries.map((e) => ({
        foodItemId: e.foodItemId,
        foodName: e.foodName,
        portionGrams: e.portionGrams,
        calories: e.totalCalories,
        protein: e.totalProtein,
        carb: e.totalCarb,
        fat: e.totalFat,
      })),
    );
  };

  const caloPct = summary && summary.targetCalories > 0
    ? Math.min(100, Math.round((summary.totalCalories / summary.targetCalories) * 100))
    : 0;
  const isOverCalo = summary?.status === 'thừa';
  const totalCalories = Math.round(entries.reduce((s, e) => s + e.calories, 0));
  const totalProtein = entries.reduce((s, e) => s + e.protein, 0);
  const totalCarb = entries.reduce((s, e) => s + e.carb, 0);
  const totalFat = entries.reduce((s, e) => s + e.fat, 0);

  return (
    <div className="page-container" style={{ maxWidth: 860, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div className="page-header">
        <h1><Icon name="apple" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Thực đơn</h1>
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          style={{
            background: 'var(--mid-dark)',
            color: 'var(--text-base)',
            border: '1px solid var(--border-dark)',
            borderRadius: 'var(--r-pill)',
            padding: '8px 16px',
            fontSize: 13,
            outline: 'none',
            cursor: 'pointer',
          }}
        />
      </div>

      {loading && meals.length === 0 && <Spinner />}

      {/* Summary panel */}
      {summary && (
        <div className="card">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))', gap: 12, marginBottom: 16 }}>
            {[
              { label: 'Đã nạp', val: `${summary.totalCalories} kcal`, color: isOverCalo ? 'var(--text-warning)' : 'var(--text-base)' },
              { label: 'Mục tiêu', val: `${summary.targetCalories} kcal`, color: 'var(--text-base)' },
              { label: 'Chênh lệch', val: `${summary.deficitOrSurplus > 0 ? '+' : ''}${summary.deficitOrSurplus} kcal`, color: isOverCalo ? 'var(--text-negative)' : 'var(--green)' },
              { label: 'Trạng thái', val: summary.status, color: isOverCalo ? 'var(--text-warning)' : 'var(--green)' },
            ].map((item) => (
              <div key={item.label} style={{ background: 'var(--mid-dark)', borderRadius: 8, padding: '10px 14px' }}>
                <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4 }}>{item.label}</div>
                <div style={{ fontWeight: 700, fontSize: 16, color: item.color }}>{item.val}</div>
              </div>
            ))}
          </div>
          <div className="progress-bar" style={{ height: 8 }}>
            <div
              className="progress-fill"
              style={{
                width: `${caloPct}%`,
                background: isOverCalo ? 'var(--text-warning)' : 'var(--green)',
              }}
            />
          </div>
          {/* Macro trong ngày — dạng cột: Đã nạp / Mục tiêu / Chênh lệch */}
          <table className="data-table" style={{ marginTop: 14 }}>
            <thead>
              <tr>
                <th>Chất</th>
                <th>Đã nạp</th>
                <th>Mục tiêu</th>
                <th>Chênh lệch</th>
              </tr>
            </thead>
            <tbody>
              {[
                { name: 'Calo', unit: 'kcal', total: summary.totalCalories, target: summary.targetCalories },
                { name: 'Protein', unit: 'g', total: summary.totalProtein, target: summary.targetProtein },
                { name: 'Carb', unit: 'g', total: summary.totalCarb, target: summary.targetCarb },
                { name: 'Fat', unit: 'g', total: summary.totalFat, target: summary.targetFat },
              ].map((row) => {
                const delta = Math.round(row.total - row.target);
                return (
                  <tr key={row.name}>
                    <td className="fw-700">{row.name}</td>
                    <td>{Math.round(row.total)} {row.unit}</td>
                    <td className="text-secondary">{Math.round(row.target)} {row.unit}</td>
                    <td className={delta > 0 ? 'text-warning fw-600' : delta < 0 ? 'text-green fw-600' : 'text-muted'}>
                      {delta > 0 ? '+' : ''}{delta} {row.unit}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
      {error   && <div className="notice notice-error">{error}</div>}

      {/* Meals list */}
      {meals.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <div className="empty-state-icon"><Icon name="utensils" size={42} /></div>
            <p className="empty-state-text">Chưa có bữa ăn nào được ghi nhận hôm nay.</p>
          </div>
        </div>
      ) : (
        meals.map((meal) => (
          <div key={meal.id} className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <div>
                <span className="fw-700"><Icon name="utensils" size={14} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Bữa {meal.mealNumber}</span>
                <span className="badge badge-neutral" style={{ marginLeft: 10 }}>{meal.totalCalories} kcal</span>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <Button variant="dark" size="sm" onClick={() => startEdit(meal)}>Sửa</Button>
                <Button variant="danger" size="sm" onClick={() => setDeletingMeal(meal)}>Xóa</Button>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
              {meal.entries.map((e) => (
                <div key={e.id} className="row-item" style={{ fontSize: 13 }}>
                  <span className="text-secondary">{e.foodName}</span>
                  <span>
                    <span className="fw-600">{e.portionGrams}g</span>
                    <span className="text-muted" style={{ marginLeft: 8 }}>{e.totalCalories} kcal</span>
                  </span>
                </div>
              ))}
            </div>
          </div>
        ))
      )}

      {/* Add/edit meal form */}
      <div className="card">
        <h3 style={{ marginBottom: 16 }}>
          {editingMealId != null ? (
            <><Icon name="pencil" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Sửa bữa {mealNumber}</>
          ) : (
            <><Icon name="plus" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Thêm bữa ăn</>
          )}
        </h3>

        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
          <div style={{ width: 100 }}>
            <TextField label="Bữa số" type="number" value={mealNumber} onChange={(e) => setMealNumber(e.target.value)} min="1" max="10" />
          </div>
          <div style={{ flex: 1, minWidth: 180 }}>
            <TextField
              label="Tìm thực phẩm"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && searchFoods()}
              placeholder="Nhập tên thực phẩm..."
            />
          </div>
          <Button variant="dark" onClick={searchFoods} style={{ marginTop: 'auto' }}>Tìm</Button>
        </div>

        {/* Food search results */}
        {foods.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 14 }}>
            {foods.map((food) => (
              <FoodChip key={food.id} food={food} onAdd={(grams) => addEntry(food, grams)} />
            ))}
          </div>
        )}

        {/* Cart entries */}
        {entries.length > 0 && (
          <div className="card" style={{ background: 'var(--mid-dark)', marginBottom: 14, padding: 14 }}>
            <h4 className="text-secondary" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 }}>Món đã chọn</h4>
            {entries.map((e, i) => (
              <div key={i} className="row-item" style={{ fontSize: 13 }}>
                <div>
                  <div className="fw-600">{e.foodName}</div>
                  <div className="text-secondary" style={{ fontSize: 12 }}>
                    {e.portionGrams}g · {Math.round(e.calories)} kcal · P {e.protein.toFixed(1)}g · C {e.carb.toFixed(1)}g · F {e.fat.toFixed(1)}g
                  </div>
                </div>
                <button
                  onClick={() => removeEntry(i)}
                  style={{ background: 'none', border: 'none', color: 'var(--text-negative)', cursor: 'pointer', fontSize: 16, lineHeight: 1, padding: 4 }}
                >
                  ×
                </button>
              </div>
            ))}
            <div className="row-item" style={{ fontWeight: 700 }}>
              <span>Tổng bữa</span>
              <span>
                {totalCalories} kcal · P {totalProtein.toFixed(1)}g · C {totalCarb.toFixed(1)}g · F {totalFat.toFixed(1)}g
              </span>
            </div>
          </div>
        )}

        <div style={{ display: 'flex', gap: 10 }}>
          <Button onClick={saveMeal} style={{ flex: 1 }}>
            {editingMealId != null ? 'Lưu thay đổi' : 'Lưu bữa ăn'}
          </Button>
          {editingMealId != null && (
            <Button variant="outlined" onClick={() => { setEditingMealId(null); setEntries([]); setMealNumber('1'); }}>
              Hủy
            </Button>
          )}
        </div>
      </div>

      <Modal
        open={deletingMeal != null}
        title="Xóa bữa ăn"
        onClose={() => setDeletingMeal(null)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setDeletingMeal(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => void confirmDeleteMeal()}>Xóa</Button>
          </>
        }
      >
        <p className="text-secondary text-sm" style={{ lineHeight: 1.7 }}>
          Xóa Bữa {deletingMeal?.mealNumber} ngày {deletingMeal ? new Date(deletingMeal.logDate).toLocaleDateString('vi-VN') : ''}? Các món trong bữa sẽ bị xóa theo.
        </p>
      </Modal>
    </div>
  );
}
