import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import TextField from '../../components/TextField';
import { trackingApi } from '../../services/trackingApi';
import { mediaUrl as toMediaUrl } from '../../services/planApi';
import type { SessionExercise, WorkoutSession as WorkoutSessionModel, WorkoutSet } from '../../services/trackingApi';
import type { SetType } from '../../services/setTarget';
import DurationTimer from './execution/DurationTimer';
import ExerciseQueueChips from './execution/ExerciseQueueChips';
import MiniEditor from './execution/MiniEditor';
import RestOverlay from './execution/RestOverlay';
import WorkoutMusicPlayer from './music/WorkoutMusicPlayer';
import { parseMusicSource } from './music/musicSource';

interface SetLog {
  id?: number;
  sessionExerciseId?: number;
  setNumber: number;
  reps?: number;
  weight?: number;
  setType?: SetType;
  durationSeconds?: number;
}

interface EditState {
  reps: string;
  weight: string;
  duration: string;
}

const DEFAULT_REST_SECONDS = 60;
const UNDO_WINDOW_MS = 60_000;
const MUSIC_STORAGE_KEY = 'workoutMusicUrl';

function toSetLog(sets: WorkoutSet[]): SetLog[] {
  return sets.map((s) => ({
    id: s.id,
    sessionExerciseId: s.sessionExerciseId ?? undefined,
    setNumber: s.setNumber,
    reps: s.repsCompleted ?? undefined,
    weight: s.weightUsed != null ? Number(s.weightUsed) : undefined,
    setType: s.setType,
    durationSeconds: s.durationSeconds ?? undefined,
  }));
}

function parseDuration(value: string): number | undefined {
  if (!value) return undefined;
  const parts = value.split(':').map((p) => Number(p));
  if (parts.length === 1) return Number.isFinite(parts[0]) ? Math.max(0, Math.floor(parts[0])) : undefined;
  if (parts.length >= 2 && Number.isFinite(parts[0]) && Number.isFinite(parts[1])) {
    return Math.max(0, Math.floor(parts[0]) * 60 + Math.floor(parts[1]));
  }
  return undefined;
}

function fmtDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return m > 0 ? `${m}m ${s}s` : `${s}s`;
}

