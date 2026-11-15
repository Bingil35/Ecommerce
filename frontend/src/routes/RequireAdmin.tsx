import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { hasRole, ROLES } from '../services/auth/roles';

export default function RequireAdmin() {
  const { user, isLoading } = useAuth();
  const location = useLocation();
  if (isLoading) return <div className="grid min-h-screen place-items-center text-sm text-slate-500">Đang kiểm tra quyền truy cập…</div>;
  return hasRole(user, ROLES.ADMIN) ? <Outlet /> : <Navigate to="/admin/login" replace state={{ from: location.pathname }} />;
}
