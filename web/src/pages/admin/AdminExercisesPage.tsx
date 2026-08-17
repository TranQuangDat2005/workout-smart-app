import { useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { adminApi } from '../../services/adminApi';
import type { AdminExercise, ExerciseInput } from '../../services/adminApi';

export default function AdminExercisesPage() {
  const [q, setQ] = useState('');
  const [list, setList] = useState<AdminExercise[]>([]);
  const [name, setName] = useState('');
  const [equipment, setEquipment] = useState('body_weight');
  const [muscleGroup, setMuscleGroup] = useState('core');
  const [category, setCategory] = useState('strength');
  const [bodyPart, setBodyPart] = useState('');
  const [gifUrl, setGifUrl] = useState('');
  const [json, setJson] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

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

  const onCreate = async () => {
    setError('');
    try {
      await adminApi.createExercise({ name, equipment, muscleGroup, category, bodyPart, gifUrl });
      setNotice('Đã thêm bài tập.');
      setName('');
      setGifUrl('');
      onSearch();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Thêm thất bại');
    }
  };

  const onImport = async () => {
    setError('');
    try {
      const items = JSON.parse(json) as ExerciseInput[];
      const res = await adminApi.importExercises(items);
      setNotice(`Import xong: ${res.inserted} mới, ${res.updated} cập nhật, ${res.skipped} bỏ qua.`);
      setJson('');
      onSearch();
    } catch {
      setError('JSON không hợp lệ');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32, display: 'flex', justifyContent: 'center', gap: 24, flexWrap: 'wrap' }}>
      <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 24, width: '100%', maxWidth: 420, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h1 style={{ fontSize: 20, fontWeight: 700 }}>Danh sách bài tập</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          <TextField label="Tìm tên" value={q} onChange={(e) => setQ(e.target.value)} />
          <Button onClick={onSearch}>Tìm</Button>
        </div>
        {list.map((ex) => (
          <div key={ex.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--mid-dark)', padding: '8px 0' }}>
            <div>
              <strong>{ex.name}</strong>
              <span style={{ color: 'var(--text-secondary)' }}> · {ex.equipment} · {ex.muscleGroup} · {ex.status}</span>
            </div>
            <Button variant="outlined" onClick={() => onToggle(ex)}>
              {ex.status === 'active' ? 'Ẩn' : 'Hiện'}
            </Button>
          </div>
        ))}
      </div>

      <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 24, width: '100%', maxWidth: 420, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h1 style={{ fontSize: 20, fontWeight: 700 }}>Thêm bài tập</h1>
        <TextField label="Tên" value={name} onChange={(e) => setName(e.target.value)} />
        <TextField label="Dụng cụ" value={equipment} onChange={(e) => setEquipment(e.target.value)} />
        <TextField label="Nhóm cơ" value={muscleGroup} onChange={(e) => setMuscleGroup(e.target.value)} />
        <TextField label="Category" value={category} onChange={(e) => setCategory(e.target.value)} />
        <TextField label="Bộ phận cơ thể" value={bodyPart} onChange={(e) => setBodyPart(e.target.value)} />
        <TextField label="GIF URL" value={gifUrl} onChange={(e) => setGifUrl(e.target.value)} />
        <Button onClick={onCreate}>Thêm</Button>

        <h1 style={{ fontSize: 20, fontWeight: 700, marginTop: 16 }}>Import JSON</h1>
        <textarea
          value={json}
          onChange={(e) => setJson(e.target.value)}
          placeholder='[{"name":"...","equipment":"...","muscleGroup":"..."}]'
          style={{ background: 'var(--mid-dark)', color: 'var(--text-base)', borderRadius: 8, padding: 12, minHeight: 120 }}
        />
        <Button onClick={onImport}>Import</Button>
      </div>

      {notice && <p style={{ color: 'var(--text-announcement)', width: '100%' }}>{notice}</p>}
      {error && <p style={{ color: 'var(--text-negative)', width: '100%' }}>{error}</p>}
    </div>
  );
}
