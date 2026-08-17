import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/useAuth';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import VerifyOtpPage from './pages/auth/VerifyOtpPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/profile/ProfilePage';
import WorkoutHistoryPage from './pages/profile/WorkoutHistoryPage';
import NutritionPage from './pages/nutrition/NutritionPage';
import FoodLibraryPage from './pages/nutrition/FoodLibraryPage';
import BodyMetricsPage from './pages/nutrition/BodyMetricsPage';
import FriendsPage from './pages/social/FriendsPage';
import LeaderboardPage from './pages/social/LeaderboardPage';
import StatsPage from './pages/stats/StatsPage';
import GoalSetupPage from './pages/plan/GoalSetupPage';
import PlanPage from './pages/plan/PlanPage';
import ExerciseSearchPage from './pages/plan/ExerciseSearchPage';
import WorkoutPage from './pages/tracking/WorkoutPage';
import AdminUsersPage from './pages/admin/AdminUsersPage';
import AdminExercisesPage from './pages/admin/AdminExercisesPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-otp" element={<VerifyOtpPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        }
      />
      <Route
        path="/profile"
        element={
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        }
      />
      <Route
        path="/history"
        element={
          <RequireAuth>
            <WorkoutHistoryPage />
          </RequireAuth>
        }
      />
      <Route
        path="/nutrition"
        element={
          <RequireAuth>
            <NutritionPage />
          </RequireAuth>
        }
      />
      <Route
        path="/foods"
        element={
          <RequireAuth>
            <FoodLibraryPage />
          </RequireAuth>
        }
      />
      <Route
        path="/body-metrics"
        element={
          <RequireAuth>
            <BodyMetricsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/friends"
        element={
          <RequireAuth>
            <FriendsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/leaderboard"
        element={
          <RequireAuth>
            <LeaderboardPage />
          </RequireAuth>
        }
      />
      <Route
        path="/stats"
        element={
          <RequireAuth>
            <StatsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/goal-setup"
        element={
          <RequireAuth>
            <GoalSetupPage />
          </RequireAuth>
        }
      />
      <Route
        path="/plan"
        element={
          <RequireAuth>
            <PlanPage />
          </RequireAuth>
        }
      />
      <Route
        path="/exercises"
        element={
          <RequireAuth>
            <ExerciseSearchPage />
          </RequireAuth>
        }
      />
      <Route
        path="/workout"
        element={
          <RequireAuth>
            <WorkoutPage />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/users"
        element={
          <RequireAuth>
            <AdminUsersPage />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/exercises"
        element={
          <RequireAuth>
            <AdminExercisesPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function RequireAuth({ children }: { children: JSX.Element }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}
