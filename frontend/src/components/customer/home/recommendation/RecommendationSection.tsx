import { ArrowRight } from 'lucide-react';

import ProductCard from './ProductCard';
import { productData } from './productData';
import { motion } from 'framer-motion';

const RecommendationSection = () => {
  return (
    <motion.section
      layout
      transition={{
        layout: {
          duration: 0.45,
          ease: [0.22, 1, 0.36, 1],
        },
      }}
      className="pb-12 pt-12"
    >
      {/* Header */}
      <div className="mb-5 flex items-end justify-between">
        <div>
          <h2
            className="
              text-xl
              font-bold
              tracking-tight
              text-[#202642]
              sm:text-2xl
            "
          >
            Gợi ý cho bạn
          </h2>

          <p className="mt-1 text-xs text-[#8A93A6] sm:text-sm">
            Những sản phẩm có thể bạn sẽ yêu thích
          </p>
        </div>

        <button
          type="button"
          className="
            flex
            items-center
            gap-1
            rounded-full
            px-2
            py-1
            text-xs
            font-medium
            text-[#5367C9]
            transition-colors
            hover:bg-[#EEF3FF]
            sm:text-sm
          "
        >
          Xem thêm
          <ArrowRight
            size={15}
            strokeWidth={1.8}
          />
        </button>
      </div>

      {/* Products */}
      <div
        className="
          grid
          grid-cols-2
          gap-3
          sm:grid-cols-3
          sm:gap-4
          lg:grid-cols-6
          lg:gap-5
        "
      >
        {productData.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
          />
        ))}
      </div>
    </motion.section>
  );
};

export default RecommendationSection;