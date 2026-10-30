import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { LogOut, Store } from 'lucide-react';

import { useAuth } from '../../context/AuthContext';
import { isShopOwner } from '../../services/auth/roles';
import { shopApi } from '../../services/shop/shopApi';
import type { ShopResponse } from '../../services/shop/shopTypes';

const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Chờ xác nhận',
  ACTIVE: 'Đang hoạt động',
  CLOSED: 'Đã đóng',
};

/**
 * Trang đích tạm của kênh người bán sau khi đăng nhập: hiển thị gian hàng và trạng thái duyệt.
 * Thay bằng dashboard thật khi làm các chức năng quản lý.
 */
export default function SellerDashboardPage() {
  const { user, isLoading: isAuthLoading, logout } = useAuth();

  const [shop, setShop] = useState<ShopResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState('');

  const canLoad = !isAuthLoading && isShopOwner(user);

  useEffect(() => {
    if (!canLoad) {
      return;
    }

    let cancelled = false;

    shopApi
      .getMyShop()
      .then((data) => {
        if (!cancelled) {
          setShop(data);
        }
      })
      .catch((error: any) => {
        if (cancelled) {
          return;
        }

        // Backend từ chối quyền (role đã bị thu hồi hoặc phiên cũ): đăng xuất.
        if (error.response?.status === 403) {
          logout();
          return;
        }

        setErrorMessage(
          error.response?.data?.message ||
            'Không tải được thông tin gian hàng.'
        );
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canLoad]);

  if (isAuthLoading) {
    return null;
  }

  if (!isShopOwner(user)) {
    return <Navigate to="/seller/login" replace />;
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <div className="rounded-2xl border border-[#E1E7F2] bg-white p-6 shadow-sm">
        <div className="flex items-center gap-3">
          <span className="grid h-12 w-12 place-items-center rounded-xl bg-[#E6EFFF] text-[#3B7BF6]">
            <Store className="h-6 w-6" strokeWidth={1.8} />
          </span>
          <div>
            <h1 className="text-xl font-bold text-[#1F1B6B]">
              {shop?.shopName ?? 'Kênh người bán'}
            </h1>
            <p className="text-sm text-[#667398]">{user?.email}</p>
          </div>
        </div>

        {errorMessage && (
          <p role="alert" className="mt-5 text-sm text-red-600">
            {errorMessage}
          </p>
        )}

        {shop && (
          <dl className="mt-6 grid grid-cols-[120px_1fr] gap-y-3 text-sm">
            <dt className="text-[#667398]">Trạng thái</dt>
            <dd>
              <span
                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  shop.status === 'ACTIVE'
                    ? 'bg-[#E6F6EC] text-[#16A34A]'
                    : 'bg-[#FFF4DC] text-[#9A6B00]'
                }`}
              >
                {STATUS_LABELS[shop.status] ?? shop.status}
              </span>
            </dd>

            <dt className="text-[#667398]">Đường dẫn</dt>
            <dd className="text-[#1F1B6B]">/shop/{shop.slug}</dd>

            <dt className="text-[#667398]">Điện thoại</dt>
            <dd className="text-[#1F1B6B]">{shop.phone || 'Chưa cập nhật'}</dd>

            <dt className="text-[#667398]">Địa chỉ</dt>
            <dd className="text-[#1F1B6B]">{shop.address || 'Chưa cập nhật'}</dd>
          </dl>
        )}

        <div className="mt-8 flex items-center justify-between border-t border-[#EEF1F6] pt-4 text-sm">
          <Link to="/" className="font-medium text-[#2F6BEA] hover:underline">
            Về trang mua sắm
          </Link>

          <button
            type="button"
            onClick={logout}
            className="flex items-center gap-2 text-[#6B7280] hover:text-[#DC5A5A]"
          >
            <LogOut className="h-4 w-4" strokeWidth={1.8} />
            Đăng xuất
          </button>
        </div>
      </div>
    </div>
  );
}
