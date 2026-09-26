import {
  useState,
  type FormEvent,
  type ReactNode,
} from 'react';

import { Link } from 'react-router-dom';
import axiosClient from '../../services/api/axiosClient';
import logo from '../../assets/logo.png';
import heroImage from '../../assets/hero_log.png';
import { useAuth } from '../../context/AuthContext';
import type { LoginResponse } from '../../services/auth/authTypes';

import {
  AppleIcon,
  EyeIcon,
  GoogleIcon,
  LockIcon,
  MailIcon,
} from '../../components/icons';

interface LoginForm {
  identifier: string;
  password: string;
}

export default function LoginPage() {
  const { login } = useAuth();

  const [form, setForm] = useState<LoginForm>({
    identifier: '',
    password: '',
  });

  const [rememberMe, setRememberMe] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (
    field: keyof LoginForm,
    value: string
  ) => {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));
  };

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();
    setErrorMessage('');

    if (!form.identifier.trim()) {
      setErrorMessage(
        'Vui lòng nhập email hoặc số điện thoại.'
      );
      return;
    }

    if (!form.password) {
      setErrorMessage('Vui lòng nhập mật khẩu.');
      return;
    }

    setIsLoading(true);

    try {
      const response = await axiosClient.post<{
        success: boolean;
        message: string;
        data: LoginResponse;
      }>('/auth/login', {
        identifier: form.identifier.trim(),
        password: form.password,
      });

      const loginData = response.data.data;

      login(
        loginData.accessToken,
        loginData.user,
        rememberMe
      );
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Đăng nhập thất bại. Vui lòng thử lại.';

      setErrorMessage(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F9FC] px-4 py-6 sm:px-6 lg:px-10">
      <div className="mx-auto flex min-h-[calc(100vh-3rem)] max-w-6xl overflow-hidden rounded-[28px] bg-white shadow-[0_20px_60px_rgba(31,41,55,0.08)]">

        {/* =====================================================
            LEFT - BRANDING
        ===================================================== */}

        <section className="hidden w-1/2 bg-[#EEF4FF] lg:flex">
          <div className="flex w-full flex-col px-12 py-10 xl:px-14">

            {/* Logo */}
            <Link
              to="/"
              className="w-fit"
            >
              <img
                src={logo}
                alt="Kala"
                className="h-20 w-auto object-contain"
              />
            </Link>

            {/* Welcome */}
            <div className="mt-10">
              <h1 className="max-w-md text-[32px] font-bold leading-[1.2] tracking-tight text-[#29266F]">
                Chào mừng bạn
                <br />
                đến với Kala!
              </h1>

              <p className="mt-4 max-w-md text-[15px] leading-6 text-[#4E5780]">
                Khám phá hàng ngàn sản phẩm chất lượng
                <br />
                và trải nghiệm mua sắm tiện lợi, an toàn.
              </p>
            </div>

            {/* Hero */}
            <div className="flex flex-1 items-center justify-center">
              <img
                src={heroImage}
                alt="Kala shopping"
                className="w-full max-w-[500px] object-contain"
              />
            </div>

            {/* Benefits */}
            <div className="grid grid-cols-4">
              <Benefit
                icon={<ShieldIcon />}
                title="Sản phẩm"
                description="chính hãng"
              />

              <Benefit
                icon={<TruckIcon />}
                title="Giao hàng"
                description="nhanh chóng"
              />

              <Benefit
                icon={<HeadphoneIcon />}
                title="Hỗ trợ"
                description="24/7"
              />

              <Benefit
                icon={<StarIcon />}
                title="Trải nghiệm"
                description="mua sắm tốt nhất"
              />
            </div>
          </div>
        </section>

        {/* =====================================================
            RIGHT - LOGIN
        ===================================================== */}

        <section className="flex w-full items-center justify-center px-6 py-8 sm:px-10 lg:w-1/2 lg:px-12 xl:px-14">
          <div className="w-full max-w-[430px]">

            {/* Mobile logo */}
            <div className="mb-8 lg:hidden">
              <Link to="/">
                <img
                  src={logo}
                  alt="Kala"
                  className="h-10 w-auto object-contain"
                />
              </Link>
            </div>

            {/* Heading */}
            <div className="mb-9">
              <h2 className="text-[30px] font-bold tracking-tight text-[#29266F]">
                Đăng nhập
              </h2>

              <p className="mt-2 text-[15px] text-[#667085]">
                Vui lòng đăng nhập để tiếp tục
              </p>
            </div>

            {/* Error */}
            {errorMessage && (
              <div className="mb-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-600">
                {errorMessage}
              </div>
            )}

            {/* Login form */}
            <form
              onSubmit={handleSubmit}
              className="space-y-5"
            >

              {/* Email / Phone */}
              <div>
                <label
                  htmlFor="identifier"
                  className="sr-only"
                >
                  Email hoặc số điện thoại
                </label>

                <div className="relative">
                  <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#52638F]">
                    <MailIcon className="h-[22px] w-[22px]" />
                  </span>

                  <input
                    id="identifier"
                    type="text"
                    value={form.identifier}
                    placeholder="Email hoặc số điện thoại"
                    autoComplete="username"
                    onChange={(event) =>
                      handleChange(
                        'identifier',
                        event.target.value
                      )
                    }
                    className="h-[60px] w-full rounded-xl border border-[#D7E0F2] bg-white pl-14 pr-4 text-[15px] text-[#172033] outline-none transition placeholder:text-[#9AA8C6] focus:border-[#5B8DEF] focus:ring-4 focus:ring-[#5B8DEF]/10"
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <label
                  htmlFor="password"
                  className="sr-only"
                >
                  Mật khẩu
                </label>

                <div className="relative">
                  <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#52638F]">
                    <LockIcon className="h-[22px] w-[22px]" />
                  </span>

                  <input
                    id="password"
                    type={
                      showPassword
                        ? 'text'
                        : 'password'
                    }
                    value={form.password}
                    placeholder="Mật khẩu"
                    autoComplete="current-password"
                    onChange={(event) =>
                      handleChange(
                        'password',
                        event.target.value
                      )
                    }
                    className="h-[60px] w-full rounded-xl border border-[#D7E0F2] bg-white pl-14 pr-14 text-[15px] text-[#172033] outline-none transition placeholder:text-[#9AA8C6] focus:border-[#5B8DEF] focus:ring-4 focus:ring-[#5B8DEF]/10"
                  />

                  <button
                    type="button"
                    onClick={() =>
                      setShowPassword(
                        (previous) => !previous
                      )
                    }
                    aria-label={
                      showPassword
                        ? 'Ẩn mật khẩu'
                        : 'Hiện mật khẩu'
                    }
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-[#8A98B8] transition hover:text-[#4F7CFF]"
                  >
                    <EyeIcon
                      className="h-[22px] w-[22px]"
                      visible={showPassword}
                    />
                  </button>
                </div>
              </div>

              {/* Remember + Forgot password */}
              <div className="flex items-center justify-between">
                <label className="flex cursor-pointer items-center gap-2.5 text-sm text-[#667085]">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(event) =>
                      setRememberMe(
                        event.target.checked
                      )
                    }
                    className="h-5 w-5 cursor-pointer appearance-none rounded-md border border-[#C9D4E8] bg-white checked:border-[#4F7CFF] checked:bg-[#4F7CFF]"
                  />

                  <span>
                    Nhớ tài khoản
                  </span>
                </label>

                <Link
                  to="/forgot-password"
                  className="text-sm font-medium text-[#3975E8] hover:underline"
                >
                  Quên mật khẩu?
                </Link>
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={isLoading}
                className="flex h-[56px] w-full items-center justify-center gap-2 rounded-full bg-[#4F7CFF] text-[15px] font-semibold text-white shadow-[0_8px_20px_rgba(79,124,255,0.2)] transition hover:bg-[#3D63D8] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isLoading
                  ? 'Đang đăng nhập...'
                  : 'Đăng nhập'}

                {!isLoading && (
                  <span className="text-lg">
                    →
                  </span>
                )}
              </button>
            </form>

            {/* Divider */}
            <div className="my-8 flex items-center gap-4">
              <div className="h-px flex-1 bg-[#E1E7F2]" />

              <span className="whitespace-nowrap text-sm text-[#9AA8C6]">
                Hoặc đăng nhập với
              </span>

              <div className="h-px flex-1 bg-[#E1E7F2]" />
            </div>

            {/* Social login */}
            <div className="grid grid-cols-2 gap-4">
              <button
                type="button"
                className="flex h-[52px] items-center justify-center gap-3 rounded-full border border-[#D7E0F2] bg-white text-sm font-medium text-[#52638F] transition hover:bg-[#F8FAFE]"
              >
                <GoogleIcon className="h-[19px] w-[19px]" />
                Google
              </button>

              <button
                type="button"
                className="flex h-[52px] items-center justify-center gap-3 rounded-full border border-[#D7E0F2] bg-white text-sm font-medium text-[#52638F] transition hover:bg-[#F8FAFE]"
              >
                <AppleIcon className="h-[19px] w-[19px]" />
                Apple
              </button>
            </div>

            {/* Register */}
            <p className="mt-9 text-center text-sm text-[#667085]">
              Chưa có tài khoản?{' '}

              <Link
                to="/register"
                className="font-semibold text-[#3975E8] hover:underline"
              >
                Đăng ký
              </Link>
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}

