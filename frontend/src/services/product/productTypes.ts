export interface Category {
  id: string;
  name: string;
  children?: Category[];
}

export interface Product {
  id: string;
  slug: string;
  name: string;
  categoryId: string;
  brand: string;
  tags: string[];
  price: number; // giá của biến thể thấp nhất
  comparePrice?: number; // giá so sánh
  rating: number;
  reviewCount: number;
  sold: number;
  inStock: boolean;
  shopName: string;
  imageUrl?: string;
}