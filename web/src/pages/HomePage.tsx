import { useAuth } from '../context/useAuth';
import UserDashboard from './UserDashboard';
import AdminDashboard from './AdminDashboard';

/** Trang chủ — render dashboard theo vai trò (bố cục sidebar do AppShell đảm nhiệm). */
export default function HomePage() {
  const { isAdmin } = useAuth();
  return isAdmin ? <AdminDashboard /> : <UserDashboard />;
}
