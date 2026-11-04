import axiosClient from '../api/axiosClient';

export interface ProductReview {
  id: string; productId: string; productName: string; customerName: string | null;
  rating: number; content: string | null; createdAt: string;
  replies: { id: string; content: string; createdAt: string }[];
}
type Envelope<T> = { data: T; message?: string } | T;
const unwrap = <T,>(value: Envelope<T>): T => value && typeof value === 'object' && 'data' in value ? (value as {data:T}).data : value as T;

export const reviewApi = {
  async list(productId: string) { return unwrap((await axiosClient.get<Envelope<ProductReview[]>>(`/products/${productId}/reviews`)).data); },
  async create(productId: string, input: { rating: number; content: string; subOrderId: string; variantId?: string }) {
    return unwrap((await axiosClient.post<Envelope<ProductReview>>(`/customer/products/${productId}/reviews`, input)).data);
  },
  async ownerList() { return unwrap((await axiosClient.get<Envelope<ProductReview[]>>('/shop-owner/reviews')).data); },
  async reply(reviewId: string, content: string) {
    return unwrap((await axiosClient.post<Envelope<ProductReview>>(`/shop-owner/reviews/${reviewId}/replies`, { content })).data);
  },
};

export const reviewDate = (value: string) => new Date(value).toLocaleDateString('vi-VN', { dateStyle: 'medium' });
