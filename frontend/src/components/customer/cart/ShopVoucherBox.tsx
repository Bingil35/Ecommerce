import { useEffect, useState } from 'react';
import { Check, ChevronRight, Ticket, X } from 'lucide-react';

import { formatVnd } from '../../../services/catalog/productApi';
import { IS_VOUCHER_READY, voucherApi } from '../../../services/voucher/voucherApi';
import type { ShopVoucher } from '../../../services/voucher/voucherTypes';

interface ShopVoucherBoxProps {
  /** id của shop; null = voucher tổng (cả đơn) */
  shopId: string | null;
  label?: string;
  title?: string;
  /** class của khung ngoài (mặc định: dòng voucher trong khối shop) */
  className?: string;
  /** Tổng tiền dùng để xét điều kiện (shop: sản phẩm đã chọn; tổng: sau khi trừ voucher shop) */
  selectedSubtotal: number;
  voucher: ShopVoucher | null;
  /** Số tiền thực sự được giảm */
  discount: number;
  /** Số tiền còn thiếu để voucher đủ điều kiện */
  missingForVoucher: number;
  onChange: (voucher: ShopVoucher | null) => void;
}

const formatDate = (iso?: string | null) =>
  iso ? new Date(iso).toLocaleDateString('vi-VN') : '';

const DEFAULT_ROW_CLASS =
  'flex flex-wrap items-center gap-x-3 gap-y-1 border-t border-dashed border-[#E8EDF5] px-4 py-3 sm:px-5';

