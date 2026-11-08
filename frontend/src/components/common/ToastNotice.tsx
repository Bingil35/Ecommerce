import { useEffect } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react';

export type ToastVariant = 'success' | 'error' | 'warning' | 'info';
export interface ToastNoticeProps {
  message: string;
  variant: ToastVariant;
  onClose: () => void;
  duration?: number;
  actionLabel?: string;
  actionTo?: string;
}
const icons = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};
export default function ToastNotice({
  message,
  variant,
  onClose,
  duration = 4000,
  actionLabel,
  actionTo,
}: ToastNoticeProps) {
  const reduced = useReducedMotion();
  useEffect(() => {
    const timer = window.setTimeout(onClose, duration);
    return () => window.clearTimeout(timer);
  }, [duration, onClose]);
  const Icon = icons[variant];
  return (
    <motion.div
      layout
      role={variant === 'error' ? 'alert' : 'status'}
      aria-atomic="true"
      initial={{ opacity: 0, x: reduced ? 0 : 35 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: reduced ? 0 : 35 }}
      transition={{ duration: reduced ? 0 : 0.18 }}
      className={`kala-toast kala-toast-${variant}`}
    >
      <Icon size={21} aria-hidden="true" />
      <div className="kala-toast-content">
        <p>{message}</p>
        {actionLabel && actionTo && (
          <a href={actionTo} onClick={onClose}>
            {actionLabel}
          </a>
        )}
      </div>
      <button type="button" aria-label="Đóng thông báo" onClick={onClose}>
        <X size={17} />
      </button>
    </motion.div>
  );
}
