import type { UserResponse } from './authTypes';

export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  SHOP_OWNER: 'SHOP_OWNER',
  ADMIN: 'ADMIN',
} as const;

export function hasRole(
  user: UserResponse | null | undefined,
  role: string
): boolean {
  if (!user) {
    return false;
  }

  // roleDefault dùng làm phương án dự phòng cho phiên cũ chưa lưu danh sách roles.
  return Boolean(user.roles?.includes(role)) || user.roleDefault === role;
}

export function isShopOwner(
  user: UserResponse | null | undefined
): boolean {
  return hasRole(user, ROLES.SHOP_OWNER);
}
