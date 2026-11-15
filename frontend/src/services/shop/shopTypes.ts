import type { UserResponse } from '../auth/authTypes';

export interface ShopResponse {
  id: string;
  shopName: string;
  slug: string;
  logoUrl: string | null;
  bannerUrl: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  addressProvince: string | null;
  addressWard: string | null;
  addressDetail: string | null;
  status: string;
  createdAt: string;
}

export interface CreateShopRequest {
  shopName: string;
  phone?: string;
  email?: string;
  address?: string;
  addressProvince?: string;
  addressWard?: string;
  addressDetail?: string;
  logoUrl?: string;
  bannerUrl?: string;
}

export interface ShopRegistrationResponse {
  shop: ShopResponse;
  accessToken: string;
  tokenType: string;
  user: UserResponse;
}
