import { http } from './http';

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
    return http
      .get<{ content: ExerciseDetail[]; totalElements: number; totalPages: number }>(`/exercises?${search}`)
      .then((r) => r.data);
  },

  getExercise: (id: number) => http.get<ExerciseDetail>(`/exercises/${id}`).then((r) => r.data),
};
