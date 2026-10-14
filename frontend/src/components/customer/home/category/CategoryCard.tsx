import type { CategoryItem } from './categoryData';

interface CategoryCardProps {
  category: CategoryItem;
}

const CategoryCard = ({ category }: CategoryCardProps) => {
  return (
    <button
      type="button"
      className="
        group
        flex
        min-w-0
        flex-col
        items-center
        text-center
      "
    >
      <div
        className="
          flex
          aspect-square
          w-full
          max-w-[88px]
          items-center
          justify-center
          overflow-hidden
          rounded-2xl
          border
          border-[#E5EBF6]
          bg-white
          shadow-[0_4px_16px_rgba(72,93,140,0.05)]
          transition-all
          duration-200
          group-hover:-translate-y-1
          group-hover:border-[#CFD9F2]
          group-hover:shadow-[0_8px_22px_rgba(72,93,140,0.1)]
        "
      >
        <img
          src={category.image}
          alt={category.name}
          className="
            h-full
            w-full
            object-cover
            transition-transform
            duration-300
            group-hover:scale-105
          "
        />
      </div>

      <span
        className="
          mt-3
          line-clamp-2
          text-xs
          font-medium
          leading-5
          text-[#374151]
          transition-colors
          group-hover:text-[#5367C9]
          sm:text-sm
        "
      >
        {category.name}
      </span>
    </button>
  );
};

export default CategoryCard;