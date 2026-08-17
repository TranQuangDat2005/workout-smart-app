import { useState } from 'react';
import Button from '../../components/Button';
import TextField from '../../components/TextField';
import { adminApi } from '../../services/adminApi';

export default function AdminExercisesPage() {
  const [name, setName] = useState('');
  const [equipment, setEquipment] = useState('body_weight');
  const [muscleGroup, setMuscleGroup] = useState('core');
  const [category, setCategory] = useState('strength');
  const [bodyPart, setBodyPart] = useState('');
  const [gifUrl, setGifUrl] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const onCreate = async () => {
    setError('');
    try {
      await adminApi.createExercise({ name, equipment, muscleGroup, category, bodyPart, gifUrl });
      setNotice('Đã thêm bài tập.');
      setName('');
      setGifUrl('');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Thêm thất bại');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32, display: 'flex', justifyContent: 'center' }}>
      <div style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 32, width: '100%', maxWidth: 560, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Thêm bài tập</h1>
        <TextField label="Tên" value={name} onChange={(e) => setName(e.target.value)} />
        <TextField label="Dụng cụ" value={equipment} onChange={(e) => setEquipment(e.target.value)} />
        <TextField label="Nhóm cơ" value={muscleGroup} onChange={(e) => setMuscleGroup(e.target.value)} />
        <TextField label="Category" value={category} onChange={(e) => setCategory(e.target.value)} />
        <TextField label="Bộ phận cơ thể" value={bodyPart} onChange={(e) => setBodyPart(e.target.value)} />
        <TextField label="GIF URL (path trong exercises-dataset)" value={gifUrl} onChange={(e) => setGifUrl(e.target.value)} />
        {notice && <span style={{ color: 'var(--text-announcement)' }}>{notice}</span>}
        {error && <span style={{ color: 'var(--text-negative)' }}>{error}</span>}
        <Button onClick={onCreate}>Thêm bài tập</Button>
      </div>
    </div>
  );
}
