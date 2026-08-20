import { http } from './http';
import type { SetTarget } from './setTarget';

export interface PlanExercise {
  id: number;
  exerciseId: number;
  exerciseName: string;
  targetSets: number;
  targetReps: number;
  restTimeSeconds: number;
  image: string | null;
  gifUrl: string | null;
  sets?: SetTarget[];
  targetDurationSeconds?: number | null;
  measureType?: string;
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
  source: string | null;
  measureType?: string;
}

export interface CustomExerciseInput {
  name: string;
  muscleGroup: string;
  equipment: string;
  category: string;
  bodyPart?: string;
  target?: string;
  image?: string;
  gifUrl?: string;
  instructions?: string;
}

/** Chuyển path media (images/..., videos/...) thành URL qua backend (/media/**). */
export function mediaUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  if (path.startsWith('http')) return path;
  return `/media/${path}`;
}

export const planApi = {
  setupGoal: (body: {
    goalType: string;
    fitnessLevel: string;
    equipment: string[];
    effectiveDate?: string;
  }) =>
    http
      .put<{ goalType: string; fitnessLevel: string; equipment: string[]; planId: number; warning: string | null }>(
        '/users/me/goals',
        body,
      )
      .then((r) => r.data),

  getActivePlan: () => http.get<WorkoutPlan>('/workout-plans/active').then((r) => r.data),

  generatePlan: (effectiveDate?: string) =>
    http.post<WorkoutPlan>('/workout-plans/generate', { effectiveDate }).then((r) => r.data),

  searchExercises: (params: {
    equipment?: string | string[];
    category?: string | string[];
    bodyPart?: string;
    muscleGroup?: string;
    q?: string;
    page?: number;
    size?: number;
  }) => {
    const search = new URLSearchParams();
    const appendAll = (key: string, value?: string | string[]) => {
      if (!value) return;
      const list = Array.isArray(value) ? value : [value];
      list.filter(Boolean).forEach((item) => search.append(key, item));
    };
    appendAll('equipment', params.equipment);
    appendAll('category', params.category);
    if (params.bodyPart) search.set('bodyPart', params.bodyPart);
    if (params.muscleGroup) search.set('muscleGroup', params.muscleGroup);
    if (params.q) search.set('q', params.q);
    search.set('page', String(params.page ?? 0));
    search.set('size', String(params.size ?? 20));
    return http
      .get<{ content: ExerciseDetail[]; totalElements: number; totalPages: number }>(`/exercises?${search}`)
      .then((r) => r.data);
  },

  replaceDayExercises: (
    dayId: number,
    exercises: {
      exerciseId: number;
      targetSets: number;
      targetReps: number;
      restTimeSeconds: number;
      sets?: SetTarget[];
      targetDurationSeconds?: number | null;
      measureType?: string;
    }[],
  ) =>
    http
      .put<WorkoutPlan>(`/workout-plans/active/days/${dayId}/exercises`, { exercises })
      .then((r) => r.data),

  createDay: (dayOfWeek: number) =>
    http
      .post<PlanDay>(`/workout-plans/active/days?dayOfWeek=${dayOfWeek}`)
      .then((r) => r.data),

  getExercise: (id: number) => http.get<ExerciseDetail>(`/exercises/${id}`).then((r) => r.data),

  createCustomExercise: (body: CustomExerciseInput) =>
    http.post<ExerciseDetail>('/exercises/custom', body).then((r) => r.data),

  updateCustomExercise: (id: number, body: Partial<CustomExerciseInput>) =>
    http.put<ExerciseDetail>(`/exercises/custom/${id}`, body).then((r) => r.data),

  deleteCustomExercise: (id: number) =>
    http.delete(`/exercises/custom/${id}`).then((r) => r.data),

  uploadMedia: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    // Không ép Content-Type — để browser tự sinh boundary multipart
    return http.post<{ url: string }>('/media/upload', form).then((r) => r.data);
  },
};