const ShopVoucherBox = ({
  shopId,
  label = 'Voucher của shop',
  title = 'Chọn voucher của shop',
  className = DEFAULT_ROW_CLASS,
  selectedSubtotal,
  voucher,
  discount,
  missingForVoucher,
  onChange,
}: ShopVoucherBoxProps) => {
  const [open, setOpen] = useState(false);
  const [vouchers, setVouchers] = useState<ShopVoucher[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [draft, setDraft] = useState<ShopVoucher | null>(null);
  const [code, setCode] = useState('');
  const [codeError, setCodeError] = useState<string | null>(null);

  // Mở modal -> tải danh sách voucher của shop
  useEffect(() => {
    if (!open) return;

    let cancelled = false;
    setIsLoading(true);
    const request =
      shopId === null
        ? voucherApi.getOrderVouchers()
        : voucherApi.getShopVouchers(shopId);

    request
      .then((data) => {
        if (!cancelled) setVouchers(data);
      })
      .catch(() => {
        if (!cancelled) setVouchers([]);
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, shopId]);

  const openModal = () => {
    if (!IS_VOUCHER_READY) {
      window.alert(
        shopId === null
          ? 'Làm voucher tổng ở scrum khác'
          : 'Làm voucher shop ở scrum khác'
      );
      return;
    }
    setDraft(voucher);
    setCode('');
    setCodeError(null);
    setOpen(true);
  };

  const selectHint =
    shopId === null
      ? 'Hãy tích chọn sản phẩm trong giỏ hàng trước.'
      : 'Hãy tích chọn sản phẩm của shop trước.';

  const isEligible = (v: ShopVoucher) =>
    selectedSubtotal > 0 && selectedSubtotal >= v.minOrderValue;

  const handleApplyCode = () => {
    const input = code.trim().toUpperCase();
    if (!input) return;

    const found = vouchers.find((v) => v.code.toUpperCase() === input);
    if (!found) {
      setCodeError('Mã voucher không hợp lệ hoặc không thuộc shop này.');
      return;
    }
    if (!isEligible(found)) {
      setCodeError(
        selectedSubtotal === 0
          ? selectHint
          : `Cần mua thêm ${formatVnd(found.minOrderValue - selectedSubtotal)} để dùng voucher này.`
      );
      return;
    }
    setCodeError(null);
    setDraft(found);
  };

  const handleConfirm = () => {
    onChange(draft);
    setOpen(false);
  };

  return (
    <>
      <div className={className}>
        <Ticket size={18} strokeWidth={1.8} className="text-[#F97316]" />
        <span className="text-sm font-medium text-[#252A3A]">{label}</span>

        {voucher ? (
          <div className="ml-auto flex flex-wrap items-center gap-2">
            <span className="rounded-md border border-[#F97316]/40 bg-[#FFF7ED] px-2 py-0.5 text-xs font-semibold text-[#EA580C]">
              {voucher.code}
              {discount > 0 ? ` · -${formatVnd(discount)}` : ''}
            </span>
            <button
              type="button"
              onClick={openModal}
              className="text-xs font-medium text-[#5367C9] hover:underline"
            >
              Đổi
            </button>
            <button
              type="button"
              onClick={() => onChange(null)}
              aria-label="Bỏ voucher"
              className="flex h-6 w-6 items-center justify-center rounded-full text-[#98A1B2] hover:bg-[#FFF5F5] hover:text-[#DC5A5A]"
            >
              <X size={14} />
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={openModal}
            className="ml-auto inline-flex items-center gap-1 text-sm font-medium text-[#5367C9] hover:underline"
          >
            Chọn voucher
            <ChevronRight size={15} />
          </button>
        )}

        {voucher && missingForVoucher > 0 && (
          <p className="w-full text-xs text-[#F59E0B]">
            Mua thêm {formatVnd(missingForVoucher)} để được giảm (đơn tối thiểu{' '}
            {formatVnd(voucher.minOrderValue)}).
          </p>
        )}
        {voucher && selectedSubtotal === 0 && (
          <p className="w-full text-xs text-[#F59E0B]">
            Tích chọn sản phẩm để áp dụng voucher.
          </p>
        )}
      </div>

      {open && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4"
          onClick={() => setOpen(false)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={title}
            onClick={(e) => e.stopPropagation()}
            className="flex max-h-[85vh] w-full max-w-md flex-col rounded-t-2xl bg-white shadow-xl sm:rounded-2xl"
          >
            <div className="flex items-center justify-between border-b border-[#E8EDF5] px-5 py-4">
              <h3 className="text-base font-bold text-[#202642]">{title}</h3>
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Đóng"
                className="flex h-8 w-8 items-center justify-center rounded-full text-[#98A1B2] hover:bg-[#F3F6FC]"
              >
                <X size={18} />
              </button>
            </div>

            <div className="overflow-y-auto px-5 py-4">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={code}
                  onChange={(e) => {
                    setCode(e.target.value);
                    setCodeError(null);
                  }}
                  onKeyDown={(e) => e.key === 'Enter' && handleApplyCode()}
                  placeholder="Nhập mã voucher"
                  className="h-10 min-w-0 flex-1 rounded-lg border border-[#E8EDF5] px-3 text-sm uppercase placeholder:normal-case focus:border-[#5367C9] focus:outline-none focus:ring-2 focus:ring-[#5367C9]/20"
                />
                <button
                  type="button"
                  onClick={handleApplyCode}
                  disabled={!code.trim()}
                  className="h-10 rounded-lg bg-[#5367C9] px-4 text-sm font-semibold text-white hover:bg-[#4659B8] disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Áp dụng
                </button>
              </div>
              {codeError && (
                <p className="mt-2 text-xs text-[#DC5A5A]">{codeError}</p>
              )}

              <div className="mt-4 space-y-3">
                {isLoading ? (
                  <p className="py-6 text-center text-sm text-[#667085]">
                    Đang tải voucher...
                  </p>
                ) : vouchers.length === 0 ? (
                  <p className="py-6 text-center text-sm text-[#667085]">
                    Shop chưa có voucher khả dụng.
                  </p>
                ) : (
                  vouchers.map((v) => {
                    const eligible = isEligible(v);
                    const isSelected = draft?.id === v.id;

                    return (
                      <button
                        key={v.id}
                        type="button"
                        disabled={!eligible}
                        onClick={() => setDraft(isSelected ? null : v)}
                        className={`flex w-full items-start gap-3 rounded-xl border p-3 text-left transition ${
                          isSelected
                            ? 'border-[#5367C9] bg-[#F4F6FF]'
                            : 'border-[#E8EDF5] bg-white hover:border-[#5367C9]/50'
                        } ${!eligible ? 'cursor-not-allowed opacity-55' : ''}`}
                      >
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#FFF7ED] text-[#F97316]">
                          <Ticket size={20} />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold text-[#252A3A]">
                            {v.title}
                          </p>
                          <p className="mt-0.5 text-xs text-[#667085]">
                            Mã {v.code} · Đơn tối thiểu{' '}
                            {formatVnd(v.minOrderValue)}
                            {v.expiresAt ? ` · HSD ${formatDate(v.expiresAt)}` : ''}
                          </p>
                          {!eligible && (
                            <p className="mt-1 text-xs text-[#F59E0B]">
                              {selectedSubtotal === 0
                                ? selectHint
                                : `Mua thêm ${formatVnd(v.minOrderValue - selectedSubtotal)} để dùng voucher.`}
                            </p>
                          )}
                        </div>
                        {isSelected && (
                          <Check size={18} className="mt-1 shrink-0 text-[#5367C9]" />
                        )}
                      </button>
                    );
                  })
                )}
              </div>

              {/* TODO: voucher shop thật làm ở SCRUM khác — hiện đang dùng dữ liệu mẫu */}
              <p className="mt-4 rounded-lg bg-[#F8FAFF] px-3 py-2 text-xs text-[#667085]">
                Ghi chú: chức năng voucher làm ở SCRUM khác. Danh sách voucher
                hiện là dữ liệu mẫu để kiểm tra việc tính lại giá.
              </p>
            </div>

            <div className="flex gap-3 border-t border-[#E8EDF5] px-5 py-4">
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="h-11 flex-1 rounded-xl border border-[#E8EDF5] text-sm font-semibold text-[#667085] hover:bg-[#F8FAFF]"
              >
                Trở lại
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                className="h-11 flex-1 rounded-xl bg-[#5367C9] text-sm font-semibold text-white hover:bg-[#4659B8]"
              >
                Đồng ý
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ShopVoucherBox;