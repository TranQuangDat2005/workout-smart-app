import { http } from './http';

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
  targetProtein: number;
  targetCarb: number;
  targetFat: number;
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

export interface NutritionNeeds {
  bmr: number;
  tdee: number;
  targetCalories: number;
  goalType: string;
  proteinG: number;
  carbG: number;
  fatG: number;
  perMealCalories: number;
  mealsPerDay: number;
  sex: string | null;
  age: number | null;
  heightCm: number | null;
  weightKg: number | null;
  activityLevel: string | null;
  calorieGoal: string | null;
}

export interface ImportFoodRow {
  name: string;
  gram: number;
  protein: number;
  carb: number;
  fat: number;
  calories: number;
}

export const nutritionApi = {
  searchFoods: (query: string, page = 0, size = 20) =>
    http
      .get<{ content: Food[]; totalElements: number; totalPages: number }>(`/foods?query=${encodeURIComponent(query)}&page=${page}&size=${size}`)
      .then((r) => r.data),

  createFood: (body: { name: string; caloriesPer100g: number; proteinPer100g: number; carbPer100g: number; fatPer100g: number }) =>
    http.post<Food>('/foods', body).then((r) => r.data),

  updateFood: (id: number, body: { name: string; caloriesPer100g: number; proteinPer100g: number; carbPer100g: number; fatPer100g: number }) =>
    http.put<Food>(`/foods/${id}`, body).then((r) => r.data),

  deleteFood: (id: number) => http.delete(`/foods/${id}`).then((r) => r.data),

  importFoods: (rows: ImportFoodRow[]) =>
    http.post<{ imported: number; errors: string[] }>('/foods/import', { rows }).then((r) => r.data),

  getMeals: (date: string) => http.get<Meal[]>(`/meals?date=${date}`).then((r) => r.data),

  createMeal: (body: { mealNumber: number; logDate: string; entries: MealEntryInput[] }) =>
    http.post<Meal>('/meals', body).then((r) => r.data),

  updateMeal: (id: number, body: { mealNumber: number; logDate: string; entries: MealEntryInput[] }) =>
    http.put<Meal>(`/meals/${id}`, body).then((r) => r.data),

  deleteMeal: (id: number) =>
    http.delete<{ message: string }>(`/meals/${id}`).then((r) => r.data),

  getSummary: (date: string) =>
    http.get<NutritionSummary>(`/nutrition/summary?date=${date}`).then((r) => r.data),

  createBodyMetric: (body: { weightKg: number; bodyFatPct?: number; waistCm?: number; chestCm?: number; armCm?: number }) =>
    http.post<BodyMetric>('/body-metrics', body).then((r) => r.data),

  getBodyMetrics: () => http.get<BodyMetric[]>('/body-metrics').then((r) => r.data),

  getNeeds: () => http.get<NutritionNeeds>('/nutrition/needs').then((r) => r.data),
};
