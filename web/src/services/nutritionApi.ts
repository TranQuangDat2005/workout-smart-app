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

export interface Food {
  id: number;
  name: string;
  caloriesPer100g: number;
  proteinPer100g: number;
  carbPer100g: number;
  fatPer100g: number;
  source: string;
}

export interface MealEntryInput {
  foodItemId: number;
  portionGrams: number;
}

export interface MealEntry extends MealEntryInput {
  id: number;
  foodName: string;
  totalCalories: number;
  totalProtein: number;
  totalCarb: number;
  totalFat: number;
}

export interface Meal {
  id: number;
  mealNumber: number;
  logDate: string;
  entries: MealEntry[];
  totalCalories: number;
  totalProtein: number;
  totalCarb: number;
  totalFat: number;
}

export interface NutritionSummary {
  date: string;
  totalCalories: number;
  totalProtein: number;
  totalCarb: number;
  totalFat: number;
  targetCalories: number;
  deficitOrSurplus: number;
  status: string;
}

export interface BodyMetric {
  id: number;
  weightKg: number;
  bodyFatPct: number | null;
  waistCm: number | null;
  chestCm: number | null;
  armCm: number | null;
  deltaWeightKg: number | null;
  recordedAt: string;
}

export const nutritionApi = {
  searchFoods: (query: string, page = 0, size = 50) =>
    client
      .get<{ content: Food[]; totalElements: number }>(`/foods?query=${encodeURIComponent(query)}&page=${page}&size=${size}`)
      .then((r) => r.data),

  createFood: (body: { name: string; caloriesPer100g: number; proteinPer100g: number; carbPer100g: number; fatPer100g: number }) =>
    client.post<Food>('/foods', body).then((r) => r.data),

  updateFood: (id: number, body: { name: string; caloriesPer100g: number; proteinPer100g: number; carbPer100g: number; fatPer100g: number }) =>
    client.put<Food>(`/foods/${id}`, body).then((r) => r.data),

  deleteFood: (id: number) => client.delete(`/foods/${id}`).then((r) => r.data),

  getMeals: (date: string) => client.get<Meal[]>(`/meals?date=${date}`).then((r) => r.data),

  createMeal: (body: { mealNumber: number; logDate: string; entries: MealEntryInput[] }) =>
    client.post<Meal>('/meals', body).then((r) => r.data),

  updateMeal: (id: number, body: { mealNumber: number; logDate: string; entries: MealEntryInput[] }) =>
    client.put<Meal>(`/meals/${id}`, body).then((r) => r.data),

  getSummary: (date: string) =>
    client.get<NutritionSummary>(`/nutrition/summary?date=${date}`).then((r) => r.data),

  createBodyMetric: (body: { weightKg: number; bodyFatPct?: number; waistCm?: number; chestCm?: number; armCm?: number }) =>
    client.post<BodyMetric>('/body-metrics', body).then((r) => r.data),

  getBodyMetrics: () => client.get<BodyMetric[]>('/body-metrics').then((r) => r.data),
};
