import { useEffect, useState } from 'react';
import { Check, ChevronRight, Ticket, X } from 'lucide-react';
import type { Coupon } from '../../../services/checkout/checkoutTypes';
import { formatMoney } from '../../../services/checkout/checkoutApi';

interface CheckoutVoucherBoxProps {
  title: string;
  label: string;
  coupons: Coupon[];
  selectedCode: string;
  selectedSubtotal: number;
  discount: number;
  onChange: (code: string) => void;
}

const dateLabel = (value: string | null) => value ? new Date(value).toLocaleDateString('vi-VN') : '';

export default function CheckoutVoucherBox({ title, label, coupons, selectedCode, selectedSubtotal, discount, onChange }: CheckoutVoucherBoxProps) {
  const [open, setOpen] = useState(false);
  const [draftCode, setDraftCode] = useState('');
  const [inputCode, setInputCode] = useState('');
  const [codeError, setCodeError] = useState('');

  useEffect(() => {
    if (open) {
      setDraftCode(selectedCode);
      setInputCode('');
      setCodeError('');
    }
  }, [open, selectedCode]);

  const eligible = (coupon: Coupon) => selectedSubtotal > 0 && selectedSubtotal >= Number(coupon.minimumOrder ?? 0);
  const formatValue = (coupon: Coupon) => coupon.discountType === 'PERCENTAGE'
    ? `${coupon.discountValue}%${coupon.maximumDiscount ? ` · tối đa ${formatMoney(coupon.maximumDiscount)}` : ''}`
    : formatMoney(coupon.discountValue);

  const applyCode = () => {
    const coupon = coupons.find(item => item.code.toLocaleLowerCase() === inputCode.trim().toLocaleLowerCase());
    if (!coupon) {
      setCodeError('Mã voucher không hợp lệ hoặc không áp dụng cho đơn hàng này.');
      return;
    }
    if (!eligible(coupon)) {
      setCodeError(`Đơn hàng cần đạt tối thiểu ${formatMoney(coupon.minimumOrder)} để dùng voucher này.`);
      return;
    }
    setDraftCode(coupon.code);
    setCodeError('');
  };

  const selected = coupons.find(coupon => coupon.code === selectedCode);
  const selectedEligible = selected ? eligible(selected) : false;

  return <>
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 rounded-xl border border-dashed border-[#D9E0F2] bg-[#FBFCFF] px-3 py-3">
      <Ticket size={18} strokeWidth={1.8} className="text-[#F97316]" />
      <span className="text-sm font-medium text-[#252A3A]">{label}</span>
      {selected && selectedEligible ? <div className="ml-auto flex items-center gap-2">
        <span className="rounded-md border border-[#F97316]/40 bg-[#FFF7ED] px-2 py-0.5 text-xs font-semibold text-[#EA580C]">
          {selected.code}{discount > 0 ? ` · −${formatMoney(discount)}` : ''}
        </span>
        <button type="button" onClick={() => setOpen(true)} className="text-xs font-medium text-[#5367C9] hover:underline">Đổi</button>
        <button type="button" onClick={() => onChange('')} aria-label="Bỏ voucher" className="flex h-6 w-6 items-center justify-center rounded-full text-[#98A1B2] hover:bg-[#FFF5F5] hover:text-[#DC5A5A]"><X size={14} /></button>
      </div> : <button type="button" onClick={() => setOpen(true)} className="ml-auto inline-flex items-center gap-1 text-sm font-medium text-[#5367C9] hover:underline">
        Chọn voucher <ChevronRight size={15} />
      </button>}
      {selected && !selectedEligible && <p className="w-full text-xs text-amber-700">Đơn hàng hiện chưa đủ điều kiện voucher này.</p>}
    </div>

    {open && <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4" onClick={() => setOpen(false)}>
      <div role="dialog" aria-modal="true" aria-label={title} onClick={event => event.stopPropagation()} className="flex max-h-[85vh] w-full max-w-md flex-col rounded-t-2xl bg-white shadow-xl sm:rounded-2xl">
        <div className="flex items-center justify-between border-b border-[#E8EDF5] px-5 py-4">
          <h3 className="text-base font-bold text-[#202642]">{title}</h3>
          <button type="button" onClick={() => setOpen(false)} aria-label="Đóng" className="flex h-8 w-8 items-center justify-center rounded-full text-[#98A1B2] hover:bg-[#F3F6FC]"><X size={18} /></button>
        </div>
        <div className="overflow-y-auto px-5 py-4">
          <div className="flex gap-2">
            <input value={inputCode} onChange={event => { setInputCode(event.target.value); setCodeError(''); }} onKeyDown={event => event.key === 'Enter' && applyCode()} placeholder="Nhập mã voucher" className="h-10 min-w-0 flex-1 rounded-lg border border-[#E8EDF5] px-3 text-sm uppercase placeholder:normal-case focus:border-[#5367C9] focus:outline-none focus:ring-2 focus:ring-[#5367C9]/20" />
            <button type="button" onClick={applyCode} disabled={!inputCode.trim()} className="h-10 rounded-lg bg-[#5367C9] px-4 text-sm font-semibold text-white hover:bg-[#4659B8] disabled:cursor-not-allowed disabled:opacity-40">Áp dụng</button>
          </div>
          {codeError && <p role="alert" className="mt-2 text-xs text-[#DC5A5A]">{codeError}</p>}
          <div className="mt-4 space-y-3">
            {coupons.length === 0 ? <p className="py-6 text-center text-sm text-[#667085]">Hiện chưa có voucher khả dụng.</p> : coupons.map(coupon => {
              const canUse = eligible(coupon);
              const active = draftCode === coupon.code;
              return <button key={coupon.id} type="button" disabled={!canUse} onClick={() => setDraftCode(active ? '' : coupon.code)} className={`flex w-full items-start gap-3 rounded-xl border p-3 text-left transition ${active ? 'border-[#5367C9] bg-[#F4F6FF]' : 'border-[#E8EDF5] bg-white hover:border-[#5367C9]/50'} ${!canUse ? 'cursor-not-allowed opacity-55' : ''}`}>
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#FFF7ED] text-[#F97316]"><Ticket size={20} /></span>
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-semibold text-[#252A3A]">{coupon.code} · {formatValue(coupon)}</span>
                  <span className="mt-0.5 block text-xs text-[#667085]">Đơn tối thiểu {formatMoney(coupon.minimumOrder)}{coupon.endAt ? ` · HSD ${dateLabel(coupon.endAt)}` : ''}</span>
                  {!canUse && <span className="mt-1 block text-xs text-amber-700">{selectedSubtotal <= 0 ? 'Chọn sản phẩm để dùng voucher.' : `Mua thêm ${formatMoney(Number(coupon.minimumOrder ?? 0) - selectedSubtotal)} để đủ điều kiện.`}</span>}
                </span>
                {active && <Check size={18} className="mt-1 shrink-0 text-[#5367C9]" />}
              </button>;
            })}
          </div>
        </div>
        <div className="flex gap-3 border-t border-[#E8EDF5] px-5 py-4">
          <button type="button" onClick={() => setOpen(false)} className="h-11 flex-1 rounded-xl border border-[#E8EDF5] text-sm font-semibold text-[#667085] hover:bg-[#F8FAFF]">Trở lại</button>
          <button type="button" disabled={Boolean(draftCode && !coupons.some(coupon => coupon.code === draftCode && eligible(coupon)))} onClick={() => { onChange(draftCode); setOpen(false); }} className="h-11 flex-1 rounded-xl bg-[#5367C9] text-sm font-semibold text-white hover:bg-[#4659B8] disabled:cursor-not-allowed disabled:opacity-40">Đồng ý</button>
        </div>
      </div>
    </div>}
  </>;
}
