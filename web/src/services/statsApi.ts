import { http } from './http';

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

export interface TargetAttainment {
  achievedSets: number;
  totalSets: number;
  attainmentPct: number;
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
  targetAttainment: TargetAttainment;
  calories: CaloriePoint[];
}

export const statsApi = {
  dashboard: (from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    const qs = params.toString();
    return http
      .get<StatsDashboard>(`/stats/dashboard${qs ? `?${qs}` : ''}`)
      .then((r) => r.data);
  },
};
