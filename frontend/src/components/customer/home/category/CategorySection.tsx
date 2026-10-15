import {
  useEffect,
  useRef,
  useState,
} from 'react';

import {
  AnimatePresence,
  motion,
} from 'framer-motion';

import {
  Minus,
  Plus,
} from 'lucide-react';

import CategoryCard from './CategoryCard';
import { categoryApi, type CategoryDto } from '../../../../services/catalog/productApi';
import type { CategoryItem } from './categoryData';

const VISIBLE_CATEGORY_COUNT = 7;

const CategorySection = () => {
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [contentHeight, setContentHeight] = useState(0);

  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    categoryApi.list().then((items: CategoryDto[]) => setCategories(items.filter((c) => !c.status || c.status === 'ACTIVE').map((c) => ({ id: c.id, name: c.name, slug: c.slug, image: c.imageUrl || 'https://placehold.co/160x160/EEF3FF/5367C9?text=Category' })))).catch(() => setLoadError(true)).finally(() => setLoading(false));
  }, []);

  const visibleCategories = categories.slice(0, VISIBLE_CATEGORY_COUNT);
  const hiddenCategories = categories.slice(VISIBLE_CATEGORY_COUNT);

  /*
   * Theo dõi chiều cao thật của grid.
   *
   * Khi mở rộng:
   * grid cao lên
   * -> ResizeObserver nhận biết
   * -> contentHeight thay đổi
   * -> motion.div animate height
   * -> RecommendationSection bị đẩy xuống từ từ
   *
   * Khi thu gọn:
   * grid thấp xuống
   * -> contentHeight giảm
   * -> motion.div animate height
   * -> RecommendationSection kéo lên từ từ
   */
  useEffect(() => {
    const element = contentRef.current;

    if (!element) {
      return;
    }

    const updateHeight = () => {
      setContentHeight(element.scrollHeight);
    };

    updateHeight();

    const resizeObserver = new ResizeObserver(() => {
      updateHeight();
    });

    resizeObserver.observe(element);

    return () => {
      resizeObserver.disconnect();
    };
  }, []);

  return (
    <section className="pt-10 sm:pt-12">
      {/* Section header */}
      <div className="mb-5">
        <h2
          className="
            text-xl
            font-bold
            tracking-tight
            text-[#202642]
            sm:text-2xl
          "
        >
          Danh mục nổi bật
        </h2>

        <p className="mt-1 text-xs text-[#8A93A6] sm:text-sm">
          Khám phá sản phẩm theo danh mục
        </p>
      </div>

      {loading && <p className="py-6 text-sm text-[#8A93A6]">Đang tải danh mục...</p>}
      {!loading && loadError && <p className="py-4 text-sm text-rose-500">Không thể tải danh mục. Vui lòng thử lại sau.</p>}
      {!loading && !loadError && categories.length === 0 && <p className="py-4 text-sm text-[#8A93A6]">Chưa có danh mục đang hoạt động.</p>}
      {/* Animated content wrapper */}
      <motion.div
        animate={{
          height: contentHeight,
        }}
        initial={false}
        transition={{
          duration: 0.45,
          ease: [0.22, 1, 0.36, 1],
        }}
        className="overflow-hidden"
      >
        {/* Actual grid */}
        <div
          ref={contentRef}
          className="
            grid
            grid-cols-4
            gap-x-3
            gap-y-6
            sm:grid-cols-5
            sm:gap-x-5
            lg:grid-cols-8
            lg:gap-x-6
          "
        >
          {/* Danh mục mặc định */}
          {visibleCategories.map((category) => (
            <CategoryCard
              key={category.id}
              category={category}
            />
          ))}

          {/* Danh mục mở rộng */}
          <AnimatePresence initial={false}>
            {isExpanded &&
              hiddenCategories.map((category) => (
                <motion.div
                  key={category.id}
                  initial={{
                    opacity: 0,
                    y: -12,
                    scale: 0.96,
                  }}
                  animate={{
                    opacity: 1,
                    y: 0,
                    scale: 1,
                  }}
                  exit={{
                    opacity: 0,
                    y: -12,
                    scale: 0.96,
                  }}
                  transition={{
                    duration: 0.25,
                    ease: 'easeOut',
                  }}
                >
                  <CategoryCard category={category} />
                </motion.div>
              ))}
          </AnimatePresence>

          {/* Xem thêm / Thu gọn */}
          {hiddenCategories.length > 0 && (
            <motion.button
              type="button"
              onClick={() =>
                setIsExpanded((previous) => !previous)
              }
              whileHover={{
                y: -2,
              }}
              whileTap={{
                scale: 0.97,
              }}
              className="
                group
                flex
                min-w-0
                flex-col
                items-center
                text-center
              "
            >
              {/* Icon */}
              <div
                className="
                  flex
                  aspect-square
                  w-full
                  max-w-[88px]
                  items-center
                  justify-center
                  rounded-2xl
                  border
                  border-dashed
                  border-[#CBD5E8]
                  bg-white
                  text-[#5367C9]
                  shadow-[0_4px_16px_rgba(72,93,140,0.04)]
                  transition-all
                  duration-200
                  group-hover:border-[#AEBDE0]
                  group-hover:bg-[#F7F9FF]
                  group-hover:text-[#4659B8]
                  group-hover:shadow-[0_8px_22px_rgba(72,93,140,0.08)]
                "
              >
                <AnimatePresence
                  mode="wait"
                  initial={false}
                >
                  {isExpanded ? (
                    <motion.div
                      key="collapse"
                      initial={{
                        opacity: 0,
                        rotate: -90,
                        scale: 0.7,
                      }}
                      animate={{
                        opacity: 1,
                        rotate: 0,
                        scale: 1,
                      }}
                      exit={{
                        opacity: 0,
                        rotate: 90,
                        scale: 0.7,
                      }}
                      transition={{
                        duration: 0.2,
                        ease: 'easeOut',
                      }}
                    >
                      <Minus
                        size={24}
                        strokeWidth={1.8}
                      />
                    </motion.div>
                  ) : (
                    <motion.div
                      key="expand"
                      initial={{
                        opacity: 0,
                        rotate: -90,
                        scale: 0.7,
                      }}
                      animate={{
                        opacity: 1,
                        rotate: 0,
                        scale: 1,
                      }}
                      exit={{
                        opacity: 0,
                        rotate: 90,
                        scale: 0.7,
                      }}
                      transition={{
                        duration: 0.2,
                        ease: 'easeOut',
                      }}
                    >
                      <Plus
                        size={24}
                        strokeWidth={1.8}
                      />
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              {/* Text */}
              <AnimatePresence
                mode="wait"
                initial={false}
              >
                {isExpanded ? (
                  <motion.span
                    key="collapse-text"
                    initial={{
                      opacity: 0,
                      y: -4,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                    }}
                    exit={{
                      opacity: 0,
                      y: 4,
                    }}
                    transition={{
                      duration: 0.15,
                    }}
                    className="
                      mt-3
                      text-xs
                      font-medium
                      leading-5
                      text-[#5367C9]
                      sm:text-sm
                    "
                  >
                    Thu gọn
                  </motion.span>
                ) : (
                  <motion.span
                    key="expand-text"
                    initial={{
                      opacity: 0,
                      y: -4,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                    }}
                    exit={{
                      opacity: 0,
                      y: 4,
                    }}
                    transition={{
                      duration: 0.15,
                    }}
                    className="
                      mt-3
                      text-xs
                      font-medium
                      leading-5
                      text-[#5367C9]
                      sm:text-sm
                    "
                  >
                    Xem thêm
                  </motion.span>
                )}
              </AnimatePresence>
            </motion.button>
          )}
        </div>
      </motion.div>
    </section>
  );
};

export default CategorySection;