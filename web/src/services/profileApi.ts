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

export interface Profile {
  id: number;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  age: number | null;
  weightKg: number | null;
  heightCm: number | null;
  goalType: string | null;
  fitnessLevel: string | null;
  sex: string | null;
  activityLevel: string | null;
  accountStatus: string;
  emailVerified: boolean;
  createdAt: string;
}

export interface WorkoutSessionItem {
  id: number;
  startTime: string;
  endTime: string | null;
  status: string;
  focusInterruptionsCount: number;
  totalSets: number;
  totalVolumeKg: number;
}

export interface WorkoutSetItem {
  id: number;
  setNumber: number;
  repsCompleted: number | null;
  weightUsed: number | null;
  restTimeSeconds: number | null;
}

export interface WorkoutSessionDetail extends WorkoutSessionItem {
  sets: WorkoutSetItem[];
}

export const profileApi = {
  getProfile: () => client.get<Profile>('/profile').then((r) => r.data),

  updateProfile: (body: {
    displayName?: string;
    avatarUrl?: string;
    age?: number;
    heightCm?: number;
    goalType?: string;
  }) => client.put<{ profile: Profile; goalChanged: boolean }>('/profile', body).then((r) => r.data),

  deleteAccount: () => client.delete<{ message: string }>('/account').then((r) => r.data),

  getSessions: (page = 0, size = 20) =>
    client
      .get<{ content: WorkoutSessionItem[]; totalElements: number; totalPages: number }>(
        `/workout-sessions?page=${page}&size=${size}`,
      )
      .then((r) => r.data),

  getSessionDetail: (id: number) =>
    client.get<WorkoutSessionDetail>(`/workout-sessions/${id}`).then((r) => r.data),
};
