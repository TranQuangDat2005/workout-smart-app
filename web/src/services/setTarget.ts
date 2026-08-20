/** Kiểu hiệp tập nâng cao (014) — đồng bộ enum backend SetType. */
export type SetType = 'normal' | 'warm_up' | 'drop_set';

export interface SetTarget {
  setNumber: number;
  targetReps: number;
  targetWeight: number | null;
  setType: SetType;
  targetDurationSeconds?: number | null;
}
