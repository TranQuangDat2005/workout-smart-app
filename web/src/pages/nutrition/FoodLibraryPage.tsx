import { useEffect, useRef, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Modal from '../../components/Modal';
import Spinner from '../../components/Spinner';
import TextField from '../../components/TextField';
import { nutritionApi } from '../../services/nutritionApi';
import type { Food } from '../../services/nutritionApi';
import { parseFoodCsv } from './foodCsv';
import type { FoodCsvRow } from './foodCsv';

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
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Import CSV (019c)
  const fileRef = useRef<HTMLInputElement | null>(null);
  const [csvRows, setCsvRows] = useState<FoodCsvRow[]>([]);
  const [csvErrors, setCsvErrors] = useState<string[]>([]);
  const [importing, setImporting] = useState(false);

  const onFileChosen = async (file: File | undefined) => {
    if (!file) return;
    const text = await file.text();
    const result = parseFoodCsv(text);
    setCsvRows(result.rows);
    setCsvErrors(result.errors);
    setNotice('');
    setError('');
    if (fileRef.current) fileRef.current.value = '';
  };

  const runImport = async () => {
    if (csvRows.length === 0) return;
    if (!window.confirm(`Nhập ${csvRows.length} món vào kho cá nhân của bạn?`)) return;
    setImporting(true);
    setError('');
    setNotice('');
    try {
      const res = await nutritionApi.importFoods(csvRows);
      setNotice(`Đã nhập ${res.imported} món vào kho cá nhân.${res.errors.length > 0 ? ` Bỏ qua ${res.errors.length} dòng lỗi.` : ''}`);
      if (res.errors.length > 0) setError(res.errors.slice(0, 5).join(' · '));
      setCsvRows([]);
      setCsvErrors([]);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Nhập CSV thất bại');
    } finally {
      setImporting(false);
    }
  };

  const load = (p = page) =>
    nutritionApi.searchFoods(query, p, 10).then((res) => {
      setFoods(res.content);
      setTotalPages(res.totalPages > 0 ? res.totalPages : 1);
    });
  useEffect(() => {
    setListLoading(true);
    load(0)
      .catch(() => setError('Không thể tải kho thực phẩm'))
      .finally(() => setListLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const goToPage = (p: number) => {
    const target = Math.min(Math.max(p, 0), totalPages - 1);
    setPage(target);
    load(target).catch(() => setError('Không thể tải kho thực phẩm'));
  };

  const runSearch = () => {
    setPage(0);
    load(0).catch(() => setError('Không thể tải kho thực phẩm'));
  };

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
      <div className="page-header">
        <h1><Icon name="leaf" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Kho thực phẩm</h1>
        <Button variant="outlined" size="sm" onClick={() => fileRef.current?.click()}>
          <Icon name="clipboard" size={14} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Nhập từ CSV
        </Button>
      </div>

      {/* File input ẩn — mở khi bấm nút "Nhập từ CSV" */}
      <input
        ref={fileRef}
        type="file"
        accept=".csv,text/csv"
        onChange={(e) => void onFileChosen(e.target.files?.[0])}
        style={{ display: 'none' }}
      />

      {/* Add/edit form */}
      <div className="card">
        <h3 style={{ marginBottom: 16 }}>
          {editingId != null ? (
            <><Icon name="pencil" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Chỉnh sửa thực phẩm</>
          ) : (
            <><Icon name="plus" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Thêm thực phẩm vào kho cá nhân</>
          )}
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

      {/* Xem trước CSV — chỉ hiện sau khi chọn file */}
      {(csvRows.length > 0 || csvErrors.length > 0) && (
        <div className="card animate-slide-up">
          <h3 style={{ marginBottom: 10 }}><Icon name="clipboard" size={16} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Xem trước nhập CSV</h3>
          <p className="text-secondary" style={{ fontSize: 12, marginBottom: 12 }}>
            Cột theo thứ tự: <strong>Tên Món, Gram/ml, Protein, Carb, Fat, Calo</strong> — số liệu cho đúng lượng Gram/ml đó, hệ thống tự quy về 100g/ml.
          </p>
          <div className="text-secondary" style={{ fontSize: 13, marginBottom: 8 }}>
            <strong className="text-base">{csvRows.length}</strong> dòng hợp lệ
            {csvErrors.length > 0 && <> · <strong style={{ color: 'var(--text-warning)' }}>{csvErrors.length}</strong> dòng lỗi</>}
          </div>
          {csvErrors.slice(0, 5).map((msg, i) => (
            <div key={i} className="text-warning" style={{ fontSize: 12 }}>{msg}</div>
          ))}
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <Button onClick={() => void runImport()} loading={importing} disabled={csvRows.length === 0}>
              Nhập {csvRows.length > 0 ? `${csvRows.length} món` : ''} vào kho
            </Button>
            <Button variant="outlined" onClick={() => { setCsvRows([]); setCsvErrors([]); }}>
              Hủy
            </Button>
          </div>
        </div>
      )}

      {/* Search */}
      <div style={{ display: 'flex', gap: 10 }}>
        <div style={{ flex: 1 }}>
          <TextField
            label="Tìm kiếm"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && runSearch()}
            placeholder="Tên thực phẩm..."
          />
        </div>
        <Button variant="dark" onClick={runSearch} style={{ marginTop: 'auto' }}>Tìm</Button>
      </div>

      {listLoading && foods.length === 0 && <Spinner />}

      {/* Food list */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {foods.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon"><Icon name="leaf" size={42} /></div>
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

      {/* Pagination */}
    {foods.length > 0 && (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12 }}>
        <Button variant="dark" size="sm" onClick={() => goToPage(page - 1)} disabled={page === 0}>
          ← Trang trước
        </Button>
        <span className="text-secondary text-sm">
          Trang {page + 1} / {totalPages}
        </span>
        <Button variant="dark" size="sm" onClick={() => goToPage(page + 1)} disabled={page >= totalPages - 1}>
          Trang sau →
        </Button>
      </div>
    )}

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
