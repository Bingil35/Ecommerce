import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useAuth } from '../context/AuthContext';

export const BECOME_SELLER_ENTRY = 'become-seller-menu';

/**
 * Chỉ cho vào trang đăng ký gian hàng khi người dùng đã đăng nhập
 * và đi từ menu tài khoản -> "Trở thành người bán".
 * Gõ URL trực tiếp (không có location.state) sẽ bị đưa về trang chủ.
 * Không chặn SHOP_OWNER ở đây để màn hình "Đã gửi đăng ký" còn hiển thị
 * sau khi login() cập nhật role; việc đó do chính trang đăng ký xử lý.
 */
export default function BecomeSellerGuard({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) return null;

  if (!isAuthenticated) return <Navigate to="/" replace />;

  const entry = (location.state as { entry?: string } | null)?.entry;
  if (entry !== BECOME_SELLER_ENTRY) return <Navigate to="/" replace />;

  return <>{children}</>;
}
