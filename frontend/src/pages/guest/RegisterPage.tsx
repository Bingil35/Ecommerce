import {
  useState,
  type FormEvent,
  type ReactNode,
} from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';

import axiosClient from '../../services/api/axiosClient';

import {
  AppleIcon,
  EyeIcon,
  GoogleIcon,
  LockIcon,
  MailIcon,
  PhoneIcon,
  UserIcon,
} from '../../components/icons';

interface RegisterForm {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

export default function RegisterPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState<RegisterForm>({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  });

  const [agreeTerms, setAgreeTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] =
    useState(false);

  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (
    field: keyof RegisterForm,
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

    if (!form.fullName.trim()) {
      setErrorMessage('Vui lòng nhập họ và tên.');
      return;
    }

    if (!form.email.trim()) {
      setErrorMessage('Vui lòng nhập email.');
      return;
    }

    if (!form.password) {
      setErrorMessage('Vui lòng nhập mật khẩu.');
      return;
    }

    if (form.password !== form.confirmPassword) {
      setErrorMessage('Mật khẩu xác nhận không khớp.');
      return;
    }

    if (!agreeTerms) {
      setErrorMessage(
        'Vui lòng đồng ý với điều khoản sử dụng.'
      );
      return;
    }

    try {
      setIsLoading(true);

      await axiosClient.post('/auth/register', {
        fullName: form.fullName,
        email: form.email,
        phone: form.phone || null,
        password: form.password,
        confirmPassword: form.confirmPassword,
      });

      navigate('/login');
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        setErrorMessage(
          error.response?.data?.message ||
            'Đăng ký không thành công. Vui lòng thử lại.'
        );
      } else {
        setErrorMessage('Đã xảy ra lỗi. Vui lòng thử lại.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F9FC]">
      <div className="mx-auto flex min-h-screen max-w-7xl items-center px-6 py-10 lg:px-10">
        <div className="grid w-full overflow-hidden rounded-3xl bg-white shadow-[0_20px_60px_rgba(31,41,55,0.08)] lg:grid-cols-2">

          {/* ================= LEFT ================= */}
          <div className="relative hidden overflow-hidden bg-[#EEF4FF] p-12 lg:flex lg:flex-col lg:justify-between">

            <div>
              <Link
                to="/"
                className="text-3xl font-bold tracking-tight text-[#3157C7]"
              >
                Kala
              </Link>

              <p className="mt-3 max-w-md text-sm leading-6 text-[#667085]">
                Nền tảng mua sắm trực tuyến giúp bạn dễ dàng
                khám phá sản phẩm và tìm kiếm những lựa chọn
                phù hợp.
              </p>
            </div>

            <div className="mx-auto flex w-full max-w-md flex-col items-center py-10 text-center">

              <div className="flex h-64 w-64 items-center justify-center rounded-full bg-white/70">
                <span className="text-7xl text-[#4F7CFF]">
                  Kala
                </span>
              </div>

              <h2 className="mt-8 text-2xl font-semibold text-[#172033]">
                Bắt đầu hành trình mua sắm
              </h2>

              <p className="mt-3 max-w-sm text-sm leading-6 text-[#667085]">
                Tạo tài khoản Kala để khám phá sản phẩm,
                quản lý đơn hàng và tận hưởng trải nghiệm
                mua sắm thuận tiện hơn.
              </p>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <Benefit
                title="Đa dạng"
                description="Nhiều sản phẩm"
              />

              <Benefit
                title="Tiện lợi"
                description="Mua sắm dễ dàng"
              />

              <Benefit
                title="An tâm"
                description="Quản lý đơn hàng"
              />
            </div>
          </div>

          {/* ================= RIGHT ================= */}
          <div className="flex items-center justify-center px-6 py-10 sm:px-10 lg:px-14">
            <div className="w-full max-w-md">

              <div className="mb-7">
                <Link
                  to="/"
                  className="text-2xl font-bold text-[#3157C7] lg:hidden"
                >
                  Kala
                </Link>

                <h1 className="mt-5 text-3xl font-bold tracking-tight text-[#172033]">
                  Tạo tài khoản
                </h1>

                <p className="mt-2 text-sm text-[#667085]">
                  Đăng ký để bắt đầu sử dụng Kala
                </p>
              </div>

              {/* Error */}
              {errorMessage && (
                <div className="mb-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
                  {errorMessage}
                </div>
              )}

              <form
                onSubmit={handleSubmit}
                className="space-y-4"
              >

                {/* Full name */}
                <InputField
                  label="Họ và tên"
                  placeholder="Nhập họ và tên"
                  value={form.fullName}
                  icon={
                    <UserIcon className="h-5 w-5" />
                  }
                  onChange={(value) =>
                    handleChange('fullName', value)
                  }
                />

                {/* Email */}
                <InputField
                  label="Email"
                  type="email"
                  placeholder="example@email.com"
                  value={form.email}
                  icon={
                    <MailIcon className="h-5 w-5" />
                  }
                  onChange={(value) =>
                    handleChange('email', value)
                  }
                />

                {/* Phone */}
                <InputField
                  label="Số điện thoại"
                  type="tel"
                  placeholder="Nhập số điện thoại"
                  value={form.phone}
                  icon={
                    <PhoneIcon className="h-5 w-5" />
                  }
                  onChange={(value) =>
                    handleChange('phone', value)
                  }
                />

                {/* Password */}
                <PasswordField
                  label="Mật khẩu"
                  placeholder="Nhập mật khẩu"
                  value={form.password}
                  visible={showPassword}
                  onToggle={() =>
                    setShowPassword(
                      (previous) => !previous
                    )
                  }
                  onChange={(value) =>
                    handleChange('password', value)
                  }
                />

                {/* Confirm password */}
                <PasswordField
                  label="Xác nhận mật khẩu"
                  placeholder="Nhập lại mật khẩu"
                  value={form.confirmPassword}
                  visible={showConfirmPassword}
                  onToggle={() =>
                    setShowConfirmPassword(
                      (previous) => !previous
                    )
                  }
                  onChange={(value) =>
                    handleChange(
                      'confirmPassword',
                      value
                    )
                  }
                />

                {/* Terms */}
                <label className="flex cursor-pointer items-start gap-3 pt-1 text-sm text-[#667085]">
                  <input
                    type="checkbox"
                    checked={agreeTerms}
                    onChange={(event) =>
                      setAgreeTerms(
                        event.target.checked
                      )
                    }
                    className="mt-0.5 h-4 w-4 rounded border-gray-300 accent-[#4F7CFF]"
                  />

                  <span>
                    Tôi đồng ý với{' '}
                    <span className="font-medium text-[#3157C7]">
                      điều khoản sử dụng
                    </span>{' '}
                    của Kala.
                  </span>
                </label>

                {/* Register button */}
                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full rounded-xl bg-[#4F7CFF] px-5 py-3.5 text-sm font-semibold text-white transition hover:bg-[#3D63D8] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isLoading
                    ? 'Đang đăng ký...'
                    : 'Đăng ký tài khoản'}
                </button>
              </form>

              {/* Divider */}
              <div className="my-6 flex items-center gap-4">
                <div className="h-px flex-1 bg-[#E4E7EC]" />

                <span className="text-xs text-[#98A2B3]">
                  hoặc
                </span>

                <div className="h-px flex-1 bg-[#E4E7EC]" />
              </div>

              {/* Social buttons */}
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  className="flex h-11 items-center justify-center gap-2.5 rounded-xl border border-[#D0D5DD] bg-white text-sm font-medium text-[#344054] transition hover:bg-[#F9FAFB]"
                >
                  <GoogleIcon className="h-5 w-5" />
                  Google
                </button>

                <button
                  type="button"
                  className="flex h-11 items-center justify-center gap-2.5 rounded-xl border border-[#D0D5DD] bg-white text-sm font-medium text-[#344054] transition hover:bg-[#F9FAFB]"
                >
                  <AppleIcon className="h-5 w-5 text-[#172033]" />
                  Apple
                </button>
              </div>

              {/* Login */}
              <p className="mt-6 text-center text-sm text-[#667085]">
                Đã có tài khoản?{' '}
                <Link
                  to="/login"
                  className="font-semibold text-[#3157C7] hover:underline"
                >
                  Đăng nhập
                </Link>
              </p>

            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* =====================================================
   INPUT FIELD
===================================================== */

interface InputFieldProps {
  label: string;
  placeholder: string;
  value: string;
  type?: string;
  icon?: ReactNode;
  onChange: (value: string) => void;
}

function InputField({
  label,
  placeholder,
  value,
  type = 'text',
  icon,
  onChange,
}: InputFieldProps) {
  return (
    <div>
      <label className="mb-1.5 block text-[13px] font-medium text-[#344054]">
        {label}
      </label>

      <div className="relative">
        {icon && (
          <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[#52638F]">
            {icon}
          </span>
        )}

        <input
          type={type}
          value={value}
          placeholder={placeholder}
          onChange={(event) =>
            onChange(event.target.value)
          }
          className="h-11 w-full rounded-xl border border-[#D0D5DD] bg-white pl-11 pr-4 text-sm text-[#172033] outline-none transition placeholder:text-[#98A2B3] focus:border-[#4F7CFF] focus:ring-4 focus:ring-[#4F7CFF]/10"
        />
      </div>
    </div>
  );
}

/* =====================================================
   PASSWORD FIELD
===================================================== */

interface PasswordFieldProps {
  label: string;
  placeholder: string;
  value: string;
  visible: boolean;
  onToggle: () => void;
  onChange: (value: string) => void;
}

function PasswordField({
  label,
  placeholder,
  value,
  visible,
  onToggle,
  onChange,
}: PasswordFieldProps) {
  return (
    <div>
      <label className="mb-1.5 block text-[13px] font-medium text-[#344054]">
        {label}
      </label>

      <div className="relative">
        {/* Lock */}
        <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[#52638F]">
          <LockIcon className="h-5 w-5" />
        </span>

        {/* Input */}
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          placeholder={placeholder}
          onChange={(event) =>
            onChange(event.target.value)
          }
          className="h-11 w-full rounded-xl border border-[#D0D5DD] bg-white pl-11 pr-12 text-sm text-[#172033] outline-none transition placeholder:text-[#98A2B3] focus:border-[#4F7CFF] focus:ring-4 focus:ring-[#4F7CFF]/10"
        />

        {/* Eye */}
        <button
          type="button"
          onClick={onToggle}
          aria-label={
            visible
              ? 'Ẩn mật khẩu'
              : 'Hiện mật khẩu'
          }
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#667085] transition hover:text-[#3157C7]"
        >
          <EyeIcon
            className="h-5 w-5"
            visible={visible}
          />
        </button>
      </div>
    </div>
  );
}

/* =====================================================
   BENEFIT
===================================================== */

interface BenefitProps {
  title: string;
  description: string;
}

function Benefit({
  title,
  description,
}: BenefitProps) {
  return (
    <div className="rounded-2xl bg-white/70 p-4">
      <p className="text-sm font-semibold text-[#172033]">
        {title}
      </p>

      <p className="mt-1 text-xs leading-5 text-[#667085]">
        {description}
      </p>
    </div>
  );
}