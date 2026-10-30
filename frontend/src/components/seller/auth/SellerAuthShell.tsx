import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import {
  ClipboardList,
  Headset,
  ShieldCheck,
  TrendingUp,
  type LucideIcon,
} from 'lucide-react';

import logo from '../../../assets/logo.png';
import SellerHeroArt from './SellerHeroArt';

interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

const FEATURES: Feature[] = [
  {
    icon: ClipboardList,
    title: 'Quản lý',
    description: 'đơn hàng hiệu quả',
  },
  {
    icon: TrendingUp,
    title: 'Theo dõi doanh thu',
    description: 'thời gian thực',
  },
  {
    icon: ShieldCheck,
    title: 'Bảo mật',
    description: 'tài khoản',
  },
  {
    icon: Headset,
    title: 'Hỗ trợ 24/7',
    description: 'từ đội ngũ Kala',
  },
];

function SellerBrand({
  logoClassName,
  pillClassName,
}: {
  logoClassName: string;
  pillClassName: string;
}) {
  return (
    <Link to="/seller/login" className="flex w-fit items-center">
      <img
        src={logo}
        alt="Kala"
        className={`${logoClassName} w-auto object-contain`}
      />
      <span
        className={`${pillClassName} rounded-full bg-[#3B7BF6] px-4 py-1.5 text-sm font-semibold text-white`}
      >
        Seller
      </span>
    </Link>
  );
}

interface SellerAuthShellProps {
  children: ReactNode;
}

/**
 * Khung hai cột dùng chung cho các trang xác thực của người bán:
 * trái là giới thiệu Kala Seller, phải là nội dung (form) truyền vào.
 */
export default function SellerAuthShell({ children }: SellerAuthShellProps) {
  return (
    <div className="min-h-screen bg-[#F4F8FF] px-4 py-6 sm:px-6 lg:px-10">
      <div className="mx-auto flex min-h-[calc(100vh-3rem)] max-w-6xl overflow-hidden rounded-[28px] bg-white shadow-[0_20px_60px_rgba(31,41,55,0.08)]">
        <section className="hidden w-1/2 bg-gradient-to-b from-[#E9F1FF] to-[#DCE9FF] lg:flex">
          <div className="flex w-full flex-col px-12 py-10 xl:px-14">
            <SellerBrand logoClassName="h-20" pillClassName="-ml-3" />

            <div className="mt-8">
              <h1 className="max-w-md text-[34px] font-bold leading-[1.2] tracking-tight text-[#1F1B6B]">
                Đồng hành cùng
                <br />
                chủ cửa hàng trên
                <br />
                hành trình kinh doanh!
              </h1>

              <p className="mt-4 max-w-md text-[15px] leading-6 text-[#4E5E8C]">
                Kala Seller giúp bạn quản lý gian hàng, theo dõi đơn hàng, tối
                ưu doanh thu và phát triển thương hiệu một cách dễ dàng.
              </p>
            </div>

            <div className="flex flex-1 items-center py-6">
              <SellerHeroArt />
            </div>

            <ul className="grid grid-cols-4">
              {FEATURES.map(({ icon: Icon, title, description }) => (
                <li
                  key={title}
                  className="flex flex-col items-center gap-1.5 border-r border-[#C9D8F2] px-2 text-center last:border-r-0"
                >
                  <Icon
                    className="h-6 w-6 text-[#3B6FD8]"
                    strokeWidth={1.7}
                  />
                  <span className="text-xs font-medium text-[#34457A]">
                    {title}
                  </span>
                  <span className="text-xs text-[#6B7AA3]">{description}</span>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="flex w-full items-center justify-center px-6 py-8 sm:px-10 lg:w-1/2 lg:px-12 xl:px-14">
          <div className="w-full max-w-[460px]">
            <div className="mb-8 lg:hidden">
              <SellerBrand logoClassName="h-12" pillClassName="ml-0" />
            </div>

            {children}
          </div>
        </section>
      </div>
    </div>
  );
}