function toDurationInput(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

function playBeep() {
  try {
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new Ctx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.type = 'sine';
    osc.frequency.value = 880;
    gain.gain.value = 0.08;
    osc.start();
    osc.stop(ctx.currentTime + 0.18);
  } catch {
    /* Audio API không khả dụng */
  }
}

/**
 * Màn tập trung tối giản (016): tên bài + thời lượng → GIF → "Hiệp X - mục tiêu" → 1 nút HOÀN THÀNH
 * (bài rep) hoặc đồng hồ đếm ngược (bài duration). Sau mỗi hiệp là màn nghỉ lấy restTimeSeconds từ DB.
 */
export default function WorkoutSession({ hasSchedule }: { hasSchedule: boolean }) {
  const [session, setSession] = useState<WorkoutSessionModel | null>(null);
  const [currentEx, setCurrentEx] = useState<SessionExercise | null>(null);
  const [setLog, setSetLog] = useState<SetLog[]>([]);
  const [elapsed, setElapsed] = useState(0);
  const [notice, setNotice] = useState<ReactNode>('');
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [completing, setCompleting] = useState(false);

  // Nghỉ giữa hiệp — deadline tuyệt đối để không trôi giờ khi tab ẩn.
  const [restDeadline, setRestDeadline] = useState<number | null>(null);
  const [restTotal, setRestTotal] = useState(DEFAULT_REST_SECONDS);
  const [restRemaining, setRestRemaining] = useState<number | null>(null);

  // Đồng hồ bài duration — deadline tuyệt đối; tự ghi khi về 0.
  const [durationTotal, setDurationTotal] = useState(0);
  const [durationDeadline, setDurationDeadline] = useState<number | null>(null);
  const [durationRemaining, setDurationRemaining] = useState(0);
  const [durationRunning, setDurationRunning] = useState(false);
  const durationForExRef = useRef<number | null>(null);
  const durationTotalRef = useRef(0);
  const durationSavedRef = useRef(false);

  // Mini editor — mỗi bài giữ trạng thái nhập riêng (FR-008).
  const [editByExercise, setEditByExercise] = useState<Record<number, EditState>>({});
  const [editing, setEditing] = useState(false);

  // Hoàn tác hiệp vừa ghi — cửa sổ 60 giây (FR-007).
  const [undoInfo, setUndoInfo] = useState<{ id: number } | null>(null);
  const undoTimerRef = useRef<number | null>(null);

  // Fallback icon khi media lỗi/thiếu.
  const [mediaErrorId, setMediaErrorId] = useState<number | null>(null);

  // Nhạc luyện tập (017): URL do User cung cấp, lưu localStorage theo thiết bị.
  const [musicRaw, setMusicRaw] = useState<string>(() => {
    try {
      return localStorage.getItem(MUSIC_STORAGE_KEY) ?? '';
    } catch {
      return '';
    }
  });
  const musicSource = useMemo(() => parseMusicSource(musicRaw), [musicRaw]);

  useEffect(() => {
    trackingApi
      .getActiveSession()
      .then((s) => {
        setSession(s);
        setSetLog(toSetLog(s.sets ?? []));
        setCurrentEx(s.exercises[0] ?? null);
      })
      .catch((err: unknown) => {
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status !== 404) setError('Không kết nối được máy chủ. Hãy thử lại.');
      });
  }, []);

  // Đồng hồ thời lượng buổi tính từ startTime thật — không reset khi reload, không trôi khi tab ẩn.
  useEffect(() => {
    if (!session || session.status !== 'active') return;
    const start = new Date(session.startTime).getTime();
    const tick = () => setElapsed(Math.max(0, Math.floor((Date.now() - start) / 1000)));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [session]);

  useEffect(() => {
    if (restDeadline == null) return;
    const tick = () => {
      const remain = Math.max(0, Math.ceil((restDeadline - Date.now()) / 1000));
      setRestRemaining((prev) => (prev === remain ? prev : remain));
      if (remain <= 0) setRestDeadline(null);
    };
    tick();
    const id = setInterval(tick, 500);
    return () => clearInterval(id);
  }, [restDeadline]);

  useEffect(() => {
    if (restRemaining === 5 || restRemaining === 0) playBeep();
  }, [restRemaining]);

  useEffect(() => {
    if (durationDeadline == null) return;
    const total = durationTotalRef.current;
    const tick = () => {
      const remain = Math.max(0, Math.ceil((durationDeadline - Date.now()) / 1000));
      setDurationRemaining((prev) => (prev === remain ? prev : remain));
      if (remain <= 0 && !durationSavedRef.current) {
        durationSavedRef.current = true;
        setDurationDeadline(null);
        setDurationRunning(false);
        void saveDurationRef.current(total);
      }
    };
    tick();
    const id = setInterval(tick, 500);
    return () => clearInterval(id);
  }, [durationDeadline]);

  useEffect(() => {
    if (!session || session.status !== 'active') return;
    // FR-010 (016): tự động ghi nhận 1 lần phân tâm mỗi khi rời tab (không có nút thủ công).
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') {
        trackingApi.incrementFocus(session.id).then(setSession).catch(() => {});
      }
    };
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, [session]);

  useEffect(() => () => {
    if (undoTimerRef.current != null) window.clearTimeout(undoTimerRef.current);
  }, []);

  const fmt = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const startRest = (seconds: number) => {
    setRestTotal(seconds);
    setRestDeadline(Date.now() + seconds * 1000);
    setRestRemaining(seconds);
  };

  const stopRest = () => {
    setRestDeadline(null);
    setRestRemaining(null);
  };

  const resetDuration = () => {
    setDurationDeadline(null);
    setDurationRunning(false);
    setDurationTotal(0);
    setDurationRemaining(0);
    durationSavedRef.current = false;
  };

  const applySession = (s: WorkoutSessionModel) => {
    setSession(s);
    setSetLog(toSetLog(s.sets ?? []));
    const next = s.exercises.find((e) => e.id === currentEx?.id) ?? s.exercises[0] ?? null;
    setCurrentEx(next);
  };

  const onStart = async () => {
    if (starting) return;
    setStarting(true);
    setError('');
    try {
      try {
        localStorage.setItem(MUSIC_STORAGE_KEY, musicRaw.trim());
      } catch {
        /* localStorage không khả dụng — bỏ qua */
      }
      const s = await trackingApi.startSession();
      applySession(s);
      setElapsed(0);
      stopRest();
      resetDuration();
      if (musicRaw.trim() && !parseMusicSource(musicRaw)) {
        setError('URL nhạc không hợp lệ — đã bỏ qua nhạc');
      } else {
        setNotice(<>Buổi tập đã bắt đầu. Cố lên! <Icon name="strength" size={14} style={{ verticalAlign: '-2px' }} /></>);
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể bắt đầu buổi tập');
    } finally {
      setStarting(false);
    }
  };

  const selectExercise = (ex: SessionExercise) => {
    setCurrentEx(ex);
    setEditing(false);
    setMediaErrorId(null);
    resetDuration();
    const done = setLog.filter((l) => l.sessionExerciseId === ex.id).length;
    const target = ex.sets?.find((s) => s.setNumber === done + 1);
    setEditByExercise((prev) => {
      if (prev[ex.id]) return prev;
      return {
        ...prev,
        [ex.id]: {
          reps: String(target?.targetReps ?? ex.targetReps),
          weight: '',
          duration: toDurationInput(target?.targetDurationSeconds ?? ex.targetDurationSeconds ?? 0),
        },
      };
    });
  };

  const updateEditor = (patch: Partial<EditState>) => {
    if (!currentEx) return;
    setEditByExercise((prev) => ({
      ...prev,
      [currentEx.id]: { ...(prev[currentEx.id] ?? { reps: '', weight: '', duration: '' }), ...patch },
    }));
  };

  /** Ghi hiệp hiện tại (HOÀN THÀNH hoặc tự ghi bài duration). */
  const saveSet = async (overrides?: { durationSeconds?: number }) => {
    if (!session || !currentEx || saving) return;
    const setNum = setLog.filter((l) => l.sessionExerciseId === currentEx.id).length + 1;
    const target = currentEx.sets?.find((s) => s.setNumber === setNum);
    const isDur = currentEx.measureType === 'duration';
    const editor = editByExercise[currentEx.id];
    const last = [...setLog].reverse().find((l) => l.sessionExerciseId === currentEx.id);
    const repsValue = editor && editor.reps !== '' ? Number(editor.reps) : target?.targetReps ?? currentEx.targetReps;
    const weightValue = editor && editor.weight !== '' ? Number(editor.weight) : last?.weight;
    const setType: SetType = target?.setType ?? 'normal';
    const rest = currentEx.restTimeSeconds || DEFAULT_REST_SECONDS;
    setSaving(true);
    setError('');
    try {
      const saved = await trackingApi.recordSet(session.id, {
        exerciseId: currentEx.exerciseId,
        sessionExerciseId: currentEx.id,
        setNumber: setNum,
        repsCompleted: isDur ? undefined : repsValue,
        weightUsed: isDur ? undefined : weightValue,
        restTimeSeconds: rest,
        setType,
        durationSeconds: overrides?.durationSeconds,
      });
      setSetLog((prev) => [
        ...prev,
        {
          id: saved.id,
          sessionExerciseId: currentEx.id,
          setNumber: setNum,
          reps: isDur ? undefined : repsValue,
          weight: isDur ? undefined : weightValue,
          setType,
          durationSeconds: overrides?.durationSeconds,
        },
      ]);
      setNotice(<><Icon name="check" size={14} style={{ verticalAlign: '-2px', marginRight: 4 }} /> Đã lưu hiệp {setNum}{currentEx ? ` · ${currentEx.exerciseName}` : ''}.</>);
      setUndoInfo({ id: saved.id });
      if (undoTimerRef.current != null) window.clearTimeout(undoTimerRef.current);
      undoTimerRef.current = window.setTimeout(() => setUndoInfo(null), UNDO_WINDOW_MS);
      setEditing(false);
      if (setType !== 'drop_set') startRest(rest);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Lưu hiệp thất bại');
    } finally {
      setSaving(false);
    }
  };

  // Ref để tick đồng hồ duration gọi được saveSet mới nhất (tránh stale closure).
  const saveDurationRef = useRef<(seconds: number) => void>(() => {});
  useEffect(() => {
    saveDurationRef.current = (seconds: number) => {
      void saveSet({ durationSeconds: seconds });
    };
  });

  const startDuration = () => {
    if (!currentEx) return;
    const setNum = setLog.filter((l) => l.sessionExerciseId === currentEx.id).length + 1;
    const target = currentEx.sets?.find((s) => s.setNumber === setNum);
    const targetSeconds = target?.targetDurationSeconds ?? currentEx.targetDurationSeconds ?? 60;
    const editor = editByExercise[currentEx.id];
    const total = editor && editor.duration !== '' ? parseDuration(editor.duration) ?? targetSeconds : targetSeconds;
    durationForExRef.current = currentEx.id;
    durationTotalRef.current = total;
    durationSavedRef.current = false;
    setDurationTotal(total);
    setDurationRemaining(total);
    setDurationDeadline(Date.now() + total * 1000);
    setDurationRunning(true);
  };

  const pauseDuration = () => {
    if (durationDeadline == null) return;
    const remain = Math.max(0, Math.ceil((durationDeadline - Date.now()) / 1000));
    setDurationRemaining(remain);
    setDurationDeadline(null);
    setDurationRunning(false);
  };

  const saveDurationManual = () => {
    const seconds = Math.max(1, durationTotal - durationRemaining);
    void saveSet({ durationSeconds: seconds });
  };

  const onUndo = async () => {
    if (!session || !undoInfo) return;
    try {
      await trackingApi.deleteSet(session.id, undoInfo.id);
      setSetLog((prev) => prev.filter((l) => l.id !== undoInfo.id));
      setUndoInfo(null);
      if (undoTimerRef.current != null) {
        window.clearTimeout(undoTimerRef.current);
        undoTimerRef.current = null;
      }
      stopRest();
      setNotice('Đã hoàn tác hiệp vừa ghi.');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể hoàn tác hiệp');
    }
  };

  const onComplete = async () => {
    if (!session || saving || completing) return;
    if (!window.confirm('Kết thúc buổi tập này?')) return;
    setCompleting(true);
    try {
      await trackingApi.completeSession(session.id);
      setNotice('Buổi tập đã hoàn thành. Tuyệt vời!');
      setSession(null);
      setCurrentEx(null);
      setSetLog([]);
      setElapsed(0);
      stopRest();
      resetDuration();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Không thể kết thúc buổi tập');
    } finally {
      setCompleting(false);
    }
  };

  const isResting = restRemaining != null && restRemaining > 0;
  const currentSetNumber = currentEx ? setLog.filter((l) => l.sessionExerciseId === currentEx.id).length + 1 : 1;
  const currentTarget = currentEx?.sets?.find((s) => s.setNumber === currentSetNumber);
  const isDuration = currentEx?.measureType === 'duration';
  const editor = currentEx ? editByExercise[currentEx.id] : undefined;
  const repsShown = editor && editor.reps !== '' ? editor.reps : String(currentTarget?.targetReps ?? currentEx?.targetReps ?? '');
  const weightShown = editor?.weight ?? '';
  const durationShown = editor && editor.duration !== '' ? editor.duration : toDurationInput(currentTarget?.targetDurationSeconds ?? currentEx?.targetDurationSeconds ?? 0);
  const currentLogs = setLog.filter((l) => l.sessionExerciseId === currentEx?.id);
  const chips = (session?.exercises ?? []).map((ex) => ({
    id: ex.id,
    name: ex.exerciseName,
    done: setLog.filter((l) => l.sessionExerciseId === ex.id).length >= ex.targetSets,
    current: currentEx?.id === ex.id,
  }));
  const currentIdx = session?.exercises.findIndex((e) => e.id === currentEx?.id) ?? -1;
  const nextEx = currentIdx >= 0 ? session?.exercises[currentIdx + 1] : undefined;
  const mediaSrc = currentEx?.mediaUrl ? toMediaUrl(currentEx.mediaUrl) : null;
  const targetText = isDuration
    ? fmtDuration(currentTarget?.targetDurationSeconds ?? currentEx?.targetDurationSeconds ?? 0)
    : `${currentTarget?.targetReps ?? currentEx?.targetReps} reps${currentTarget?.targetWeight != null ? ` · ${currentTarget.targetWeight} kg` : ''}`;

  if (!session) {
    if (!hasSchedule) {
      return (
        <div className="card" style={{ textAlign: 'center', padding: '48px 32px' }}>
          <div style={{ marginBottom: 16 }}><Icon name="sun" size={64} /></div>
          <h2 style={{ marginBottom: 8 }}>Hôm nay là ngày nghỉ</h2>
          <p className="text-secondary" style={{ fontSize: 14 }}>
            Hôm nay không có bài tập trong lịch. Hãy nghỉ ngơi, hoặc vào tab Lịch tập để thêm bài.
          </p>
          {error && <div className="notice notice-error" style={{ marginTop: 16 }}>{error}</div>}
        </div>
      );
    }
    return (
      <div className="card" style={{ textAlign: 'center', padding: '48px 32px' }}>
        <div style={{ marginBottom: 16 }}><Icon name="strength" size={64} /></div>
        <h2 style={{ marginBottom: 8 }}>Sẵn sàng chinh phục?</h2>
        <p className="text-secondary" style={{ marginBottom: 28, fontSize: 14 }}>
          Bắt đầu sẽ copy bài của ngày hôm nay từ lộ trình (snapshot). Sửa lộ trình sau đó không đổi buổi này.
        </p>
        <div style={{ textAlign: 'left', maxWidth: 420, margin: '0 auto 20px' }}>
          <TextField
            label="Nhạc luyện tập (URL YouTube/Spotify/mp3 — tùy chọn)"
            value={musicRaw}
            onChange={(e) => setMusicRaw(e.target.value)}
            placeholder="https://…"
          />
        </div>
        <Button onClick={onStart} size="lg" loading={starting}>
          Bắt đầu buổi tập
        </Button>
        {error && <div className="notice notice-error" style={{ marginTop: 16 }}>{error}</div>}
      </div>
    );
  }

  return (
    <>
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 20px', gap: 12, flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: 160 }}>
          <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1 }}>Bài hiện tại</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{currentEx?.exerciseName ?? '—'}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1 }}>Thời gian</div>
          <div style={{ fontSize: 24, fontWeight: 700, fontFamily: 'monospace', color: 'var(--green)' }}>{fmt(elapsed)}</div>
          <div className="text-xs text-muted" style={{ marginTop: 2 }}>Phân tâm {session.focusInterruptionsCount}×</div>
        </div>
      </div>

      {musicSource && (
        <div className="card">
          <div className="text-xs text-muted" style={{ textTransform: 'uppercase', letterSpacing: 1, marginBottom: 10 }}>
            Nhạc luyện tập
          </div>
          <WorkoutMusicPlayer source={musicSource} onError={setError} />
        </div>
      )}

      <div className="card">
        <ExerciseQueueChips
          chips={chips}
          onSelect={(id) => {
            const ex = session?.exercises.find((e) => e.id === id);
            if (ex) selectExercise(ex);
          }}
        />
        {nextEx && (
          <div className="text-xs text-muted" style={{ marginTop: 10 }}>
            Tiếp theo: {nextEx.exerciseName}
          </div>
        )}
      </div>

      {isResting && restRemaining != null ? (
        <RestOverlay remaining={restRemaining} total={restTotal} onSkip={stopRest} />
      ) : (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 14 }}>
            {currentEx && mediaSrc && mediaErrorId !== currentEx.id ? (
              <img
                src={mediaSrc}
                alt={currentEx.exerciseName}
                style={{ width: 180, height: 180, objectFit: 'cover', borderRadius: 8, background: '#2a2a2a' }}
                onError={() => setMediaErrorId(currentEx.id)}
              />
            ) : (
              <div
                style={{
                  width: 180,
                  height: 180,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: '#2a2a2a',
                  borderRadius: 8,
                }}
              >
                <Icon name="strength" size={64} />
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={() => setEditing((v) => !v)}
            style={{
              width: '100%',
              textAlign: 'center',
              background: 'none',
              border: 'none',
              color: 'inherit',
              cursor: 'pointer',
              padding: '6px 0',
            }}
          >
            <span style={{ fontSize: '1.4rem', fontWeight: 700 }}>
              Hiệp {currentSetNumber}
              <span style={{ color: 'var(--green)' }}> - {targetText}</span>
            </span>
            <span className="text-xs text-muted" style={{ marginLeft: 8 }}>{editing ? '▲' : '✎'}</span>
          </button>

          {editing && (
            <MiniEditor
              measureType={currentEx?.measureType ?? 'reps_weight'}
              reps={repsShown}
              weight={weightShown}
              duration={durationShown}
              onRepsChange={(v) => updateEditor({ reps: v })}
              onWeightChange={(v) => updateEditor({ weight: v })}
              onDurationChange={(v) => updateEditor({ duration: v })}
              onClose={() => setEditing(false)}
            />
          )}

          <div style={{ marginTop: 18 }}>
            {isDuration ? (
              <DurationTimer
                total={durationTotal}
                remaining={durationRemaining}
                running={durationRunning}
                saving={saving}
                onStart={startDuration}
                onPause={pauseDuration}
                onSave={saveDurationManual}
              />
            ) : (
              <Button onClick={() => void saveSet()} fullWidth size="lg" loading={saving} style={{ minHeight: 52 }}>
                HOÀN THÀNH
              </Button>
            )}
          </div>
        </div>
      )}

      {(undoInfo || currentLogs.length > 0) && (
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          {undoInfo && (
            <button type="button" className="btn btn-sm btn-dark" onClick={() => void onUndo()}>
              Hoàn tác hiệp vừa ghi
            </button>
          )}
          {currentLogs.length > 0 && (
            <span className="text-xs text-muted">
              Đã ghi:{' '}
              {currentLogs
                .map((l) =>
                  l.durationSeconds != null
                    ? `#${l.setNumber} ${fmtDuration(l.durationSeconds)}`
                    : `#${l.setNumber} ${l.reps ?? '—'}×${l.weight ?? 0}kg`,
                )
                .join(' · ')}
            </span>
          )}
        </div>
      )}

      {notice && <div className="notice notice-success animate-slide-up">{notice}</div>}
      {error && <div className="notice notice-error animate-slide-up">{error}</div>}

      <div style={{ display: 'flex', gap: 10 }}>
        <Button
          variant="outlined"
          onClick={() => void onComplete()}
          disabled={completing || saving}
          style={{ flex: 1, color: 'var(--green)', borderColor: 'rgba(30,215,96,0.4)' }}
        >
          Kết thúc buổi tập
        </Button>
      </div>
    </>
  );
}
