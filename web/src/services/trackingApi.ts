import { http } from './http';

export interface WorkoutSession {
  id: number;
  status: string;
  startTime: string;
  endTime: string | null;
  focusInterruptionsCount: number;
}

export interface WorkoutSet {
  id: number;
  sessionId: number;
  exerciseId: number | null;
  setNumber: number;
  repsCompleted: number | null;
  weightUsed: number | null;
  restTimeSeconds: number | null;
}

export const trackingApi = {
  startSession: (body?: { planId?: number; startTime?: string }) =>
    http.post<WorkoutSession>('/workout-sessions', body ?? {}).then((r) => r.data),

  recordSet: (
    sessionId: number,
    body: { exerciseId?: number; setNumber: number; repsCompleted?: number; weightUsed?: number; restTimeSeconds?: number },
  ) => http.post<WorkoutSet>(`/workout-sessions/${sessionId}/sets`, body).then((r) => r.data),

  incrementFocus: (sessionId: number) =>
    http.post<WorkoutSession>(`/workout-sessions/${sessionId}/focus-interruption`).then((r) => r.data),

  completeSession: (sessionId: number) =>
    http.post<{ message: string }>(`/workout-sessions/${sessionId}/complete`).then((r) => r.data),
};
