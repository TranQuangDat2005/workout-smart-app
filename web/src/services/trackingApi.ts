import axios from 'axios';
import { tokenStorage } from './tokenStorage';

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

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
    client.post<WorkoutSession>('/workout-sessions', body ?? {}).then((r) => r.data),

  recordSet: (
    sessionId: number,
    body: { exerciseId?: number; setNumber: number; repsCompleted?: number; weightUsed?: number; restTimeSeconds?: number },
  ) => client.post<WorkoutSet>(`/workout-sessions/${sessionId}/sets`, body).then((r) => r.data),

  incrementFocus: (sessionId: number) =>
    client.post<WorkoutSession>(`/workout-sessions/${sessionId}/focus-interruption`).then((r) => r.data),

  completeSession: (sessionId: number) =>
    client.post<{ message: string }>(`/workout-sessions/${sessionId}/complete`).then((r) => r.data),
};
