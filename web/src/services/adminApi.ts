import { http } from './http';

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

export interface AdminExerciseDetail {
  id: number;
  name: string;
  category: string | null;
  bodyPart: string | null;
  equipment: string | null;
  target: string | null;
  muscleGroup: string | null;
  image: string | null;
  gifUrl: string | null;
  instructions: string | null;
  status: string;
}

export interface AdminUserDetail {
  profile: {
    id: number;
    email: string;
    displayName: string | null;
    avatarUrl: string | null;
    age: number | null;
    weightKg: number | null;
    heightCm: number | null;
    goalType: string | null;
    fitnessLevel: string | null;
    accountStatus: string;
    emailVerified: boolean;
  };
  recentSessions: Array<{
    id: number;
    startTime: string;
    endTime: string | null;
    status: string;
    focusInterruptionsCount: number;
    totalSets: number;
    totalVolumeKg: number;
  }>;
}

export const adminApi = {
  searchUsers: (q: string) => http.get<AdminUser[]>(`/admin/users?q=${encodeURIComponent(q)}`).then((r) => r.data),

  getUserDetail: (id: number) => http.get<AdminUserDetail>(`/admin/users/${id}`).then((r) => r.data),

  banUser: (id: number, reason: string) => http.post(`/admin/users/${id}/ban`, { reason }).then((r) => r.data),

  unbanUser: (id: number) => http.post(`/admin/users/${id}/unban`).then((r) => r.data),

  createExercise: (body: ExerciseInput) => http.post('/admin/exercises', body).then((r) => r.data),

  updateExercise: (id: number, body: Partial<ExerciseInput>) =>
    http.put(`/admin/exercises/${id}`, body).then((r) => r.data),

  getExercise: (id: number) => http.get<AdminExerciseDetail>(`/admin/exercises/${id}`).then((r) => r.data),

  importExercises: (items: ExerciseInput[]) =>
    http
      .post<{ inserted: number; updated: number; skipped: number }>('/admin/exercises/import', { items })
      .then((r) => r.data),

  setExerciseStatus: (id: number, status: 'active' | 'inactive', reason?: string) =>
    http.patch(`/admin/exercises/${id}/status`, { status, reason }).then((r) => r.data),

  listExercises: (q: string) =>
    http.get<AdminExercise[]>(`/admin/exercises?q=${encodeURIComponent(q)}`).then((r) => r.data),
};
