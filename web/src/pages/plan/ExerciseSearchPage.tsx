import { useState } from 'react';
import Button from '../../components/Button';
import ExerciseBrowser from '../../components/ExerciseBrowser';
import Icon from '../../components/Icon';
import Modal from '../../components/Modal';
import TextField from '../../components/TextField';
import { planApi } from '../../services/planApi';
import type { ExerciseDetail } from '../../services/planApi';
import {
  EXERCISE_CATEGORIES,
  EXERCISE_CATEGORY_LABELS,
  EXERCISE_EQUIPMENT_LABELS,
  EXERCISE_EQUIPMENTS,
  EXERCISE_MUSCLE_GROUP_LABELS,
  EXERCISE_MUSCLE_GROUPS,
  label,
} from '../../services/labels';

export default function ExerciseSearchPage() {
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState('');
  const [refreshNonce, setRefreshNonce] = useState(0);

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formName, setFormName] = useState('');
  const [formCategory, setFormCategory] = useState('chest');
  const [formMuscleGroup, setFormMuscleGroup] = useState('chest');
  const [formEquipment, setFormEquipment] = useState('body_weight');
  const [formInstructions, setFormInstructions] = useState('');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ExerciseDetail | null>(null);

  const openCreate = () => {
    setEditingId(null);
    setFormName('');
    setFormCategory('chest');
    setFormMuscleGroup('chest');
    setFormEquipment('body_weight');
    setFormInstructions('');
    setImageFile(null);
    setFormOpen(true);
  };

  const openEdit = (d: ExerciseDetail) => {
    setEditingId(d.id);
    setFormName(d.name);
    setFormCategory(d.category ?? d.bodyPart ?? 'chest');
    setFormMuscleGroup(d.muscleGroup ?? 'chest');
    setFormEquipment(d.equipment ?? 'body_weight');
    setFormInstructions(d.instructions ?? '');
    setImageFile(null);
    setFormOpen(true);
  };

  const submitForm = async () => {
    if (!formName.trim() || !formMuscleGroup || !formEquipment || !formCategory) {
      setError('Vui lòng nhập đủ tên, category, nhóm cơ và dụng cụ.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const image = imageFile ? (await planApi.uploadMedia(imageFile)).url : undefined;
      const body = {
        name: formName.trim(),
        muscleGroup: formMuscleGroup,
        equipment: formEquipment,
        category: formCategory,
        bodyPart: formCategory,
        instructions: formInstructions || undefined,
        image,
      };
      if (editingId != null) {
        await planApi.updateCustomExercise(editingId, body);
      } else {
        await planApi.createCustomExercise(body);
      }
      setFormOpen(false);
      setRefreshNonce((n) => n + 1);
    } catch {
      setError('Không thể lưu bài tập');
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await planApi.deleteCustomExercise(deleteTarget.id);
      setDeleteTarget(null);
      setRefreshNonce((n) => n + 1);
    } catch {
      setError('Xóa bài tập thất bại');
    }
  };

  return (
    <div className="page-container" style={{ maxWidth: 1040, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Icon name="book" size={24} />
          <h1>Thư viện bài tập</h1>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="badge badge-neutral">{totalElements} bài tập</span>
          <Button variant="dark" size="sm" onClick={openCreate}>+ Tạo bài tập</Button>
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      <ExerciseBrowser
        refreshNonce={refreshNonce}
        onTotalChange={setTotalElements}
        detailActions={(ex) =>
          ex.source === 'user_custom' ? (
            <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
              <Button variant="dark" size="sm" onClick={() => openEdit(ex)}>Sửa</Button>
              <Button variant="danger" size="sm" onClick={() => setDeleteTarget(ex)}>Xóa</Button>
            </div>
          ) : null
        }
      />

      <Modal
        open={formOpen}
        title={editingId != null ? 'Sửa bài tập của bạn' : 'Tạo bài tập cá nhân'}
        onClose={() => setFormOpen(false)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setFormOpen(false)}>Hủy</Button>
            <Button onClick={() => void submitForm()} loading={saving}>{editingId != null ? 'Lưu thay đổi' : 'Tạo bài tập'}</Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <TextField label="Tên bài tập *" value={formName} onChange={(e) => setFormName(e.target.value)} placeholder="Kéo cáp 1 tay" />
          <div>
            <div className="input-label" style={{ marginBottom: 8 }}>Category *</div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {EXERCISE_CATEGORIES.map((value) => (
                <button
                  key={value}
                  type="button"
                  className={`chip ${formCategory === value ? 'selected' : ''}`}
                  onClick={() => setFormCategory(value)}
                >
                  {label(EXERCISE_CATEGORY_LABELS, value)}
                </button>
              ))}
            </div>
          </div>
          <div>
            <div className="input-label" style={{ marginBottom: 8 }}>Dụng cụ *</div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {EXERCISE_EQUIPMENTS.map((value) => (
                <button
                  key={value}
                  type="button"
                  className={`chip ${formEquipment === value ? 'selected' : ''}`}
                  onClick={() => setFormEquipment(value)}
                >
                  {label(EXERCISE_EQUIPMENT_LABELS, value)}
                </button>
              ))}
            </div>
          </div>
          <div>
            <div className="input-label" style={{ marginBottom: 8 }}>Nhóm cơ (Rule Engine) *</div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {EXERCISE_MUSCLE_GROUPS.map((value) => (
                <button
                  key={value}
                  type="button"
                  className={`chip ${formMuscleGroup === value ? 'selected' : ''}`}
                  onClick={() => setFormMuscleGroup(value)}
                >
                  {label(EXERCISE_MUSCLE_GROUP_LABELS, value)}
                </button>
              ))}
            </div>
          </div>
          <div className="input-group">
            <label className="input-label">Hướng dẫn từng bước (tuỳ chọn)</label>
            <textarea
              value={formInstructions}
              onChange={(e) => setFormInstructions(e.target.value)}
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
          <div className="input-group">
            <label className="input-label">Ảnh / GIF (tuỳ chọn, ≤ 5MB)</label>
            <input
              type="file"
              accept="image/*,.gif"
              onChange={(e) => setImageFile(e.target.files?.[0] ?? null)}
              style={{ fontSize: 13, color: 'var(--text-secondary)' }}
            />
          </div>
        </div>
      </Modal>

      <Modal
        open={deleteTarget != null}
        title="Xóa bài tập"
        onClose={() => setDeleteTarget(null)}
        footer={
          <>
            <Button variant="outlined" onClick={() => setDeleteTarget(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => void confirmDelete()}>Xóa</Button>
          </>
        }
      >
        <p className="text-secondary text-sm" style={{ lineHeight: 1.7 }}>
          Xóa &quot;{deleteTarget?.name}&quot;? Bài tập sẽ bị ẩn khỏi tìm kiếm nhưng vẫn hiển thị trong lịch sử 1 tuần.
        </p>
      </Modal>
    </div>
  );
}
