import { http } from './http';

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
  calorieGoal: string | null;
  customCalorieOffset: number | null;
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

export interface ExerciseProgress {
  exerciseId: number;
  exerciseName: string;
  firstWeight: number | null;
  lastWeight: number | null;
  weightDelta: number | null;
  firstReps: number | null;
  lastReps: number | null;
  repsDelta: number | null;
}

export const profileApi = {
  getProfile: () => http.get<Profile>('/profile').then((r) => r.data),

  updateProfile: (body: {
    displayName?: string;
    avatarUrl?: string;
    age?: number;
    heightCm?: number;
    weightKg?: number;
    sex?: string;
    activityLevel?: string;
    calorieGoal?: string;
    customCalorieOffset?: number;
    goalType?: string;
  }) => http.put<{ profile: Profile; goalChanged: boolean }>('/profile', body).then((r) => r.data),

  deleteAccount: (password?: string) =>
    http
      .delete<{ message: string }>('/account', { data: password ? { password } : undefined })
      .then((r) => r.data),

  getSessions: (page = 0, size = 20) =>
    http
      .get<{ content: WorkoutSessionItem[]; totalElements: number; totalPages: number }>(
        `/workout-sessions?page=${page}&size=${size}`,
      )
      .then((r) => r.data),

  getSessionDetail: (id: number) =>
    http.get<WorkoutSessionDetail>(`/workout-sessions/${id}`).then((r) => r.data),

  getProgress: (days = 30) =>
    http.get<ExerciseProgress[]>(`/workout-sessions/progress?days=${days}`).then((r) => r.data),

  clearHistory: () =>
    http.delete<{ message: string }>('/workout-sessions/history').then((r) => r.data),
};
