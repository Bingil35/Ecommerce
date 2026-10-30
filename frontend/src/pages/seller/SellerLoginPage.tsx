import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  Check,
  Eye,
  EyeOff,
  LockKeyhole,
  Store,
} from 'lucide-react';

import axiosClient from '../../services/api/axiosClient';
import type { ApiResponse } from '../../services/api/apiTypes';
import type { LoginResponse } from '../../services/auth/authTypes';
import { ROLES, hasRole, isShopOwner } from '../../services/auth/roles';
import { useAuth } from '../../context/AuthContext';
import { AppleIcon, GoogleIcon } from '../../components/icons';
import SellerAuthShell from '../../components/seller/auth/SellerAuthShell';
import SellerField from '../../components/seller/auth/SellerField';

interface LocationState {
  from?: string;
}

export default function SellerLoginPage() {
  const { login, user, isLoading: isAuthLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as LocationState | null)?.from;

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [noShop, setNoShop] = useState(false);

  // Đã là chủ shop và đang đăng nhập thì vào thẳng kênh người bán.
  if (!isAuthLoading && isShopOwner(user)) {
    return <Navigate to="/seller" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage('');
    setNoShop(false);

    if (!identifier.trim()) {
      setErrorMessage('Vui lòng nhập email hoặc số điện thoại.');
      return;
    }

    if (!password) {
      setErrorMessage('Vui lòng nhập mật khẩu.');
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await axiosClient.post<ApiResponse<LoginResponse>>(
        '/auth/seller/login',
        { identifier: identifier.trim(), password }
      );

      const { accessToken, user: loggedInUser } = response.data.data;

      // Chỉ tài khoản có role SHOP_OWNER mới được vào kênh người bán.
      if (isShopOwner(loggedInUser)) {
        login(accessToken, loggedInUser, rememberMe);
        navigate(from?.startsWith('/seller') ? from : '/seller', {
          replace: true,
        });
        return;
      }

      // Chưa có gian hàng: KHÔNG gọi login(), không lưu token/user.
      if (hasRole(loggedInUser, ROLES.CUSTOMER)) {
        setNoShop(true);
        return;
      }

      setErrorMessage('Tài khoản này không dùng được kênh người bán.');
    } catch (error: any) {
      // 403: đúng thông tin đăng nhập nhưng không có role SHOP_OWNER (chưa có gian hàng).
      if (error.response?.status === 403) {
        setNoShop(true);
        return;
      }

      setErrorMessage(
        error.response?.data?.message ||
          'Đăng nhập thất bại. Vui lòng thử lại.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <SellerAuthShell>
      <div className="mb-9">
        <h2 className="text-[32px] font-bold tracking-tight text-[#1F1B6B]">
          Đăng nhập
        </h2>
        <p className="mt-2 text-[15px] text-[#667398]">
          Vui lòng đăng nhập để quản lý cửa hàng của bạn
        </p>
      </div>

      {errorMessage && (
        <div
          role="alert"
          className="mb-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-600"
        >
          {errorMessage}
        </div>
      )}

      {noShop && (
        <div
          role="alert"
          className="mb-5 rounded-xl border border-[#BFD3F8] bg-[#EEF4FF] px-4 py-3 text-sm leading-5 text-[#2F57B5]"
        >
          Tài khoản này chưa có gian hàng. Hãy{' '}
          <Link to="/login" className="font-semibold underline">
            đăng nhập tại trang mua sắm
          </Link>
          , mở menu tài khoản và chọn “Mở gian hàng” để đăng ký.
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        <SellerField
          id="seller-identifier"
          label="Email hoặc số điện thoại của cửa hàng"
          icon={Store}
          type="text"
          value={identifier}
          placeholder="Email hoặc số điện thoại của cửa hàng"
          autoComplete="username"
          onChange={(event) => setIdentifier(event.target.value)}
        />

        <SellerField
          id="seller-password"
          label="Mật khẩu"
          icon={LockKeyhole}
          type={showPassword ? 'text' : 'password'}
          value={password}
          placeholder="Mật khẩu"
          autoComplete="current-password"
          onChange={(event) => setPassword(event.target.value)}
          trailing={
            <button
              type="button"
              onClick={() => setShowPassword((previous) => !previous)}
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              className="text-[#8A98B8] transition hover:text-[#3B7BF6]"
            >
              {showPassword ? (
                <EyeOff className="h-[22px] w-[22px]" strokeWidth={1.7} />
              ) : (
                <Eye className="h-[22px] w-[22px]" strokeWidth={1.7} />
              )}
            </button>
          }
        />

        <div className="flex items-center justify-between">
          <label className="flex cursor-pointer items-center gap-2.5 text-sm text-[#52638F]">
            <span className="relative flex h-5 w-5 shrink-0">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
                className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-[#C9D4E8] bg-white checked:border-[#3B7BF6] checked:bg-[#3B7BF6]"
              />
              <Check
                className="pointer-events-none absolute inset-0 m-auto hidden h-3.5 w-3.5 text-white peer-checked:block"
                strokeWidth={3}
              />
            </span>
            Ghi nhớ đăng nhập
          </label>

          <Link
            to="/forgot-password"
            className="text-sm font-medium text-[#2F6BEA] hover:underline"
          >
            Quên mật khẩu?
          </Link>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex h-[58px] w-full items-center justify-center gap-2 rounded-full bg-[#3B7BF6] text-[15px] font-semibold text-white shadow-[0_10px_24px_rgba(59,123,246,0.28)] transition hover:bg-[#2F6BEA] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          {!isSubmitting && <ArrowRight className="h-5 w-5" strokeWidth={2} />}
        </button>
      </form>

      <div className="my-8 flex items-center gap-4">
        <div className="h-px flex-1 bg-[#E1E7F2]" />
        <span className="whitespace-nowrap text-sm text-[#8E9BBB]">
          Hoặc đăng nhập với
        </span>
        <div className="h-px flex-1 bg-[#E1E7F2]" />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <button
          type="button"
          className="flex h-[54px] items-center justify-center gap-3 rounded-full border border-[#D5E0F5] bg-white text-sm font-medium text-[#42517F] transition hover:bg-[#F6F9FF]"
        >
          <GoogleIcon className="h-[19px] w-[19px]" />
          Google
        </button>

        <button
          type="button"
          className="flex h-[54px] items-center justify-center gap-3 rounded-full border border-[#D5E0F5] bg-white text-sm font-medium text-[#42517F] transition hover:bg-[#F6F9FF]"
        >
          <AppleIcon className="h-[19px] w-[19px]" />
          Apple
        </button>
      </div>

      <p className="mt-9 text-center text-sm text-[#667398]">
        <Link to="/" className="hover:underline">
          Quay lại trang mua sắm
        </Link>
      </p>
    </SellerAuthShell>
  );
}
