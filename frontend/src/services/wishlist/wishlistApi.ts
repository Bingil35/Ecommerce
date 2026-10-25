import axios from 'axios';

import axiosClient from '../api/axiosClient';
import type { ApiEnvelope } from '../catalog/productApi';
import type {
  RemoveManyResult,
  WishlistAllResult,
  WishlistItemDto,
  WishlistPageDto,
  WishlistStatusDto,
} from './wishlistTypes';

/** BE giới hạn size tối đa 50 (ProductSearchService.MAX_PAGE_SIZE). */
export const WISHLIST_BE_MAX_PAGE_SIZE = 50;
/** Chặn tải quá nhiều trang: 40 x 50 = 2000 sản phẩm. */
const MAX_PAGES = 40;
const REMOVE_BATCH_SIZE = 5;

const dataOf = <T>(response: { data: ApiEnvelope<T> }): T => response.data.data;

export const isAbortError = (error: unknown): boolean =>
  axios.isCancel(error) ||
  (error instanceof DOMException && error.name === 'AbortError');

export const wishlistApi = {
  /** GET /customer/wishlist?page&size — mới thêm trước, chỉ gồm sản phẩm đang hiển thị. */
  async listPage(
    page: number,
    size: number,
    signal?: AbortSignal
  ): Promise<WishlistPageDto> {
    return dataOf(
      await axiosClient.get<ApiEnvelope<WishlistPageDto>>('/customer/wishlist', {
        params: { page, size },
        signal,
      })
    );
  },

  /**
   * BE chỉ phân trang và không hỗ trợ tìm kiếm/lọc/sắp xếp, nên để làm được các việc đó
   * ta tải toàn bộ danh sách (mỗi lần tối đa 50 mục) rồi xử lý ở client.
   */
  async listAll(signal?: AbortSignal): Promise<WishlistAllResult> {
    const first = await wishlistApi.listPage(0, WISHLIST_BE_MAX_PAGE_SIZE, signal);
    const pagesToLoad = Math.min(first.totalPages, MAX_PAGES);

    const rest = await Promise.all(
      Array.from({ length: Math.max(0, pagesToLoad - 1) }, (_, index) =>
        wishlistApi.listPage(index + 1, WISHLIST_BE_MAX_PAGE_SIZE, signal)
      )
    );

    // Dữ liệu có thể xê dịch giữa các lần gọi -> loại trùng theo sản phẩm
    const byProduct = new Map<string, WishlistItemDto>();
    [first, ...rest].forEach((page) =>
      page.items.forEach((item) => {
        if (!byProduct.has(item.product.id)) byProduct.set(item.product.id, item);
      })
    );

    return {
      items: [...byProduct.values()],
      totalElements: first.totalElements,
      truncated: first.totalPages > MAX_PAGES,
    };
  },

  /** Chỉ lấy tổng số (1 request nhỏ) cho badge trên Header. */
  async count(signal?: AbortSignal): Promise<number> {
    return (await wishlistApi.listPage(0, 1, signal)).totalElements;
  },

  /** POST /customer/wishlist/{productId} — 201 mới thêm, 200 đã có từ trước. */
  async add(productId: string): Promise<WishlistStatusDto> {
    return dataOf(
      await axiosClient.post<ApiEnvelope<WishlistStatusDto>>(
        `/customer/wishlist/${encodeURIComponent(productId)}`
      )
    );
  },

  /** DELETE /customer/wishlist/{productId} — 404 nếu sản phẩm không có trong danh sách. */
  async remove(productId: string): Promise<WishlistStatusDto> {
    return dataOf(
      await axiosClient.delete<ApiEnvelope<WishlistStatusDto>>(
        `/customer/wishlist/${encodeURIComponent(productId)}`
      )
    );
  },

  /** GET /customer/wishlist/{productId}/status */
  async status(productId: string, signal?: AbortSignal): Promise<WishlistStatusDto> {
    return dataOf(
      await axiosClient.get<ApiEnvelope<WishlistStatusDto>>(
        `/customer/wishlist/${encodeURIComponent(productId)}/status`,
        { signal }
      )
    );
  },

  /**
   * BE không có API xóa hàng loạt, nên "Xóa tất cả" gọi DELETE từng sản phẩm theo lô nhỏ.
   * 404 nghĩa là sản phẩm đã không còn trong danh sách nên vẫn tính là đã xóa.
   */
  async removeMany(productIds: string[]): Promise<RemoveManyResult> {
    const removedIds: string[] = [];
    const failedIds: string[] = [];

    for (let start = 0; start < productIds.length; start += REMOVE_BATCH_SIZE) {
      const batch = productIds.slice(start, start + REMOVE_BATCH_SIZE);
      const results = await Promise.allSettled(
        batch.map((id) => wishlistApi.remove(id))
      );

      results.forEach((result, index) => {
        const id = batch[index];
        if (result.status === 'fulfilled' || isNotFound(result.reason)) {
          removedIds.push(id);
        } else {
          failedIds.push(id);
        }
      });
    }

    return { removedIds, failedIds };
  },
};

export const isNotFound = (error: unknown): boolean =>
  axios.isAxiosError(error) && error.response?.status === 404;

export const getHttpStatus = (error: unknown): number | undefined =>
  axios.isAxiosError(error) ? error.response?.status : undefined;

export const getWishlistErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    if (status === 401) {
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    }
    if (status === 403) {
      return 'Chỉ tài khoản khách hàng mới sử dụng được danh sách yêu thích.';
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Không kết nối được máy chủ. Vui lòng thử lại.';
    }
    if (error.code === 'ECONNABORTED') {
      return 'Máy chủ phản hồi quá lâu. Vui lòng thử lại.';
    }
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message) return message;
  }
  return 'Không thể xử lý danh sách yêu thích. Vui lòng thử lại.';
};
