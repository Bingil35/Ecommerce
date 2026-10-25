import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  CircleAlert,
  Heart,
  RotateCcw,
  SearchX,
} from 'lucide-react';

export const WishlistSkeleton = ({ count = 12 }: { count?: number }) => (
  <div
    aria-busy="true"
    aria-label="Đang tải danh sách yêu thích"
    className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6"
  >
    {Array.from({ length: count }, (_, index) => (
      <div
        key={index}
        className="animate-pulse rounded-xl border border-[#E8EDF5] bg-white p-1.5"
      >
        <div className="aspect-[4/3] rounded-lg bg-[#EDF1F8]" />
        <div className="space-y-2 px-1.5 pb-1.5 pt-3">
          <div className="h-3.5 w-11/12 rounded bg-[#EDF1F8]" />
          <div className="h-3.5 w-2/3 rounded bg-[#EDF1F8]" />
          <div className="h-3 w-1/2 rounded bg-[#EDF1F8]" />
          <div className="h-5 w-3/5 rounded bg-[#EDF1F8]" />
          <div className="h-9 rounded-lg bg-[#EDF1F8]" />
        </div>
      </div>
    ))}
  </div>
);

export const EmptyWishlist = () => (
  <div className="flex min-h-[420px] flex-col items-center justify-center rounded-2xl border border-[#E8EDF5] bg-white px-6 py-14 text-center shadow-[0_4px_18px_rgba(72,93,140,0.05)]">
    <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-[#FFF0F3]">
      <Heart size={46} strokeWidth={1.5} className="text-[#E5334F]" />
    </div>
    <h2 className="mb-3 text-2xl font-bold text-[#202642]">
      Danh sách yêu thích đang trống
    </h2>
    <p className="mb-8 max-w-md text-sm leading-6 text-[#667085]">
      Hãy khám phá các sản phẩm trên Kala và nhấn vào biểu tượng trái tim để lưu lại
      những món bạn thích, bạn có thể xem lại bất cứ lúc nào.
    </p>
    <Link
      to="/products"
      className="inline-flex items-center gap-2 rounded-full bg-[#5367C9] px-6 py-3 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(83,103,201,0.25)] transition hover:-translate-y-0.5 hover:bg-[#4659B8]"
    >
      Khám phá sản phẩm
      <ArrowRight size={17} strokeWidth={2} />
    </Link>
  </div>
);

export const NoWishlistResults = ({ onReset }: { onReset: () => void }) => (
  <div className="flex flex-col items-center rounded-2xl border border-dashed border-[#DCE5F5] bg-white px-6 py-14 text-center">
    <SearchX size={40} strokeWidth={1.5} className="mb-3 text-[#A4AEC2]" />
    <h2 className="font-semibold text-[#202642]">Không tìm thấy sản phẩm phù hợp.</h2>
    <p className="mt-1 text-sm text-[#8A93A6]">
      Hãy thử đổi từ khóa hoặc bỏ bớt bộ lọc.
    </p>
    <button
      type="button"
      onClick={onReset}
      className="mt-5 inline-flex items-center gap-2 rounded-full border border-[#DCE4F2] px-5 py-2.5 text-sm font-semibold text-[#5367C9] hover:bg-[#F4F7FF]"
    >
      <RotateCcw size={15} />
      Xóa tìm kiếm và bộ lọc
    </button>
  </div>
);

export const WishlistError = ({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) => (
  <div
    role="alert"
    className="flex flex-col items-center rounded-2xl border border-[#E8EDF5] bg-white px-6 py-16 text-center"
  >
    <CircleAlert size={40} strokeWidth={1.5} className="mb-3 text-[#DC5A5A]" />
    <p className="mb-4 text-sm text-[#DC5A5A]">{message}</p>
    <button
      type="button"
      onClick={onRetry}
      className="rounded-full bg-[#5367C9] px-6 py-2.5 text-sm font-semibold text-white hover:bg-[#4659B8]"
    >
      Thử lại
    </button>
  </div>
);

/* ---------------- Hộp thoại xác nhận ---------------- */

interface ConfirmDialogProps {
  title: string;
  description: string;
  confirmLabel: string;
  isBusy: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmDialog = ({
  title,
  description,
  confirmLabel,
  isBusy,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) => {
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    cancelRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isBusy) onCancel();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [isBusy, onCancel]);

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-[#1F2540]/40 px-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isBusy) onCancel();
      }}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-desc"
        className="w-full max-w-[400px] rounded-2xl bg-white p-6 shadow-[0_24px_60px_rgba(31,37,64,0.25)]"
      >
        <h2 id="confirm-title" className="text-lg font-bold text-[#202642]">
          {title}
        </h2>
        <p id="confirm-desc" className="mt-2 text-sm leading-6 text-[#667085]">
          {description}
        </p>
        <div className="mt-6 flex justify-end gap-3">
          <button
            ref={cancelRef}
            type="button"
            disabled={isBusy}
            onClick={onCancel}
            className="rounded-full border border-[#DCE4F2] px-5 py-2.5 text-sm font-semibold text-[#4D5872] hover:bg-[#F7F9FD] disabled:opacity-60"
          >
            Hủy
          </button>
          <button
            type="button"
            disabled={isBusy}
            onClick={onConfirm}
            className="rounded-full bg-[#E5334F] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[#CF2A45] disabled:cursor-wait disabled:opacity-70"
          >
            {isBusy ? 'Đang xóa...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
