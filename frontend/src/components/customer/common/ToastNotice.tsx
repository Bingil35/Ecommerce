import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { CheckCircle2, Heart, X } from 'lucide-react';

export type ToastVariant = 'success' | 'danger';

interface ToastNoticeProps {
  message: string;
  /** success = xanh (thêm thành công), danger = đỏ (hủy/xóa) */
  variant?: ToastVariant;
  /** Liên kết phụ, ví dụ "Xem danh sách" */
  actionLabel?: string;
  actionTo?: string;
  onClose: () => void;
}

const VARIANT_STYLES: Record<
  ToastVariant,
  { box: string; icon: string; link: string; close: string }
> = {
  success: {
    box: 'border-[#BFE7D1] bg-[#F0FBF5]',
    icon: 'text-[#1F9D61]',
    link: 'text-[#1F9D61]',
    close: 'text-[#5FA882] hover:bg-[#DDF3E6] hover:text-[#17774A]',
  },
  danger: {
    box: 'border-[#F6C9D0] bg-[#FFF3F5]',
    icon: 'text-[#E5334F]',
    link: 'text-[#E5334F]',
    close: 'text-[#D9707F] hover:bg-[#FDE2E6] hover:text-[#C4263F]',
  },
};

/**
 * Thông báo nổi ở góc phải phía trên: trượt vào từ mép phải, có nút tắt.
 * Việc tự ẩn do nơi dùng quyết định; bọc trong <AnimatePresence> để có hiệu ứng trượt ra.
 */
const ToastNotice = ({
  message,
  variant = 'success',
  actionLabel,
  actionTo,
  onClose,
}: ToastNoticeProps) => {
  const styles = VARIANT_STYLES[variant];
  const Icon = variant === 'success' ? CheckCircle2 : Heart;

  return (
    <motion.div
      role="status"
      initial={{ x: 'calc(100% + 1.5rem)', opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      exit={{ x: 'calc(100% + 1.5rem)', opacity: 0 }}
      transition={{ type: 'spring', stiffness: 380, damping: 32 }}
      className={`fixed right-4 top-20 z-[70] flex w-[min(360px,calc(100vw-2rem))] items-start gap-3 rounded-xl border py-3 pl-4 pr-2.5 shadow-[0_12px_32px_rgba(31,37,64,0.16)] ${styles.box}`}
    >
      <Icon
        size={20}
        className={`mt-0.5 shrink-0 ${styles.icon}`}
        fill={variant === 'danger' ? 'currentColor' : 'none'}
      />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium leading-5 text-[#202642]">{message}</p>
        {actionLabel && actionTo && (
          <Link
            to={actionTo}
            onClick={onClose}
            className={`mt-1 inline-block text-sm font-semibold hover:underline ${styles.link}`}
          >
            {actionLabel}
          </Link>
        )}
      </div>
      <button
        type="button"
        aria-label="Đóng thông báo"
        onClick={onClose}
        className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full transition ${styles.close}`}
      >
        <X size={16} />
      </button>
    </motion.div>
  );
};

export default ToastNotice;