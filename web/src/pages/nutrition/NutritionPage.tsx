import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { nutritionApi } from '../../services/nutritionApi';
import type { Food, Meal, MealEntryInput, NutritionSummary } from '../../services/nutritionApi';

export default function NutritionPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [date, setDate] = useState(today);
  const [meals, setMeals] = useState<Meal[]>([]);
  const [summary, setSummary] = useState<NutritionSummary | null>(null);
  const [foods, setFoods] = useState<Food[]>([]);
  const [query, setQuery] = useState('');
  const [mealNumber, setMealNumber] = useState('1');
  const [entries, setEntries] = useState<MealEntryInput[]>([]);
  const [editingMealId, setEditingMealId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = () => {
    nutritionApi.getMeals(date).then(setMeals).catch(() => setError('Không thể tải bữa ăn'));
    nutritionApi
      .getSummary(date)
      .then(setSummary)
      .catch((err: unknown) => {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
        if (msg) setError(msg);
        setSummary(null);
      });
  };

  useEffect(load, [date]);

  const searchFoods = () => {
    nutritionApi.searchFoods(query).then((page) => setFoods(page.content));
  };

  const addEntry = (food: Food, grams: number) => {
    if (grams <= 0) return;
    setEntries((prev) => [...prev, { foodItemId: food.id, portionGrams: grams }]);
  };

  const removeEntry = (index: number) => {
    setEntries((prev) => prev.filter((_, i) => i !== index));
  };

  const saveMeal = async () => {
    setError('');
    setNotice('');
    if (entries.length === 0) {
      setError('Cần ít nhất 1 món');
      return;
    }
    const body = { mealNumber: Number(mealNumber), logDate: date, entries };
    try {
      if (editingMealId != null) {
        await nutritionApi.updateMeal(editingMealId, body);
        setNotice('Đã sửa bữa ăn.');
      } else {
        await nutritionApi.createMeal(body);
        setNotice('Đã thêm bữa ăn.');
      }
      setEntries([]);
      setEditingMealId(null);
      setMealNumber('1');
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu bữa ăn thất bại');
    }
  };

  const startEdit = (meal: Meal) => {
    setEditingMealId(meal.id);
    setMealNumber(String(meal.mealNumber));
    setEntries(meal.entries.map((e) => ({ foodItemId: e.foodItemId, portionGrams: e.portionGrams })));
  };

  const caloPct = summary && summary.targetCalories > 0
    ? Math.min(100, Math.round((summary.totalCalories / summary.targetCalories) * 100))
    : 0;

  return (
    <div style={{  }}>
      <div style={{ maxWidth: 860, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <h1 style={{ fontSize: 24, fontWeight: 700 }}>Dinh dưỡng</h1>
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            style={{
              background: 'var(--mid-dark)', color: 'var(--text-base)',
              border: '1px solid transparent', borderRadius: 500, padding: '8px 16px', fontSize: 14,
            }}
          />
        </div>

        {summary && (
          <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
              <span>Đã nạp: <b>{summary.totalCalories} kcal</b></span>
              <span>Mục tiêu: <b>{summary.targetCalories} kcal</b></span>
              <span style={{ color: 'var(--text-announcement)' }}>
                {summary.deficitOrSurplus >= 0 ? '+' : ''}{summary.deficitOrSurplus} kcal ({summary.status})
              </span>
            </div>
            <div style={{ background: 'var(--mid-dark)', borderRadius: 9999, height: 12, marginTop: 8, overflow: 'hidden' }}>
              <div style={{
                width: `${caloPct}%`, height: '100%',
                background: summary.status === 'thừa' ? 'var(--text-negative)' : 'var(--green)',
                borderRadius: 9999,
              }} />
            </div>
            <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-secondary)' }}>
              Protein {summary.totalProtein}g · Carb {summary.totalCarb}g · Fat {summary.totalFat}g
            </div>
          </div>
        )}

        {error && <span style={{ fontSize: 12, color: 'var(--text-negative)' }}>{error}</span>}
        {notice && <span style={{ fontSize: 12, color: 'var(--text-announcement)' }}>{notice}</span>}

        {/* Danh sách bữa */}
        {meals.map((meal) => (
          <div key={meal.id} style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <b>Bữa {meal.mealNumber}</b>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <span style={{ fontSize: 14, color: 'var(--text-secondary)' }}>{meal.totalCalories} kcal</span>
                <Button variant="dark" onClick={() => startEdit(meal)}>Sửa</Button>
              </div>
            </div>
            <ul style={{ marginTop: 8, fontSize: 14, color: 'var(--text-secondary)', paddingLeft: 20 }}>
              {meal.entries.map((e) => (
                <li key={e.id}>{e.foodName} — {e.portionGrams}g ({e.totalCalories} kcal)</li>
              ))}
            </ul>
          </div>
        ))}

        {/* Form thêm/sửa bữa */}
        <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <b>{editingMealId != null ? `Sửa bữa ${mealNumber}` : 'Thêm bữa ăn'}</b>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
            <TextField label="Bữa số" type="number" value={mealNumber}
              onChange={(e) => setMealNumber(e.target.value)} />
            <div style={{ flex: 1 }}>
              <TextField label="Tìm thực phẩm" value={query}
                onChange={(e) => setQuery(e.target.value)} />
            </div>
            <Button variant="dark" onClick={searchFoods}>Tìm</Button>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {foods.map((food) => (
              <FoodChip key={food.id} food={food} onAdd={(grams) => addEntry(food, grams)} />
            ))}
          </div>
          {entries.length > 0 && (
            <div style={{ fontSize: 14 }}>
              {entries.map((e, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
                  <span>Món #{e.foodItemId} — {e.portionGrams}g</span>
                  <button style={{ background: 'none', border: 'none', color: 'var(--text-negative)', cursor: 'pointer' }}
                    onClick={() => removeEntry(i)}>X</button>
                </div>
              ))}
            </div>
          )}
          <div style={{ display: 'flex', gap: 8 }}>
            <Button onClick={saveMeal}>{editingMealId != null ? 'Lưu thay đổi' : 'Lưu bữa ăn'}</Button>
            {editingMealId != null && (
              <Button variant="outlined" onClick={() => { setEditingMealId(null); setEntries([]); setMealNumber('1'); }}>
                Hủy
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function FoodChip({ food, onAdd }: { food: Food; onAdd: (grams: number) => void }) {
  const [grams, setGrams] = useState('100');
  return (
    <div style={{
      background: 'var(--mid-dark)', borderRadius: 6, padding: 8, fontSize: 12,
      display: 'flex', alignItems: 'center', gap: 6,
    }}>
      <span>{food.name}</span>
      <input
        type="number" value={grams} onChange={(e) => setGrams(e.target.value)}
        style={{ width: 60, background: 'var(--near-black)', color: 'var(--text-base)', border: 'none', borderRadius: 4, padding: '4px 6px' }}
      />
      <span>g</span>
      <button style={{ background: 'var(--green)', color: '#000', border: 'none', borderRadius: 9999, padding: '4px 10px', cursor: 'pointer', fontWeight: 700 }}
        onClick={() => onAdd(Number(grams))}>+</button>
    </div>
  );
}
