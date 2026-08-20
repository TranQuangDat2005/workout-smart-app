import TextField from '../../../components/TextField';

interface MiniEditorProps {
  measureType: string;
  reps: string;
  weight: string;
  duration: string;
  onRepsChange: (value: string) => void;
  onWeightChange: (value: string) => void;
  onDurationChange: (value: string) => void;
  onClose: () => void;
}

/**
 * Mini editor (016 FR-006): mở khi chạm dòng "Hiệp X - mục tiêu".
 * Giá trị sửa ở đây được dùng cho hiệp sắp ghi — không chiếm chỗ màn hình khi không cần.
 */
export default function MiniEditor({
  measureType,
  reps,
  weight,
  duration,
  onRepsChange,
  onWeightChange,
  onDurationChange,
  onClose,
}: MiniEditorProps) {
  const isDuration = measureType === 'duration';
  return (
    <div
      className="animate-fade"
      style={{
        marginTop: 10,
        padding: '12px 14px',
        background: 'rgba(255,255,255,0.04)',
        borderRadius: 10,
        border: '1px solid rgba(255,255,255,0.08)',
      }}
    >
      <div className="text-xs text-muted" style={{ marginBottom: 8 }}>
        Sửa giá trị cho hiệp sắp ghi — bỏ trống để dùng mục tiêu
      </div>
      {isDuration ? (
        <TextField label="Thời gian (mm:ss)" value={duration} onChange={(e) => onDurationChange(e.target.value)} placeholder="1:00" />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <TextField label="Số lần (reps)" type="number" value={reps} onChange={(e) => onRepsChange(e.target.value)} placeholder="12" min="0" step="1" />
          <TextField label="Tạ (kg)" type="number" value={weight} onChange={(e) => onWeightChange(e.target.value)} placeholder="0" min="0" step="0.5" />
        </div>
      )}
      <button type="button" className="btn btn-sm btn-dark" onClick={onClose} style={{ marginTop: 10 }}>
        Đóng
      </button>
    </div>
  );
}
