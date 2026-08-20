import { http } from './http';
import type { SetTarget, SetType } from './setTarget';

export interface WorkoutSession {
  id: number;
  status: string;
  startTime: string;
  endTime: string | null;
  focusInterruptionsCount: number;
  planId: number | null;
  exercises: SessionExercise[];
  sets: WorkoutSet[];
}

export interface SessionExercise {
  id: number;
  exerciseId: number;
  exerciseName: string;
  sortOrder: number;
  targetSets: number;
  targetReps: number;
  restTimeSeconds: number;
  sets?: SetTarget[];
  targetDurationSeconds?: number | null;
  measureType?: string;
  mediaUrl?: string | null;
}

export interface WorkoutSet {
  id: number;
  sessionId: number;
  exerciseId: number | null;
  setNumber: number;
  repsCompleted: number | null;
  weightUsed: number | null;
  restTimeSeconds: number | null;
  sessionExerciseId: number | null;
  setType: SetType;
  durationSeconds?: number | null;
}

export const trackingApi = {
  getActiveSession: () => http.get<WorkoutSession>('/workout-sessions/active').then((r) => r.data),

  startSession: (body?: { planId?: number; startTime?: string }) =>
    http.post<WorkoutSession>('/workout-sessions', body ?? {}).then((r) => r.data),

  recordSet: (
    sessionId: number,
    body: {
      exerciseId?: number;
      setNumber: number;
      repsCompleted?: number;
      weightUsed?: number;
      restTimeSeconds?: number;
      sessionExerciseId?: number;
      setType?: SetType;
      durationSeconds?: number;
    },
  ) => http.post<WorkoutSet>(`/workout-sessions/${sessionId}/sets`, body).then((r) => r.data),

  incrementFocus: (sessionId: number) =>
    http.post<WorkoutSession>(`/workout-sessions/${sessionId}/focus-interruption`).then((r) => r.data),

  deleteSet: (sessionId: number, setId: number) =>
    http.delete<void>(`/workout-sessions/${sessionId}/sets/${setId}`).then((r) => r.data),

  completeSession: (sessionId: number) =>
    http.post<{ message: string }>(`/workout-sessions/${sessionId}/complete`).then((r) => r.data),
};
