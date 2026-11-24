import type { OrderStatus } from './checkoutTypes';

export const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  SHIPPING: 'Đang vận chuyển',
  DELIVERED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

export const orderStatusClasses: Record<OrderStatus, string> = {
  PENDING: 'bg-amber-50 text-amber-700',
  CONFIRMED: 'bg-indigo-50 text-indigo-700',
  SHIPPING: 'bg-sky-50 text-sky-700',
  DELIVERED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-slate-100 text-slate-600',
};
