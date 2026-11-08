import axiosClient from '../api/axiosClient';

type Envelope<T> = { data: T };

export interface ProductOptionDto {
  id: string;
  productId: string;
  name: string;
}

export interface ProductOptionValueDto {
  id: string;
  optionId: string;
  value: string;
}

export interface ProductVariantDto {
  id: string;
  productId: string;
  sku: string | null;
  price: number | null;
  comparePrice: number | null;
  stockQuantity: number | null;
  imageUrl: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'REJECTED';
  optionValueIds: string[];
}

export interface ProductVariantPage {
  items: ProductVariantDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  minPrice: number | null;
  maxPrice: number | null;
  minComparePrice: number | null;
  maxComparePrice: number | null;
}

export interface ProductMediaDto {
  id: string;
  productId: string;
  mediaType: 'IMAGE' | 'VIDEO';
  mediaUrl: string;
  displayOrder: number | null;
  thumbnail: boolean;
}

const base = '/shop-owner';

export const productDetailApi = {
  async options(productId: string): Promise<ProductOptionDto[]> {
    return (await axiosClient.get<Envelope<ProductOptionDto[]>>(`${base}/products/${productId}/options`)).data.data;
  },
  async optionValues(optionId: string): Promise<ProductOptionValueDto[]> {
    return (await axiosClient.get<Envelope<ProductOptionValueDto[]>>(`${base}/options/${optionId}/values`)).data.data;
  },
  async variants(productId: string): Promise<ProductVariantDto[]> {
    return (await axiosClient.get<Envelope<ProductVariantDto[]>>(`${base}/products/${productId}/variants`)).data.data;
  },
  async variantsPage(productId: string, page = 0, size = 10): Promise<ProductVariantPage> {
    return (await axiosClient.get<Envelope<ProductVariantPage>>(`${base}/products/${productId}/variants/page`, { params: { page, size } })).data.data;
  },
  async media(productId: string): Promise<ProductMediaDto[]> {
    return (await axiosClient.get<Envelope<ProductMediaDto[]>>(`${base}/products/${productId}/media`)).data.data;
  },
};