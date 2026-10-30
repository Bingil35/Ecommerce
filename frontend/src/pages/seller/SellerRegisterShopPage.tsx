import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  CheckCircle2,
  Info,
  MapPin,
  Phone,
  Store,
} from 'lucide-react';

import { useAuth } from '../../context/AuthContext';
import { authStorage } from '../../services/auth/authStorage';
import { isShopOwner } from '../../services/auth/roles';
import { shopApi } from '../../services/shop/shopApi';
import type { ShopResponse } from '../../services/shop/shopTypes';
import SellerAuthShell from '../../components/seller/auth/SellerAuthShell';
import SellerField from '../../components/seller/auth/SellerField';

// Khớp quy tắc của backend: cột shops.phone dài 10 ký tự.
const SHOP_PHONE_PATTERN = /^0\d{9}$/;

interface LocationState {
  notice?: string;
}

export default function SellerRegisterShopPage() {
  const { user, isLoading: isAuthLoading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const notice = (location.state as LocationState | null)?.notice;

  const [shopName, setShopName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdShop, setCreatedShop] = useState<ShopResponse | null>(null);

  if (isAuthLoading) {
    return null;
  }

  // Đã có gian hàng thì không đăng ký lại (trừ lúc đang hiện màn hình thành công).
  if (!createdShop && isShopOwner(user)) {
    return <Navigate to="/seller" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage('');

    const name = shopName.trim();
    const shopPhone = phone.trim();

    if (!name) {
      setErrorMessage('Vui lòng nhập tên gian hàng.');
      return;
    }

    if (shopPhone && !SHOP_PHONE_PATTERN.test(shopPhone)) {
      setErrorMessage(
        'Số điện thoại gian hàng gồm 10 chữ số và bắt đầu bằng 0.'
      );
      return;
    }

    setIsSubmitting(true);

    try {
      // Phải đọc trước khi login() ghi đè phiên: giữ nguyên lựa chọn "ghi nhớ đăng nhập".
      const remembered = authStorage.isRemembered();

      const result = await shopApi.register({
        shopName: name,
        phone: shopPhone || undefined,
        address: address.trim() || undefined,
      });

      // Token và thông tin user mới đã có thêm role SHOP_OWNER.
      login(result.accessToken, result.user, remembered);
      setCreatedShop(result.shop);
    } catch (error: any) {
      setErrorMessage(
        error.response?.data?.message ||
          'Đăng ký gian hàng thất bại. Vui lòng thử lại.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  if (createdShop) {
    return (
      <SellerAuthShell>
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#E6F6EC] text-[#16A34A]">
          <CheckCircle2 className="h-8 w-8" strokeWidth={1.8} />
        </div>

        <h2 className="mt-6 text-[30px] font-bold tracking-tight text-[#1F1B6B]">
          Đã gửi đăng ký gian hàng
        </h2>

        <p className="mt-3 text-[15px] leading-6 text-[#667398]">
          <strong className="text-[#1F1B6B]">{createdShop.shopName}</strong>{' '}
          đang chờ Kala xác nhận. Khách hàng sẽ nhìn thấy gian hàng sau khi
          được duyệt. Trong lúc chờ, bạn có thể vào kênh người bán để chuẩn bị
          sản phẩm.
        </p>

        <button
          type="button"
          onClick={() => navigate('/seller', { replace: true })}
          className="mt-8 flex h-[58px] w-full items-center justify-center gap-2 rounded-full bg-[#3B7BF6] text-[15px] font-semibold text-white shadow-[0_10px_24px_rgba(59,123,246,0.28)] transition hover:bg-[#2F6BEA]"
        >
          Vào kênh người bán
          <ArrowRight className="h-5 w-5" strokeWidth={2} />
        </button>

        <p className="mt-6 text-center text-sm text-[#667398]">
          <Link to="/" className="hover:underline">
            Quay lại trang mua sắm
          </Link>
        </p>
      </SellerAuthShell>
    );
  }

  return (
    <SellerAuthShell>
      <div className="mb-8">
        <h2 className="text-[32px] font-bold tracking-tight text-[#1F1B6B]">
          Đăng ký mở gian hàng
        </h2>
        <p className="mt-2 text-[15px] text-[#667398]">
          Tài khoản{' '}
          <strong className="text-[#34457A]">
            {user?.fullName || user?.email}
          </strong>{' '}
          sẽ là chủ gian hàng.
        </p>
      </div>

      {notice && !errorMessage && (
        <div className="mb-5 flex gap-2.5 rounded-xl border border-[#BFD3F8] bg-[#EEF4FF] px-4 py-3 text-sm leading-5 text-[#2F57B5]">
          <Info className="mt-0.5 h-4 w-4 shrink-0" strokeWidth={2} />
          <span>{notice}</span>
        </div>
      )}

      {errorMessage && (
        <div
          role="alert"
          className="mb-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-600"
        >
          {errorMessage}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        <SellerField
          id="shop-name"
          label="Tên gian hàng"
          icon={Store}
          type="text"
          value={shopName}
          placeholder="Tên gian hàng"
          autoComplete="organization"
          maxLength={255}
          onChange={(event) => setShopName(event.target.value)}
        />

        <SellerField
          id="shop-phone"
          label="Số điện thoại gian hàng (không bắt buộc)"
          icon={Phone}
          type="tel"
          inputMode="tel"
          value={phone}
          placeholder="Số điện thoại gian hàng (không bắt buộc)"
          autoComplete="tel"
          onChange={(event) => setPhone(event.target.value)}
        />

        <SellerField
          id="shop-address"
          label="Địa chỉ gian hàng (không bắt buộc)"
          icon={MapPin}
          type="text"
          value={address}
          placeholder="Địa chỉ gian hàng (không bắt buộc)"
          autoComplete="street-address"
          onChange={(event) => setAddress(event.target.value)}
        />

        <p className="flex gap-2 text-sm leading-5 text-[#667398]">
          <Info className="mt-0.5 h-4 w-4 shrink-0" strokeWidth={1.8} />
          Gian hàng cần được Kala xác nhận trước khi hiển thị với khách hàng.
        </p>

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex h-[58px] w-full items-center justify-center gap-2 rounded-full bg-[#3B7BF6] text-[15px] font-semibold text-white shadow-[0_10px_24px_rgba(59,123,246,0.28)] transition hover:bg-[#2F6BEA] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? 'Đang gửi đăng ký...' : 'Đăng ký mở gian hàng'}
          {!isSubmitting && <ArrowRight className="h-5 w-5" strokeWidth={2} />}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-[#667398]">
        <Link to="/" className="hover:underline">
          Quay lại trang mua sắm
        </Link>
      </p>
    </SellerAuthShell>
  );
}
