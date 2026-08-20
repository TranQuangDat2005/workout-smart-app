import { useCallback, useEffect, useRef, useState } from 'react';
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import Button from '../../components/Button';
import ExerciseBrowser from '../../components/ExerciseBrowser';
import Icon from '../../components/Icon';
import Modal from '../../components/Modal';
import Spinner from '../../components/Spinner';
import { mediaUrl, planApi } from '../../services/planApi';
import type { ExerciseDetail, PlanDay, PlanExercise, WorkoutPlan } from '../../services/planApi';
import { trackingApi } from '../../services/trackingApi';
import type { SetTarget } from '../../services/setTarget';
import { EXERCISE_EQUIPMENT_LABELS, EXERCISE_MUSCLE_GROUP_LABELS, label } from '../../services/labels';

const DAY_LABELS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
const WEEK_DAYS = [1, 2, 3, 4, 5, 6, 0]; // Thứ 2 → Chủ nhật
let nextTempId = -1; // ID tạm (âm) cho bài mới thêm trước khi server trả ID thật

const smallField = { display: 'flex', alignItems: 'center', gap: 6 } as const;
const smallLabel = { fontSize: 12, color: 'var(--text-secondary)', whiteSpace: 'nowrap' as const };
const smallInput = {
  width: 64,
  background: 'var(--near-black)',
  color: 'var(--text-base)',
  border: '1px solid var(--border-dark)',
  borderRadius: 8,
  padding: '8px 10px',
} as const;

