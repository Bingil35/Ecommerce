import { useEffect, useState } from 'react';
import { Link, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isShopOwner } from '../services/auth/roles';
import { shopApi } from '../services/shop/shopApi';

/** Backend xác thực vai trò, quyền sở hữu và trạng thái ACTIVE trước khi render. */
export default function RequireShopOwner() {
  const { user, isLoading } = useAuth();
  const location = useLocation();
  const [result, setResult] = useState<{ key: string; message: string } | null>(null);
  const key = `${user?.id}:${location.pathname}`;
  useEffect(() => {
    let cancelled = false;
    if (isLoading || !user) return;
    const check = async () => {
      try {
        const shop = await shopApi.getMyShop();
        if (!cancelled) setResult({ key, message: shop.status === 'ACTIVE' ? '' : shop.status === 'PENDING' ? 'Cửa hàng này chưa được duyệt. Vui lòng chờ quản trị viên xác nhận.' : 'Cửa hàng hiện không hoạt động.' });
      } catch (error: any) {
        if (!cancelled) setResult({ key, message: error.response?.data?.message || 'Không thể kiểm tra quyền quản lý gian hàng. Vui lòng thử lại.' });
      }
    };
    void check();
    const refresh = () => { void check(); };
    window.addEventListener('focus', refresh);
    return () => { cancelled = true; window.removeEventListener('focus', refresh); };
  }, [user, isLoading, key]);
  if (isLoading) return null;

  if (!isShopOwner(user)) {
    return (
      <Navigate
        to="/shop-owner/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  if (!result || result.key !== key) return null;
  if (result.message) {
    return <div role="alert" className="p-6">{result.message} <Link to="/">Về trang chủ</Link></div>;
  }

  return <Outlet />;
}
