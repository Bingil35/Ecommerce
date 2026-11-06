import { useEffect, useState } from 'react';
import { ArrowLeft, TicketPercent } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { checkoutApi, formatMoney } from '../../services/checkout/checkoutApi';
import type { Coupon } from '../../services/checkout/checkoutTypes';

export default function VoucherDetailPage() {
  const { couponId } = useParams();
  const [coupon, setCoupon] = useState<Coupon | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => { let active = true; void checkoutApi.getCoupons().then(items => { if (active) setCoupon(items.find(item => item.id === couponId) ?? null); }).catch(() => { if (active) setCoupon(null); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [couponId]);

  return <section className="rounded-2xl border border-[#E6EAF3] bg-white p-5 shadow-sm sm:p-7">
    <Link to="/account/notifications" className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-[var(--kala-primary)]"><ArrowLeft size={16}/> Quay lại thông báo</Link>
    {loading ? <p className="py-12 text-center text-sm text-slate-500">Đang tải voucher...</p> : !coupon ? <p className="py-12 text-center text-sm text-slate-600">Voucher không còn khả dụng.</p> : <article className="mx-auto mt-7 max-w-lg rounded-2xl border border-dashed border-[var(--kala-primary)] p-6 text-center">
      <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-[var(--kala-soft)] text-[var(--kala-primary)]"><TicketPercent size={26}/></span>
      <p className="mt-4 text-xs font-bold uppercase tracking-wider text-[var(--kala-primary)]">Voucher {coupon.scope === 'PLATFORM' ? 'toàn sàn' : 'cửa hàng'}</p>
      <h1 className="mt-2 text-2xl font-extrabold text-[#252B40]">{coupon.code}</h1>
      <p className="mt-2 text-lg font-semibold text-slate-700">Giảm {coupon.discountType === 'PERCENTAGE' ? `${coupon.discountValue}%` : formatMoney(coupon.discountValue)}</p>
      {coupon.minimumOrder != null && Number(coupon.minimumOrder) > 0 && <p className="mt-2 text-sm text-slate-500">Đơn tối thiểu {formatMoney(coupon.minimumOrder)}</p>}
      {coupon.maximumDiscount != null && Number(coupon.maximumDiscount) > 0 && <p className="mt-1 text-sm text-slate-500">Giảm tối đa {formatMoney(coupon.maximumDiscount)}</p>}
      {coupon.endAt && <p className="mt-3 text-xs text-slate-400">Hạn sử dụng: {new Date(coupon.endAt).toLocaleDateString('vi-VN')}</p>}
      <Link to="/checkout" className="mt-6 inline-flex rounded-xl bg-[var(--kala-primary)] px-5 py-2.5 text-sm font-semibold text-white hover:opacity-90">Dùng voucher</Link>
    </article>}
  </section>;
}
