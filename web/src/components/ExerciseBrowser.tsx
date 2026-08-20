import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import Button from './Button';
import Icon from './Icon';
import Spinner from './Spinner';
import TextField from './TextField';
import { mediaUrl, planApi } from '../services/planApi';
import type { ExerciseDetail } from '../services/planApi';
import {
  EXERCISE_CATEGORIES,
  EXERCISE_CATEGORY_LABELS,
  EXERCISE_EQUIPMENT_LABELS,
  EXERCISE_EQUIPMENTS,
  EXERCISE_MUSCLE_GROUP_LABELS,
  label,
} from '../services/labels';

const PAGE_SIZE = 20;

function toggleValue(current: string[], value: string): string[] {
  return current.includes(value) ? current.filter((item) => item !== value) : [...current, value];
}

function gifClass(source?: string | null): string | undefined {
  return source === 'user_custom' ? undefined : 'exercise-gif';
}

export default function ExerciseBrowser({
  onPick,
  pickLabel = 'Thêm vào ngày này',
  detailActions,
  listMaxHeight = 450,
  refreshNonce = 0,
  onTotalChange,
}: {
  onPick?: (exercise: ExerciseDetail) => void | Promise<void>;
  pickLabel?: string;
  detailActions?: (exercise: ExerciseDetail) => ReactNode;
  listMaxHeight?: number;
  refreshNonce?: number;
  onTotalChange?: (total: number) => void;
}) {
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [categories, setCategories] = useState<string[]>([]);
  const [equipments, setEquipments] = useState<string[]>([]);
  const [results, setResults] = useState<ExerciseDetail[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState<ExerciseDetail | null>(null);
  const [imgSrc, setImgSrc] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [picking, setPicking] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const loadingMoreRef = useRef(false);
  const requestIdRef = useRef(0);

  const load = (pageNumber: number, append = false) => {
    if (append) {
      if (loadingMoreRef.current) return;
      loadingMoreRef.current = true;
      setLoadingMore(true);
    } else {
      setLoading(true);
    }
    const requestId = append ? requestIdRef.current : ++requestIdRef.current;
    setError('');
    planApi
      .searchExercises({
        q: qDebounced || undefined,
        equipment: equipments.length ? equipments : undefined,
        category: categories.length ? categories : undefined,
        page: pageNumber,
        size: PAGE_SIZE,
      })
      .then((res) => {
        if (requestId !== requestIdRef.current) return;
        setResults((prev) => (append ? [...prev, ...res.content] : res.content));
        setTotalPages(res.totalPages);
        setPage(pageNumber);
        onTotalChange?.(res.totalElements);
      })
      .catch(() => {
        if (requestId !== requestIdRef.current) return;
        setError('Không thể tải danh sách bài tập');
      })
      .finally(() => {
        loadingMoreRef.current = false;
        if (requestId === requestIdRef.current) {
          setLoading(false);
          setLoadingMore(false);
        }
      });
  };

  const handleScroll = () => {
    const el = listRef.current;
    if (!el || loading || loadingMore || page >= totalPages - 1) return;
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 80) {
      load(page + 1, true);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => setQDebounced(q), 300);
    return () => window.clearTimeout(timer);
  }, [q]);

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [qDebounced, categories, equipments, refreshNonce]);

  useEffect(() => {
    if (!loading) {
      listRef.current?.scrollTo?.({ top: 0 });
    }
  }, [loading]);

  const onOpen = async (ex: ExerciseDetail) => {
    setDetail(ex);
    setImgSrc(mediaUrl(ex.gifUrl) ?? mediaUrl(ex.image));
    try {
      const d = await planApi.getExercise(ex.id);
      setDetail(d);
      setImgSrc(mediaUrl(d.gifUrl) ?? mediaUrl(d.image));
    } catch {
      /* giữ dữ liệu từ danh sách */
    }
  };

  const onImgError = () => {
    if (!detail) return;
    const fallback = mediaUrl(detail.image);
    if (fallback && imgSrc !== fallback) setImgSrc(fallback);
  };

  const confirmPick = async () => {
    if (!detail || !onPick || picking) return;
    setPicking(true);
    setError('');
    try {
      await onPick(detail);
    } catch (err) {
      const message = err instanceof Error && err.message ? err.message : 'Không thể thêm bài tập';
      setError(message);
    } finally {
      setPicking(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div className="card" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div>
          <div className="input-label" style={{ marginBottom: 8 }}>Category</div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {EXERCISE_CATEGORIES.map((value) => (
              <button
                key={value}
                type="button"
                className={`chip ${categories.includes(value) ? 'selected' : ''}`}
                onClick={() => setCategories((prev) => toggleValue(prev, value))}
              >
                {label(EXERCISE_CATEGORY_LABELS, value)}
              </button>
            ))}
          </div>
        </div>
        <div>
          <div className="input-label" style={{ marginBottom: 8 }}>Dụng cụ</div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {EXERCISE_EQUIPMENTS.map((value) => (
              <button
                key={value}
                type="button"
                className={`chip ${equipments.includes(value) ? 'selected' : ''}`}
                onClick={() => setEquipments((prev) => toggleValue(prev, value))}
              >
                {label(EXERCISE_EQUIPMENT_LABELS, value)}
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      <div className="grid-two" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, alignItems: 'start' }}>
        <div>
          <TextField
            label="Tìm bài tập"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Squat, pull up, bench press..."
          />
          <div style={{ height: 14 }} />
          {loading ? (
            <Spinner />
          ) : results.length === 0 ? (
            <div className="empty-state" style={{ paddingTop: 24 }}>
              <div className="empty-state-icon"><Icon name="search" size={36} /></div>
              <p className="empty-state-text">Không tìm thấy bài tập phù hợp. Hãy thử bộ lọc khác.</p>
            </div>
          ) : (
            <div
              ref={listRef}
              onScroll={handleScroll}
              style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: listMaxHeight, overflowY: 'auto', overscrollBehavior: 'contain', paddingRight: 6 }}
            >
              {results.map((ex) => (
                <button
                  key={ex.id}
                  type="button"
                  onClick={() => void onOpen(ex)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    width: '100%',
                    textAlign: 'left',
                    padding: '12px 14px',
                    background: detail?.id === ex.id ? 'var(--mid-dark)' : 'var(--dark-surface)',
                    border: `1px solid ${detail?.id === ex.id ? 'var(--green)' : 'transparent'}`,
                    borderRadius: 8,
                    color: 'var(--text-base)',
                    cursor: 'pointer',
                    transition: 'background var(--t-fast), border-color var(--t-fast)',
                  }}
                >
                  {mediaUrl(ex.image) ? (
                    <img
                      src={mediaUrl(ex.image) ?? undefined}
                      alt=""
                      className={gifClass(ex.source)}
                      style={{ width: 44, height: 44, borderRadius: 6, objectFit: 'cover', flexShrink: 0 }}
                    />
                  ) : (
                    <div style={{ width: 44, height: 44, borderRadius: 6, background: 'var(--mid-dark)', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-secondary)' }}>
                      <Icon name="strength" />
                    </div>
                  )}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="fw-600" style={{ fontSize: 14 }}>{ex.name}</div>
                    <div className="text-secondary" style={{ fontSize: 12 }}>
                      {label(EXERCISE_CATEGORY_LABELS, ex.category)} · {label(EXERCISE_EQUIPMENT_LABELS, ex.equipment)}
                    </div>
                  </div>
                  {ex.source === 'user_custom' && <span className="badge badge-green">Của bạn</span>}
                </button>
              ))}
              {loadingMore && (
                <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0' }}>
                  <Spinner />
                </div>
              )}
            </div>
          )}
        </div>

        <div className="card" style={{ position: 'sticky', top: 0 }}>
          {detail ? (
            <div className="animate-fade">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10 }}>
                <h2 style={{ marginBottom: 6, fontSize: 18 }}>{detail.name}</h2>
                {detailActions?.(detail)}
              </div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
                {detail.equipment && (
                  <span className="badge badge-neutral">{label(EXERCISE_EQUIPMENT_LABELS, detail.equipment)}</span>
                )}
                {detail.category && (
                  <span className="badge badge-info">{label(EXERCISE_CATEGORY_LABELS, detail.category)}</span>
                )}
                {detail.muscleGroup && (
                  <span className="badge badge-neutral">{label(EXERCISE_MUSCLE_GROUP_LABELS, detail.muscleGroup)}</span>
                )}
                {detail.source === 'user_custom' && <span className="badge badge-green">Của bạn</span>}
              </div>
              {imgSrc ? (
                <img
                  src={imgSrc}
                  alt={detail.name}
                  onError={onImgError}
                  className={gifClass(detail.source)}
                  style={{ width: 180, height: 180, objectFit: 'contain', borderRadius: 10, display: 'block', marginBottom: 14 }}
                />
              ) : (
                <div style={{ width: 180, height: 180, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--mid-dark)', borderRadius: 10, color: 'var(--text-secondary)', marginBottom: 14 }}>
                  <Icon name="strength" size={48} />
                </div>
              )}
              {detail.instructions && (
                <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                  {detail.instructions}
                </p>
              )}
              {onPick && (
                <Button onClick={() => void confirmPick()} fullWidth loading={picking} style={{ marginTop: 14 }}>
                  {pickLabel}
                </Button>
              )}
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '32px 16px' }}>
              <div className="empty-state-icon"><Icon name="strength" size={36} /></div>
              <p className="empty-state-text">Chọn một bài tập để xem hướng dẫn chi tiết và ảnh/GIF.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
