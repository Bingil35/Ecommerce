import { RefreshCw, ShieldCheck, Truck } from 'lucide-react';

// Nội dung lấy theo ảnh thiết kế; cần bộ phận kinh doanh xác nhận lại các điều kiện (300.000đ, 7 ngày).
const BENEFITS = [
  {
    icon: Truck,
    title: 'Miễn phí giao hàng',
    description: 'Cho đơn hàng từ 300.000đ',
  },
  {
    icon: RefreshCw,
    title: 'Đổi trả dễ dàng',
    description: 'Trong vòng 7 ngày',
  },
  {
    icon: ShieldCheck,
    title: 'Thanh toán an toàn',
    description: 'Bảo mật thông tin tuyệt đối',
  },
] as const;

const WishlistBenefits = () => (
  <section
    aria-label="Quyền lợi khi mua sắm"
    className="grid divide-y divide-[#E8EDF5] rounded-2xl border border-[#E1E7F2] bg-white py-2 shadow-[0_2px_10px_rgba(72,93,140,0.04)] sm:grid-cols-3 sm:divide-x sm:divide-y-0 sm:py-4"
  >
    {BENEFITS.map(({ icon: Icon, title, description }) => (
      <div key={title} className="flex items-center gap-4 px-6 py-3 sm:justify-center">
        <Icon size={34} strokeWidth={1.4} className="shrink-0 text-[#5B6FF5]" />
        <div className="min-w-0">
          <p className="text-sm font-semibold text-[#202642]">{title}</p>
          <p className="mt-0.5 text-xs text-[#7B8498]">{description}</p>
        </div>
      </div>
    ))}
  </section>
);

export default WishlistBenefits;
