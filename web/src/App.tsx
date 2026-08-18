import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/useAuth';
import AppShell from './components/AppShell';
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

      <Route path="/" element={<Protected><HomePage /></Protected>} />
      <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />
      <Route path="/history" element={<Protected><WorkoutHistoryPage /></Protected>} />
      <Route path="/nutrition" element={<Protected><NutritionPage /></Protected>} />
      <Route path="/foods" element={<Protected><FoodLibraryPage /></Protected>} />
      <Route path="/body-metrics" element={<Protected><BodyMetricsPage /></Protected>} />
      <Route path="/friends" element={<Protected><FriendsPage /></Protected>} />
      <Route path="/leaderboard" element={<Protected><LeaderboardPage /></Protected>} />
      <Route path="/stats" element={<Protected><StatsPage /></Protected>} />
      <Route path="/goal-setup" element={<Protected><GoalSetupPage /></Protected>} />
      <Route path="/plan" element={<Protected><PlanPage /></Protected>} />
      <Route path="/exercises" element={<Protected><ExerciseSearchPage /></Protected>} />
      <Route path="/workout" element={<Protected><WorkoutPage /></Protected>} />
      <Route path="/admin/users" element={<Protected admin><AdminUsersPage /></Protected>} />
      <Route path="/admin/exercises" element={<Protected admin><AdminExercisesPage /></Protected>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function Protected({ children, admin = false }: { children: JSX.Element; admin?: boolean }) {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (admin && !isAdmin) return <Navigate to="/" replace />;
  return <AppShell>{children}</AppShell>;
}
