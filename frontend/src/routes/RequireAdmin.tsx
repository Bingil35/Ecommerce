import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { hasRole, ROLES } from '../services/auth/roles';

export default function RequireAdmin() {
  const { user, isLoading } = useAuth();
  if (isLoading) return <div className="grid min-h-screen place-items-center text-sm text-slate-500">Đang kiểm tra quyền truy cập…</div>;
  return hasRole(user, ROLES.ADMIN) ? <Outlet /> : <Navigate to="/login" replace />;
}
