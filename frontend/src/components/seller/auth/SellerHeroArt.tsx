import { Check, Package, ShoppingBag, Store, TrendingUp } from 'lucide-react';

const BARS = [28, 42, 36, 58, 50, 76, 92];

/**
 * Minh họa dashboard người bán, vẽ bằng Tailwind + lucide nên luôn nét.
 * Có ảnh minh họa riêng thì thay cả component này bằng một thẻ <img>.
 */
export default function SellerHeroArt() {
  return (
    <div
      className="relative mx-auto h-[260px] w-full max-w-[520px]"
      aria-hidden="true"
    >
      <div className="absolute bottom-2 left-4 right-4 h-9 rounded-[50%] bg-[#C4D6FA]/60 blur-md" />

      {/* Biểu đồ */}
      <div className="absolute bottom-8 left-[17%] h-[150px] w-[54%] rounded-2xl bg-white/85 p-4 shadow-[0_14px_32px_rgba(59,123,246,0.16)]">
        <div className="relative flex h-full items-end gap-2.5">
          {BARS.map((height, index) => (
            <div
              key={index}
              className="flex-1 rounded-t-md bg-gradient-to-t from-[#3B7BF6] to-[#A6C5FF]"
              style={{ height: `${height}%` }}
            />
          ))}

          <svg
            className="absolute inset-0 h-full w-full"
            viewBox="0 0 100 100"
            preserveAspectRatio="none"
          >
            <polyline
              points="6,72 20,58 34,66 48,44 62,50 77,24 93,10"
              fill="none"
              stroke="#2F6BEA"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              vectorEffect="non-scaling-stroke"
            />
          </svg>
        </div>
      </div>

      {/* Doanh thu hôm nay */}
      <div className="absolute left-0 top-1 rounded-xl bg-white px-4 py-3 shadow-[0_10px_26px_rgba(59,123,246,0.15)]">
        <p className="text-[11px] text-[#6B7AA3]">Doanh thu hôm nay</p>
        <p className="mt-0.5 flex items-baseline gap-2">
          <span className="text-lg font-bold text-[#1F1B6B]">12.560.000đ</span>
          <span className="flex items-center gap-0.5 text-xs font-semibold text-[#16A34A]">
            <TrendingUp className="h-3.5 w-3.5" strokeWidth={2.2} />
            12%
          </span>
        </p>
      </div>

      {/* Cửa hàng */}
      <div className="absolute left-[50%] top-0 grid h-14 w-14 place-items-center rounded-xl bg-white shadow-[0_10px_26px_rgba(59,123,246,0.15)]">
        <Store className="h-7 w-7 text-[#3B7BF6]" strokeWidth={1.8} />
      </div>

      {/* Điện thoại */}
      <div className="absolute bottom-5 right-[9%] flex h-[190px] w-[98px] flex-col items-center rounded-[22px] border-[5px] border-[#1E2A6B] bg-white px-2 pt-6 shadow-[0_16px_34px_rgba(30,42,107,0.25)]">
        <span className="text-base font-extrabold text-[#1F1B6B]">Kala</span>
        <span className="mt-3 grid h-14 w-14 place-items-center rounded-2xl bg-[#E6EFFF]">
          <Store className="h-8 w-8 text-[#3B7BF6]" strokeWidth={1.8} />
        </span>
        <span className="mt-3 h-1.5 w-12 rounded-full bg-[#DCE6F8]" />
        <span className="mt-1.5 h-1.5 w-8 rounded-full bg-[#DCE6F8]" />
      </div>

      <span className="absolute right-[17%] top-2 grid h-9 w-9 place-items-center rounded-full bg-[#3B7BF6] text-white shadow-[0_8px_18px_rgba(59,123,246,0.35)]">
        <Check className="h-5 w-5" strokeWidth={3} />
      </span>

      {/* Túi mua sắm */}
      <span className="absolute bottom-6 right-0 grid h-[72px] w-[58px] rotate-3 place-items-center rounded-xl bg-[#3B7BF6] text-white shadow-[0_12px_26px_rgba(59,123,246,0.35)]">
        <ShoppingBag className="h-8 w-8" strokeWidth={1.7} />
      </span>

      {/* Thùng hàng */}
      <span className="absolute bottom-4 left-[2%] grid h-16 w-16 place-items-center rounded-lg bg-[#E8BE86] text-[#8A5A24] shadow-md">
        <Package className="h-9 w-9" strokeWidth={1.6} />
      </span>
      <span className="absolute bottom-3 left-[14%] grid h-12 w-12 place-items-center rounded-lg bg-[#F0CC9A] text-[#8A5A24] shadow-md">
        <Package className="h-7 w-7" strokeWidth={1.6} />
      </span>
    </div>
  );
}
