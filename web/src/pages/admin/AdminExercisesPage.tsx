import { useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import TextField from '../../components/TextField';
import { adminApi } from '../../services/adminApi';
import type { AdminExercise, ExerciseInput } from '../../services/adminApi';
import { EXERCISE_STATUS_LABELS, label } from '../../services/labels';

export default function AdminExercisesPage() {
  const [q, setQ] = useState('');
  const [list, setList] = useState<AdminExercise[]>([]);
  const [name, setName] = useState('');
  const [equipment, setEquipment] = useState('body_weight');
  const [muscleGroup, setMuscleGroup] = useState('core');
  const [category, setCategory] = useState('strength');
  const [bodyPart, setBodyPart] = useState('');
  const [gifUrl, setGifUrl] = useState('');
  const [image, setImage] = useState('');
  const [instructions, setInstructions] = useState('');
  const [json, setJson] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [tab, setTab] = useState<'create' | 'import'>('create');

  const onSearch = async () => {
    setError('');
    try {
      setList(await adminApi.listExercises(q));
    } catch {
      setError('Không thể tải danh sách');
    }
  };

  const onToggle = async (ex: AdminExercise) => {
    try {
      await adminApi.setExerciseStatus(ex.id, ex.status === 'ACTIVE' || ex.status === 'active' ? 'inactive' : 'active');
      onSearch();
    } catch {
      setError('Đổi trạng thái thất bại');
    }
  };

  const resetForm = () => {
    setName(''); setEquipment('body_weight'); setMuscleGroup('core'); setCategory('strength');
    setBodyPart(''); setGifUrl(''); setImage(''); setInstructions(''); setEditingId(null);
  };

  const startEdit = async (ex: AdminExercise) => {
    setError('');
    try {
      const d = await adminApi.getExercise(ex.id);
      setEditingId(d.id);
      setName(d.name);
      setEquipment(d.equipment ?? 'body_weight');
      setMuscleGroup(d.muscleGroup ?? 'core');
      setCategory(d.category ?? 'strength');
      setBodyPart(d.bodyPart ?? '');
      setGifUrl(d.gifUrl ?? '');
      setImage(d.image ?? '');
      setInstructions(d.instructions ?? '');
      setTab('create');
    } catch {
      setError('Không thể tải chi tiết bài tập');
    }
  };

  const onCreate = async () => {
    setError(''); setCreating(true);
    const body = {
      name,
      equipment,
      muscleGroup,
      category,
      bodyPart,
      gifUrl,
      image: image || undefined,
      instructions: instructions || undefined,
    };
    try {
      if (editingId != null) {
        await adminApi.updateExercise(editingId, body);
        setNotice('Đã cập nhật bài tập.');
      } else {
        await adminApi.createExercise(body);
        setNotice('Đã thêm bài tập thành công.');
      }
      resetForm();
      onSearch();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? (editingId != null ? 'Cập nhật thất bại' : 'Thêm thất bại'));
    } finally {
      setCreating(false);
    }
  };

  const onImport = async () => {
    setError(''); setImporting(true);
    try {
      const items = JSON.parse(json) as ExerciseInput[];
      const res = await adminApi.importExercises(items);
      setNotice(`Import xong: ${res.inserted} mới · ${res.updated} cập nhật · ${res.skipped} bỏ qua.`);
      setJson('');
      onSearch();
    } catch {
      setError('JSON không hợp lệ hoặc import thất bại');
    } finally {
      setImporting(false);
    }
  };

  const selectStyle = {
    background: 'var(--mid-dark)',
    color: 'var(--text-base)',
    border: 'none',
    borderRadius: 8,
    padding: '12px 16px',
    fontSize: 15,
    outline: 'none',
    boxShadow: 'var(--inset-border)',
    cursor: 'pointer',
    width: '100%',
  } as React.CSSProperties;

  return (
    <div className="page-container" style={{ maxWidth: 960, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <h1><Icon name="strength" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Quản lý bài tập</h1>
        {list.length > 0 && <span className="badge badge-neutral">{list.length} bài tập</span>}
      </div>

      {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
      {error  && <div className="notice notice-error">{error}</div>}

      <div className="grid-sidebar-main" style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: 20, alignItems: 'start' }}>
        {/* List panel */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '14px 18px', display: 'flex', gap: 10, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <div style={{ flex: 1 }}>
              <TextField
                label="Tìm tên bài tập"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && onSearch()}
                placeholder="Squat, Pull up..."
              />
            </div>
            <Button variant="dark" onClick={onSearch} style={{ marginTop: 'auto' }}>Tìm</Button>
          </div>
          {list.length === 0 ? (
            <div className="empty-state" style={{ padding: '32px 20px' }}>
              <p className="empty-state-text">Nhập từ khóa và tìm kiếm để xem danh sách.</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tên</th>
                  <th>Dụng cụ</th>
                  <th>Nhóm cơ</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {list.map((ex) => (
                  <tr key={ex.id}>
                    <td className="fw-600">{ex.name}</td>
                    <td className="text-secondary">{ex.equipment}</td>
                    <td className="text-secondary">{ex.muscleGroup}</td>
                    <td>
                      <span className={`badge ${ex.status === 'active' ? 'badge-green' : 'badge-neutral'}`}>
                        {label(EXERCISE_STATUS_LABELS, ex.status)}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <Button variant="dark" size="sm" onClick={() => void startEdit(ex)}>Sửa</Button>
                        <Button
                          variant={ex.status === 'active' ? 'outlined' : 'dark'}
                          size="sm"
                          onClick={() => onToggle(ex)}
                        >
                          {ex.status === 'active' ? 'Ẩn' : 'Hiện'}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Create/Import panel */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {/* Tab switcher */}
          <div style={{ display: 'flex', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            {(['create', 'import'] as const).map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                style={{
                  flex: 1,
                  padding: '12px 0',
                  border: 'none',
                  background: 'transparent',
                  color: tab === t ? 'var(--text-base)' : 'var(--text-secondary)',
                  fontWeight: tab === t ? 700 : 500,
                  fontSize: 13,
                  cursor: 'pointer',
                  borderBottom: tab === t ? '2px solid var(--green)' : '2px solid transparent',
                  transition: 'all var(--t-fast)',
                  textTransform: 'uppercase',
                  letterSpacing: 1,
                }}
              >
                {t === 'create' ? '+ Thêm mới' : <><Icon name="inbox" size={14} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Import JSON</>}
              </button>
            ))}
          </div>

          <div style={{ padding: 18, display: 'flex', flexDirection: 'column', gap: 12 }}>
            {tab === 'create' ? (
              <>
                {editingId != null && (
                  <div className="notice notice-info" style={{ fontSize: 12 }}>Đang chỉnh sửa bài tập #{editingId}</div>
                )}
                <TextField label="Tên bài tập *" value={name} onChange={(e) => setName(e.target.value)} placeholder="Pull up" />
                <div className="input-group">
                  <label className="input-label">Dụng cụ</label>
                  <select value={equipment} onChange={(e) => setEquipment(e.target.value)} style={selectStyle}>
                    {['body_weight', 'dumbbell', 'barbell', 'machine', 'resistance_band'].map((v) => (
                      <option key={v} value={v}>{v}</option>
                    ))}
                  </select>
                </div>
                <TextField label="Nhóm cơ" value={muscleGroup} onChange={(e) => setMuscleGroup(e.target.value)} placeholder="core, chest, back..." />
                <div className="input-group">
                  <label className="input-label">Category</label>
                  <select value={category} onChange={(e) => setCategory(e.target.value)} style={selectStyle}>
                    {['strength', 'cardio', 'flexibility', 'balance'].map((v) => (
                      <option key={v} value={v}>{v}</option>
                    ))}
                  </select>
                </div>
                <TextField label="Bộ phận cơ thể" value={bodyPart} onChange={(e) => setBodyPart(e.target.value)} placeholder="upper_body, lower_body..." />
                <TextField label="GIF URL (tùy chọn)" value={gifUrl} onChange={(e) => setGifUrl(e.target.value)} placeholder="https://..." />
                <TextField label="Ảnh thumbnail 180×180 (URL)" value={image} onChange={(e) => setImage(e.target.value)} placeholder="https://..." />
                <div className="input-group">
                  <label className="input-label">Hướng dẫn từng bước</label>
                  <textarea
                    value={instructions}
                    onChange={(e) => setInstructions(e.target.value)}
                    placeholder={'Bước 1: ...\nBước 2: ...'}
                    style={{
                      background: 'var(--mid-dark)',
                      color: 'var(--text-base)',
                      borderRadius: 8,
                      padding: 12,
                      minHeight: 90,
                      border: '1px solid var(--border-dark)',
                      fontSize: 13,
                      resize: 'vertical',
                      outline: 'none',
                      lineHeight: 1.5,
                    }}
                  />
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <Button onClick={onCreate} fullWidth loading={creating} disabled={!name.trim()}>
                    {editingId != null ? 'Lưu thay đổi' : 'Thêm bài tập'}
                  </Button>
                  {editingId != null && <Button variant="outlined" onClick={resetForm}>Hủy</Button>}
                </div>
              </>
            ) : (
              <>
                <p className="text-secondary text-sm">
                  JSON array theo format: <code style={{ color: 'var(--green)', fontSize: 11 }}>[{'{'}name, equipment, muscleGroup{'}'}]</code>
                </p>
                <textarea
                  value={json}
                  onChange={(e) => setJson(e.target.value)}
                  placeholder={'[{"name":"...","equipment":"...","muscleGroup":"...","category":"strength"}]'}
                  style={{
                    background: 'var(--mid-dark)',
                    color: 'var(--text-base)',
                    borderRadius: 8,
                    padding: 12,
                    minHeight: 160,
                    border: '1px solid var(--border-dark)',
                    fontSize: 12,
                    fontFamily: 'monospace',
                    resize: 'vertical',
                    outline: 'none',
                  }}
                />
                <Button onClick={onImport} fullWidth loading={importing} disabled={!json.trim()}>
                  Import JSON
                </Button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
