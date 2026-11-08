import { ChevronLeft, ChevronRight } from 'lucide-react';

type Props = {
  /** Trang hiện tại, bắt đầu từ 0 (khớp với backend). */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPage: (page: number) => void;
  onSize?: (size: number) => void;
  sizes?: number[];
  unit?: string;
  disabled?: boolean;
};

/**
 * Luôn trả về đúng tối đa SLOTS phần tử (số trang hoặc dấu …) để chiều rộng
 * thanh phân trang không đổi khi chuyển trang.
 */
const SLOTS = 7;
function pageSlots(page: number, pages: number): (number | '…')[] {
  if (pages <= SLOTS) return Array.from({ length: pages }, (_, i) => i);
  const last = pages - 1;
  if (page <= 3) return [0, 1, 2, 3, 4, '…', last];
  if (page >= last - 3) return [0, '…', last - 4, last - 3, last - 2, last - 1, last];
  return [0, '…', page - 1, page, page + 1, '…', last];
}

export default function SellerPagination({ page, size, totalElements, totalPages, onPage, onSize, sizes = [10, 20, 50], unit = 'biến thể', disabled }: Props) {
  if (totalElements === 0) return null;
  const pages = Math.max(1, totalPages);
  const slots = pageSlots(page, pages);
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  const btn = 'grid h-9 w-9 shrink-0 place-items-center rounded-lg border text-sm tabular-nums';

  return <div className="flex flex-col gap-3 border-t border-slate-100 px-4 py-3 text-sm text-slate-600">
    <div className="flex items-center justify-between gap-3">
      <span className="whitespace-nowrap tabular-nums">Hiển thị {from}–{to} / {totalElements} {unit}</span>
      {onSize && <label className="flex shrink-0 items-center gap-2 whitespace-nowrap">Mỗi trang
        <select value={size} disabled={disabled} onChange={e => onSize(Number(e.target.value))}
          className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-sm" style={{ width: 'auto' }}>
          {sizes.map((n: number) => <option key={n} value={n}>{n}</option>)}
        </select>
      </label>}
    </div>
    <nav aria-label="Phân trang" className="flex flex-nowrap items-center justify-center gap-1">
      <button type="button" aria-label="Trang trước" disabled={disabled || page <= 0} onClick={() => onPage(page - 1)}
        className={`${btn} border-slate-200 bg-white disabled:opacity-40`}><ChevronLeft size={16} /></button>
      {slots.map((n: number | '…', i: number) => n === '…'
        ? <span key={`gap-${i}`} className="grid h-9 w-6 shrink-0 place-items-center text-slate-400">…</span>
        : <button key={n} type="button" disabled={disabled} aria-current={n === page ? 'page' : undefined} onClick={() => onPage(n)}
          className={`${btn} font-semibold ${n === page ? 'border-[#5367C9] bg-[#5367C9] text-white' : 'border-slate-200 bg-white text-slate-700 hover:border-[#5367C9]'}`}>{n + 1}</button>)}
      <button type="button" aria-label="Trang sau" disabled={disabled || page >= pages - 1} onClick={() => onPage(page + 1)}
        className={`${btn} border-slate-200 bg-white disabled:opacity-40`}><ChevronRight size={16} /></button>
    </nav>
  </div>;
}