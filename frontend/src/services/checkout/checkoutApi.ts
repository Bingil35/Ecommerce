import axiosClient from '../api/axiosClient';
import type { CustomerAddress, Coupon, CheckoutPreview, CheckoutPreviewRequest, PlaceOrderRequest, OrderListItem, PageResponse, OrderDetail, PaymentConfirmation, OrderStatus, PlaceOrderResult, ShopOwnerOrder } from './checkoutTypes';

type Envelope<T> = { data?: T; success?: boolean; message?: string } | T;
const unwrap = <T,>(value: Envelope<T>): T =>
  value && typeof value === 'object' && 'data' in value ? (value as { data: T }).data : value as T;

export const checkoutApi = {
  async getAddresses(): Promise<CustomerAddress[]> {
    return unwrap((await axiosClient.get<Envelope<CustomerAddress[]>>('/customer/addresses')).data);
  },
  async saveAddress(input: Omit<CustomerAddress, 'id' | 'createdAt' | 'updatedAt' | 'isDefault'> & { isDefault?: boolean }, id?: string): Promise<CustomerAddress> {
    const url = id ? `/customer/addresses/${id}` : '/customer/addresses';
    return unwrap((await (id ? axiosClient.put<Envelope<CustomerAddress>>(url, input) : axiosClient.post<Envelope<CustomerAddress>>(url, input))).data);
  },
  async deleteAddress(id: string): Promise<void> { await axiosClient.delete(`/customer/addresses/${id}`); },
  async setDefaultAddress(id: string): Promise<CustomerAddress> {
    return unwrap((await axiosClient.put<Envelope<CustomerAddress>>(`/customer/addresses/${id}/default`)).data);
  },
  async getCoupons(scope?: 'PLATFORM' | 'SHOP'): Promise<Coupon[]> {
    return unwrap((await axiosClient.get<Envelope<Coupon[]>>('/customer/coupons', { params: scope ? { scope } : {} })).data);
  },
  async preview(payload: CheckoutPreviewRequest): Promise<CheckoutPreview> {
    return unwrap((await axiosClient.post<Envelope<CheckoutPreview>>('/customer/checkout/preview', payload)).data);
  },
  async validateCoupon(payload: CheckoutPreviewRequest): Promise<CheckoutPreview> {
    return unwrap((await axiosClient.post<Envelope<CheckoutPreview>>('/customer/coupons/validate', payload)).data);
  },
  async placeOrder(payload: PlaceOrderRequest): Promise<PlaceOrderResult> {
    return unwrap((await axiosClient.post<Envelope<PlaceOrderResult>>('/customer/orders', payload)).data);
  },
  async getOrders(params: { page: number; size: number; status?: OrderStatus | ''; orderCode?: string; from?: string; to?: string }): Promise<PageResponse<OrderListItem>> {
    const response = await axiosClient.get<Envelope<PageResponse<OrderListItem>>>('/customer/orders', { params });
    return unwrap(response.data);
  },
  async getOrder(id: string): Promise<OrderDetail> {
    return unwrap((await axiosClient.get<Envelope<OrderDetail>>(`/customer/orders/${id}`)).data);
  },
  async cancelSubOrder(id: string, reason?: string): Promise<unknown> {
    return unwrap((await axiosClient.post(`/customer/orders/sub-orders/${id}/cancel`, { reason })).data);
  },
  async confirmCodReceived(orderId: string): Promise<PaymentConfirmation> {
    return unwrap((await axiosClient.post<Envelope<PaymentConfirmation>>(`/customer/orders/${orderId}/cod/confirm-received`)).data);
  },
  async confirmSubOrderReceived(subOrderId: string): Promise<{ status: OrderStatus }> {
    return unwrap((await axiosClient.post<Envelope<{ status: OrderStatus }>>(`/customer/orders/sub-orders/${subOrderId}/confirm-received`)).data);
  },
  async updateShopSubOrderStatus(subOrderId: string, status: OrderStatus, note?: string): Promise<{ status: OrderStatus }> {
    return unwrap((await axiosClient.patch<Envelope<{ status: OrderStatus }>>(`/shop-owner/orders/sub-orders/${subOrderId}/status`, { status, note })).data);
  },
  async getShopOrders(params: { page: number; size: number; status?: OrderStatus | ''; keyword?: string }): Promise<PageResponse<ShopOwnerOrder>> {
    return unwrap((await axiosClient.get<Envelope<PageResponse<ShopOwnerOrder>>>('/shop-owner/orders', { params })).data);
  },
  async getShopOrder(id: string): Promise<ShopOwnerOrder> {
    return unwrap((await axiosClient.get<Envelope<ShopOwnerOrder>>(`/shop-owner/orders/sub-orders/${id}`)).data);
  },
};

export const formatMoney = (value: number | string | null | undefined) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value ?? 0));
export const formatDateTime = (value: string) =>
  new Date(value).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' });
export const getApiError = (error: unknown, fallback = 'Không thể xử lý yêu cầu. Vui lòng thử lại.') => {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return fallback;
};
