import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { hasRole, isShopOwner, ROLES } from '../../services/auth/roles';
import HomePage from './HomePage';

export default function DomainHomePage() {
  const host = window.location.hostname.toLowerCase();
  const { user, isLoading } = useAuth();
  if (isLoading) return null;
  if (host.startsWith('admin.')) return <Navigate to={hasRole(user, ROLES.ADMIN) ? '/admin' : '/admin/login'} replace />;
  if (host.startsWith('shop.')) return <Navigate to={isShopOwner(user) ? '/shop-owner' : '/shop-owner/login'} replace />;
  return <HomePage />;
}
