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

export interface AdminUser {
  id: number;
  email: string;
  displayName: string | null;
  accountStatus: string;
  emailVerified: boolean;
}

export interface ExerciseInput {
  name: string;
  category?: string;
  bodyPart?: string;
  equipment: string;
  target?: string;
  muscleGroup: string;
  image?: string;
  gifUrl?: string;
  instructions?: string;
}

export interface AdminExercise {
  id: number;
  name: string;
  equipment: string | null;
  muscleGroup: string | null;
  status: string;
}

export const adminApi = {
  searchUsers: (q: string) => client.get<AdminUser[]>(`/admin/users?q=${encodeURIComponent(q)}`).then((r) => r.data),

  banUser: (id: number, reason: string) => client.post(`/admin/users/${id}/ban`, { reason }).then((r) => r.data),

  unbanUser: (id: number) => client.post(`/admin/users/${id}/unban`).then((r) => r.data),

  createExercise: (body: ExerciseInput) => client.post('/admin/exercises', body).then((r) => r.data),

  updateExercise: (id: number, body: Partial<ExerciseInput>) =>
    client.put(`/admin/exercises/${id}`, body).then((r) => r.data),

  importExercises: (items: ExerciseInput[]) =>
    client
      .post<{ inserted: number; updated: number; skipped: number }>('/admin/exercises/import', { items })
      .then((r) => r.data),

  setExerciseStatus: (id: number, status: 'active' | 'inactive', reason?: string) =>
    client.patch(`/admin/exercises/${id}/status`, { status, reason }).then((r) => r.data),

  listExercises: (q: string) =>
    client.get<AdminExercise[]>(`/admin/exercises?q=${encodeURIComponent(q)}`).then((r) => r.data),
};
