import type { ProductCardDto } from '../catalog/productApi';

/** Một mục trong danh sách yêu thích (WishlistItemResponse của BE). */
export interface WishlistItemDto {
  /** Id của dòng wishlist (không phải id sản phẩm) */
  id: string;
  /** Thời điểm thêm vào yêu thích (ISO 8601) */
  addedAt: string;
  /** Sản phẩm, cùng dạng với thẻ sản phẩm của catalog */
  product: ProductCardDto;
}

/** PageResponse<WishlistItemResponse> của BE. `page` bắt đầu từ 0. */
export interface WishlistPageDto {
  items: WishlistItemDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** WishlistStatusResponse của BE. */
export interface WishlistStatusDto {
  productId: string;
  inWishlist: boolean;
}

export interface WishlistAllResult {
  items: WishlistItemDto[];
  totalElements: number;
  /** true nếu danh sách dài hơn giới hạn tải của giao diện */
  truncated: boolean;
}

export interface RemoveManyResult {
  removedIds: string[];
  failedIds: string[];
}
