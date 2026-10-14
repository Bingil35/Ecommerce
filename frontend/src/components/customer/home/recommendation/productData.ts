export interface HomeProduct {
  id: number;
  name: string;
  image: string;
  price: number;
  originalPrice?: number;
  discount?: number;
  sold: number;
  rating: number;
}

export const productData: HomeProduct[] = [
  {
    id: 1,
    name: 'Tai nghe Bluetooth không dây thế hệ mới',
    image: 'https://placehold.co/400x400/EEF3FF/5367C9?text=Product+1',
    price: 299000,
    originalPrice: 450000,
    discount: 34,
    sold: 1200,
    rating: 4.8,
  },
  {
    id: 2,
    name: 'Áo thun nam cotton basic cao cấp',
    image: 'https://placehold.co/400x400/F2F5FF/5367C9?text=Product+2',
    price: 159000,
    originalPrice: 220000,
    discount: 28,
    sold: 850,
    rating: 4.7,
  },
  {
    id: 3,
    name: 'Bình giữ nhiệt inox 500ml',
    image: 'https://placehold.co/400x400/EDF3FF/5367C9?text=Product+3',
    price: 189000,
    sold: 620,
    rating: 4.9,
  },
  {
    id: 4,
    name: 'Bàn phím cơ không dây RGB',
    image: 'https://placehold.co/400x400/F0F4FF/5367C9?text=Product+4',
    price: 599000,
    originalPrice: 750000,
    discount: 20,
    sold: 430,
    rating: 4.8,
  },
  {
    id: 5,
    name: 'Túi đeo chéo thời trang unisex',
    image: 'https://placehold.co/400x400/EEF4FF/5367C9?text=Product+5',
    price: 249000,
    originalPrice: 320000,
    discount: 22,
    sold: 310,
    rating: 4.6,
  },
  {
    id: 6,
    name: 'Đèn bàn LED học tập chống cận',
    image: 'https://placehold.co/400x400/F3F6FF/5367C9?text=Product+6',
    price: 329000,
    originalPrice: 420000,
    discount: 22,
    sold: 270,
    rating: 4.8,
  },
];