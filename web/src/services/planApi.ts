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

export interface PlanExercise {
  id: number;
  exerciseId: number;
  exerciseName: string;
  targetSets: number;
  targetReps: number;
  restTimeSeconds: number;
  image: string | null;
  gifUrl: string | null;
}

export interface PlanDay {
  id: number;
  dayOfWeek: number;
  exercises: PlanExercise[];
}

export interface WorkoutPlan {
  id: number;
  name: string;
  goalType: string;
  fitnessLevel: string;
  status: string;
  days: PlanDay[];
}

export interface ExerciseDetail {
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
}

export const planApi = {
  setupGoal: (body: {
    goalType: string;
    fitnessLevel: string;
    equipment: string[];
    effectiveDate?: string;
  }) =>
    client
      .put<{ goalType: string; fitnessLevel: string; equipment: string[]; planId: number; warning: string | null }>(
        '/users/me/goals',
        body,
      )
      .then((r) => r.data),

  getActivePlan: () => client.get<WorkoutPlan>('/workout-plans/active').then((r) => r.data),

  generatePlan: (effectiveDate?: string) =>
    client.post<WorkoutPlan>('/workout-plans/generate', { effectiveDate }).then((r) => r.data),

  searchExercises: (params: {
    equipment?: string;
    category?: string;
    bodyPart?: string;
    muscleGroup?: string;
    q?: string;
    page?: number;
    size?: number;
  }) => {
    const search = new URLSearchParams();
    if (params.equipment) search.set('equipment', params.equipment);
    if (params.category) search.set('category', params.category);
    if (params.bodyPart) search.set('bodyPart', params.bodyPart);
    if (params.muscleGroup) search.set('muscleGroup', params.muscleGroup);
    if (params.q) search.set('q', params.q);
    search.set('page', String(params.page ?? 0));
    search.set('size', String(params.size ?? 20));
    return client
      .get<{ content: ExerciseDetail[]; totalElements: number; totalPages: number }>(`/exercises?${search}`)
      .then((r) => r.data);
  },

  getExercise: (id: number) => client.get<ExerciseDetail>(`/exercises/${id}`).then((r) => r.data),
};
