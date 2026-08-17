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

export interface WeightPoint {
  date: string;
  weightKg: number;
}

export interface VolumePoint {
  weekStart: string;
  totalKg: number;
}

export interface StreakStats {
  currentStreakWeeks: number;
  longestStreakWeeks: number;
}

export interface PlanCompletion {
  completedSessions: number;
  plannedDays: number;
  completionPct: number;
}

export interface CaloriePoint {
  date: string;
  caloriesIn: number;
  caloriesBurned: number;
}

export interface StatsDashboard {
  weight: WeightPoint[];
  volume: VolumePoint[];
  streak: StreakStats;
  planCompletion: PlanCompletion;
  calories: CaloriePoint[];
}

export const statsApi = {
  dashboard: (from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    const qs = params.toString();
    return client
      .get<StatsDashboard>(`/stats/dashboard${qs ? `?${qs}` : ''}`)
      .then((r) => r.data);
  },
};