/* =====================================================
   BENEFIT
===================================================== */

interface BenefitProps {
  icon: ReactNode;
  title: string;
  description: string;
}

function Benefit({
  icon,
  title,
  description,
}: BenefitProps) {
  return (
    <div className="flex items-center gap-3 border-r border-[#D3DEF2] px-4 first:pl-0 last:border-r-0">
      <div className="shrink-0 text-[#415A9B]">
        {icon}
      </div>

      <div>
        <p className="text-xs font-medium text-[#52638F]">
          {title}
        </p>

        <p className="text-xs text-[#52638F]">
          {description}
        </p>
      </div>
    </div>
  );
}

/* =====================================================
   BENEFIT ICONS
===================================================== */

function ShieldIcon() {
  return (
    <svg
      width="26"
      height="26"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="shrink-0"
    >
      <path d="M12 3 20 6v5c0 5-3.4 8.7-8 10-4.6-1.3-8-5-8-10V6l8-3Z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}

function TruckIcon() {
  return (
    <svg
      width="26"
      height="26"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="shrink-0"
    >
      <path d="M3 6h11v10H3z" />
      <path d="M14 10h4l3 3v3h-7z" />

      <circle
        cx="7"
        cy="18"
        r="2"
      />

      <circle
        cx="18"
        cy="18"
        r="2"
      />
    </svg>
  );
}

function HeadphoneIcon() {
  return (
    <svg
      width="26"
      height="26"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="shrink-0"
    >
      <path d="M4 14v-2a8 8 0 0 1 16 0v2" />

      <path d="M4 14h3v5H5a1 1 0 0 1-1-1v-4Z" />

      <path d="M20 14h-3v5h2a1 1 0 0 0 1-1v-4Z" />
    </svg>
  );
}

function StarIcon() {
  return (
    <svg
      width="26"
      height="26"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="shrink-0"
    >
      <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-2.9-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z" />
    </svg>
  );
}