export interface Category {
  id: string;
  name: string;
  parentId?: string | null;
  children?: Category[];
}

export interface Product {
  id: string;
  slug: string;
  name: string;
  categoryId?: string | null;
  price: number | null; // giá của biến thể thấp nhất (null nếu chưa có biến thể)
  comparePrice?: number; // giá so sánh
  rating: number;
  reviewCount: number;
  inStock: boolean;
  imageUrl?: string;
}
