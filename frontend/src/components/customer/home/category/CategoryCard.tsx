import { useNavigate } from 'react-router-dom';
import type { CategoryItem } from './categoryData';
interface CategoryCardProps { category: CategoryItem; }
export default function CategoryCard({ category }: CategoryCardProps) {
  const navigate = useNavigate();
  return <button type="button" onClick={() => navigate(`/products?categoryId=${encodeURIComponent(category.id)}&category=${encodeURIComponent(category.name)}`)} className="group flex min-w-0 flex-col items-center text-center">
    <div className="flex aspect-square w-full max-w-[88px] items-center justify-center overflow-hidden rounded-2xl border border-[#E5EBF6] bg-white shadow-sm transition-all group-hover:-translate-y-1 group-hover:border-[#CFD9F2]">
      <img src={category.image} alt={category.name} className="h-full w-full object-cover transition-transform group-hover:scale-105" />
    </div><span className="mt-3 line-clamp-2 text-xs font-medium leading-5 text-[#374151] group-hover:text-[#5367C9] sm:text-sm">{category.name}</span>
  </button>;
}
