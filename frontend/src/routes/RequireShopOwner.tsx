import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAuth } from '../context/AuthContext';
import { isShopOwner } from '../services/auth/roles';

/** Khu vực kênh người bán: chỉ tài khoản có role SHOP_OWNER mới được vào. */
export default function RequireShopOwner() {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) return null;

  if (!isShopOwner(user)) {
    return (
      <Navigate
        to="/seller/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  return <Outlet />;
}