function ExerciseCard({
  item,
  index,
  onChange,
  onRemove,
  onToggleDetail,
  onTogglePerSet,
  onMeasureChange,
  perSetActive,
  expanded,
  perSetValues,
  onPerSetValueChange,
  onApplyPerSet,
}: {
  item: PlanExercise;
  index: number;
  onChange: (id: number, patch: Partial<PlanExercise>) => void;
  onRemove: (id: number) => void;
  onToggleDetail: (exerciseId: number) => void;
  onTogglePerSet: (id: number) => void;
  onMeasureChange: (id: number, value: string) => void;
  perSetActive: boolean;
  expanded: boolean;
  perSetValues: number[];
  onPerSetValueChange: (index: number, value: number) => void;
  onApplyPerSet: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({ id: item.id });
  const isDuration = item.measureType === 'duration';
  const hasSets = item.sets != null && item.sets.length > 0;
  return (
    <div
      ref={setNodeRef}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
        background: 'var(--mid-dark)',
        borderRadius: 12,
        padding: '14px 16px',
        border: expanded ? '1px solid rgba(30,215,96,0.35)' : '1px solid var(--border-dark)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <button
          type="button"
          className="btn btn-sm btn-dark"
          {...attributes}
          {...listeners}
          aria-label="Kéo đổi thứ tự"
          style={{ cursor: 'grab', flexShrink: 0 }}
        >
          <Icon name="moveVertical" size={16} /> {index + 1}
        </button>
        <button
          type="button"
          onClick={() => onToggleDetail(item.exerciseId)}
          title="Xem chi tiết bài tập"
          style={{
            flex: 1,
            textAlign: 'left',
            background: 'transparent',
            border: 'none',
            padding: '6px 4px',
            color: expanded ? 'var(--green)' : 'var(--text-base)',
            fontWeight: 600,
            fontSize: 15,
            cursor: 'pointer',
          }}
        >
          {item.exerciseName}
          {expanded && <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--text-secondary)' }}>· thu gọn</span>}
        </button>
        <Button variant="danger" size="sm" onClick={() => onRemove(item.id)}>Xóa</Button>
      </div>

      <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap', alignItems: 'center', marginTop: 12 }}>
        <div style={smallField}>
          <span style={smallLabel}>Sets</span>
          <input
            type="number"
            min={1}
            value={item.targetSets}
            onChange={(e) => onChange(item.id, { targetSets: Number(e.target.value) || 1 })}
            style={smallInput}
          />
        </div>
        <div style={smallField}>
          <span style={smallLabel}>Mục tiêu</span>
          <select
            value={isDuration ? 'duration' : 'reps_weight'}
            onChange={(e) => onMeasureChange(item.id, e.target.value)}
            title="Đơn vị theo dõi"
            style={{ ...smallInput, width: 78 }}
          >
            <option value="reps_weight">reps</option>
            <option value="duration">giây</option>
          </select>
          {isDuration ? (
            <input
              type="number"
              min={0}
              value={item.targetDurationSeconds ?? 0}
              onChange={(e) => onChange(item.id, { targetDurationSeconds: Number(e.target.value) || 0 })}
              title="Thời gian (giây)"
              style={smallInput}
            />
          ) : (
            <input
              type="number"
              min={1}
              value={item.targetReps}
              onChange={(e) => onChange(item.id, { targetReps: Number(e.target.value) || 1 })}
              style={smallInput}
            />
          )}
        </div>
        <div style={smallField}>
          <span style={smallLabel}>Nghỉ (s)</span>
          <input
            type="number"
            min={0}
            value={item.restTimeSeconds}
            onChange={(e) => onChange(item.id, { restTimeSeconds: Number(e.target.value) || 0 })}
            style={{ ...smallInput, width: 72 }}
          />
        </div>
        <Button variant={perSetActive ? 'primary' : 'dark'} size="sm" onClick={() => onTogglePerSet(item.id)}>
          {isDuration ? 'Thời gian từng hiệp' : 'Reps từng hiệp'}
        </Button>
      </div>

      {hasSets && !perSetActive && (
        <div style={{ marginTop: 10, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {item.sets!.map((s, i) => (
            <span key={i} className="badge badge-neutral" style={{ fontSize: 11 }}>
              #{i + 1}: {isDuration ? `${s.targetDurationSeconds ?? 0}s` : s.targetReps}
            </span>
          ))}
        </div>
      )}

      {perSetActive && (
        <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
          <div className="fw-600" style={{ fontSize: 13, marginBottom: 10 }}>
            {isDuration ? 'Thời gian từng hiệp' : 'Reps từng hiệp'} · {item.exerciseName}
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(96px, 1fr))', gap: 8 }}>
            {perSetValues.map((v, i) => (
              <div key={i} style={smallField}>
                <span style={smallLabel}>#{i + 1}</span>
                <input
                  type="number"
                  min={0}
                  value={v}
                  onChange={(e) => onPerSetValueChange(i, Number(e.target.value) || 0)}
                  style={smallInput}
                />
              </div>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <Button size="sm" onClick={onApplyPerSet}>Lưu</Button>
            <Button size="sm" variant="dark" onClick={() => onTogglePerSet(item.id)}>Hủy</Button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function PlanEditor({
  plan,
  onPlanChange,
  visible = true,
}: {
  plan: WorkoutPlan;
  onPlanChange: (plan: WorkoutPlan) => void;
  visible?: boolean;
}) {
  const [error, setError] = useState('');
  const [activeDay, setActiveDay] = useState<number>(new Date().getDay());
  const [saving, setSaving] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ExerciseDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [perSetFor, setPerSetFor] = useState<number | null>(null);
  const [perSetValues, setPerSetValues] = useState<number[]>([]);
  const [hasActiveSession, setHasActiveSession] = useState(false);
  const persistSeq = useRef(0);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  useEffect(() => {
    if (!visible) return;
    trackingApi.getActiveSession()
      .then(() => setHasActiveSession(true))
      .catch(() => setHasActiveSession(false));
  }, [visible]);

  const selectedDay: PlanDay | undefined = plan.days.find((d) => d.dayOfWeek === activeDay);

  const persist = useCallback(async (dayId: number, exercises: PlanExercise[]) => {
    // Guard thứ tự: chỉ apply phản hồi của request mới nhất — kéo liên tiếp không bị giật ngược.
    const seq = ++persistSeq.current;
    setSaving(true);
    setError('');
    try {
      const updated = await planApi.replaceDayExercises(
        dayId,
        exercises.map((ex) => ({
          exerciseId: ex.exerciseId,
          targetSets: ex.targetSets,
          targetReps: ex.targetReps,
          restTimeSeconds: ex.restTimeSeconds,
          targetDurationSeconds: ex.targetDurationSeconds,
          measureType: ex.measureType,
          sets: ex.sets,
        })),
      );
      if (seq === persistSeq.current) onPlanChange(updated);
    } catch (err) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không lưu được thay đổi ngày tập');
    } finally {
      if (seq === persistSeq.current) setSaving(false);
    }
  }, [onPlanChange]);

  const updateDayExercises = (next: PlanExercise[]) => {
    if (!selectedDay) return;
    void persist(selectedDay.id, next);
  };

  const onDragEnd = (event: DragEndEvent) => {
    if (!selectedDay) return;
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const ids = selectedDay.exercises.map((e) => e.id);
    const oldIndex = ids.indexOf(Number(active.id));
    const newIndex = ids.indexOf(Number(over.id));
    updateDayExercises(arrayMove(selectedDay.exercises, oldIndex, newIndex));
  };

  const onPatch = (id: number, patch: Partial<PlanExercise>) => {
    if (!selectedDay) return;
    updateDayExercises(selectedDay.exercises.map((e) => (e.id === id ? { ...e, ...patch } : e)));
  };

  const onRemove = (id: number) => {
    if (!selectedDay) return;
    updateDayExercises(selectedDay.exercises.filter((e) => e.id !== id));
  };

  const openPerSet = (id: number) => {
    if (!selectedDay) return;
    if (perSetFor === id) {
      setPerSetFor(null);
      return;
    }
    const ex = selectedDay.exercises.find((e) => e.id === id);
    const isDur = ex?.measureType === 'duration';
    const existing = ex?.sets?.map((s) => (isDur ? s.targetDurationSeconds ?? 0 : s.targetReps));
    const count = Math.max(1, ex?.targetSets ?? 1, existing?.length ?? 0);
    const fallback = isDur ? (ex?.targetDurationSeconds ?? 60) : (ex?.targetReps ?? 10);
    setPerSetFor(id);
    setPerSetValues(Array.from({ length: count }, (_, i) => existing?.[i] ?? fallback));
  };

  const updatePerSetValue = (index: number, value: number) => {
    setPerSetValues((prev) => prev.map((v, i) => (i === index ? value : v)));
  };

  const applyPerSet = () => {
    if (!selectedDay || perSetFor == null) return;
    const ex = selectedDay.exercises.find((e) => e.id === perSetFor);
    const isDur = ex?.measureType === 'duration';
    const sets: SetTarget[] = perSetValues.map((v, i) => ({
      setNumber: i + 1,
      targetReps: isDur ? 0 : v,
      targetWeight: null,
      setType: 'normal',
      targetDurationSeconds: isDur ? v : undefined,
    }));
    updateDayExercises(
      selectedDay.exercises.map((e) => (e.id === perSetFor ? { ...e, sets } : e)),
    );
    setPerSetFor(null);
  };

  const onMeasureChange = (id: number, value: string) => {
    if (!selectedDay) return;
    updateDayExercises(selectedDay.exercises.map((e) => {
      if (e.id !== id) return e;
      if (value === 'duration') {
        return { ...e, measureType: 'duration', targetReps: 0, targetDurationSeconds: e.targetDurationSeconds ?? 60 };
      }
      return { ...e, measureType: 'reps_weight', targetDurationSeconds: undefined, targetReps: e.targetReps || 10 };
    }));
  };

  const addExercise = async (ex: ExerciseDetail) => {
    const existing = selectedDay?.exercises ?? [];
    if (existing.length >= 15) {
      throw new Error('Tối đa 15 bài mỗi ngày');
    }
    const newExercise: PlanExercise = {
      id: nextTempId--,
      exerciseId: ex.id,
      exerciseName: ex.name,
      targetSets: 3,
      targetReps: ex.measureType === 'duration' ? 0 : 10,
      restTimeSeconds: 60,
      image: ex.image,
      gifUrl: ex.gifUrl,
      measureType: ex.measureType,
      targetDurationSeconds: ex.measureType === 'duration' ? 60 : undefined,
    };
    setSaving(true);
    try {
      let dayId = selectedDay?.id;
      if (dayId == null) {
        const day = await planApi.createDay(activeDay);
        dayId = day.id;
      }
      const updated = await planApi.replaceDayExercises(
        dayId,
        [...existing, newExercise].map((e) => ({
          exerciseId: e.exerciseId,
          targetSets: e.targetSets,
          targetReps: e.targetReps,
          restTimeSeconds: e.restTimeSeconds,
          targetDurationSeconds: e.targetDurationSeconds,
          measureType: e.measureType,
        })),
      );
      onPlanChange(updated);
      setPickerOpen(false);
    } catch (err) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      throw new Error(msg ?? 'Không thể thêm bài tập');
    } finally {
      setSaving(false);
    }
  };

  const toggleDetail = async (exerciseId: number) => {
    if (detailId === exerciseId) {
      setDetailId(null);
      setDetail(null);
      return;
    }
    setDetailId(exerciseId);
    setDetail(null);
    setDetailLoading(true);
    try {
      setDetail(await planApi.getExercise(exerciseId));
    } catch {
      setError('Không thể tải chi tiết bài tập');
    } finally {
      setDetailLoading(false);
    }
  };

  const today = new Date().getDay();
  const detailImg = detail ? (mediaUrl(detail.gifUrl) ?? mediaUrl(detail.image)) : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <p className="text-secondary text-sm">
        Sửa ngày này là sửa mẫu tuần (mọi tuần sau). Vào tab “Hôm nay” để bắt đầu và ghi nhận buổi tập.
      </p>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {WEEK_DAYS.map((dayOfWeek) => (
          <button
            key={dayOfWeek}
            type="button"
            onClick={() => { setActiveDay(dayOfWeek); setDetailId(null); setDetail(null); setPerSetFor(null); }}
            className={`btn btn-sm ${activeDay === dayOfWeek ? 'btn-primary' : 'btn-dark'}`}
            style={{ position: 'relative' }}
          >
            {DAY_LABELS[dayOfWeek]}
            {dayOfWeek === today && (
              <span
                style={{
                  position: 'absolute', top: -4, right: -4,
                  width: 8, height: 8, borderRadius: '50%',
                  background: 'var(--green)', border: '2px solid var(--near-black)',
                }}
              />
            )}
          </button>
        ))}
      </div>

      {error && <div className="notice notice-error">{error}</div>}
      {/* Vùng có chiều cao cố định — trạng thái lưu hiện/ẩn KHÔNG đẩy layout (không giật màn hình). */}
      <div style={{ minHeight: 18, display: 'flex', alignItems: 'center' }}>
        {saving && <span className="text-muted" style={{ fontSize: 12 }}>Đang lưu thay đổi…</span>}
      </div>

      {hasActiveSession && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', background: 'rgba(255,193,7,0.12)', border: '1px solid rgba(255,193,7,0.35)', borderRadius: 10 }}>
          <Icon name="alert" size={18} />
          <span style={{ fontSize: 13 }}>Bạn đang trong buổi tập — hoàn tất buổi tập ở tab “Hôm nay” rồi mới sửa được lịch.</span>
        </div>
      )}

      <div className="card animate-fade" style={{ opacity: hasActiveSession ? 0.6 : 1, pointerEvents: hasActiveSession ? 'none' : 'auto' }}>
        <div className="section-header" style={{ marginBottom: 16 }}>
          <div>
            {activeDay === today && <span className="badge badge-green">Hôm nay</span>}
          </div>
          <Button variant="dark" size="sm" onClick={() => setPickerOpen(true)} disabled={hasActiveSession}>
            + Thêm bài
          </Button>
        </div>

        {selectedDay && selectedDay.exercises.length > 0 && (
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
            <SortableContext items={selectedDay.exercises.map((e) => e.id)} strategy={verticalListSortingStrategy}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {selectedDay.exercises.map((ex, idx) => (
                  <ExerciseCard
                    key={ex.id}
                    item={ex}
                    index={idx}
                    onChange={onPatch}
                    onRemove={onRemove}
                    onToggleDetail={toggleDetail}
                    onTogglePerSet={openPerSet}
                    onMeasureChange={onMeasureChange}
                    perSetActive={perSetFor === ex.id}
                    expanded={detailId === ex.exerciseId}
                    perSetValues={perSetValues}
                    onPerSetValueChange={updatePerSetValue}
                    onApplyPerSet={applyPerSet}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        )}

        {selectedDay && selectedDay.exercises.length === 0 && (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-state-icon"><Icon name="strength" size={42} /></div>
            <p className="empty-state-text">Chưa có bài tập nào. Bấm “+ Thêm bài” để bắt đầu.</p>
          </div>
        )}

        {detailId != null && (
          <div style={{ marginTop: 16, borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: 16 }}>
            {detailLoading ? (
              <Spinner />
            ) : detail ? (
              <div className="animate-fade" style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                {detailImg ? (
                  <img
                    src={detailImg}
                    alt={detail.name}
                    className={detail.source === 'user_custom' ? undefined : 'exercise-gif'}
                    style={{ width: 180, height: 180, objectFit: 'contain', display: 'block', flexShrink: 0, borderRadius: 10 }}
                  />
                ) : (
                  <div style={{ width: 180, height: 180, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--mid-dark)', borderRadius: 10, color: 'var(--text-secondary)', flexShrink: 0 }}>
                    <Icon name="strength" size={48} />
                  </div>
                )}
                <div style={{ flex: 1, minWidth: 200 }}>
                  <div className="fw-600" style={{ marginBottom: 8 }}>{detail.name}</div>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
                    {detail.muscleGroup && <span className="badge badge-neutral">{label(EXERCISE_MUSCLE_GROUP_LABELS, detail.muscleGroup)}</span>}
                    {detail.equipment && <span className="badge badge-info">{label(EXERCISE_EQUIPMENT_LABELS, detail.equipment)}</span>}
                  </div>
                  {detail.instructions && (
                    <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-wrap', margin: 0 }}>{detail.instructions}</p>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        )}
      </div>

      <Modal open={pickerOpen} title="Thêm bài tập" size="lg" onClose={() => setPickerOpen(false)}>
        <ExerciseBrowser
          onPick={addExercise}
          pickLabel="Thêm vào ngày này"
          listMaxHeight={280}
        />
      </Modal>
    </div>
  );
}
