import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import ProductCard from '../../components/customer/home/recommendation/ProductCard';
import type { HomeProduct } from '../../components/customer/home/recommendation/productData';
import { productApi } from '../../services/catalog/productApi';

export default function ProductListingPage() {
  const [params] = useSearchParams();
  const [products, setProducts] = useState<HomeProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const q = params.get('q') || '';
  const categoryId = params.get('categoryId') || '';
  const featured = params.get('featured') === 'true';
  const title = params.get('title') || (featured ? 'Gợi ý hôm nay' : categoryId ? params.get('category') || 'Sản phẩm theo danh mục' : q ? `Kết quả tìm kiếm: “${q}”` : 'Tất cả sản phẩm Kala');

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError('');
    productApi.list({
      q: q || undefined,
      categoryId: categoryId || undefined,
      featured: featured ? true : undefined,
    })
      .then((items) => { if (alive) setProducts(items); })
      .catch(() => { if (alive) setError('Không thể tải danh sách sản phẩm. Vui lòng kiểm tra kết nối backend và thử lại.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [q, categoryId, featured]);

  return (
    <main className="min-h-screen bg-[#F7FAFF]">
      <div className="mx-auto max-w-[1200px] px-4 py-8 sm:py-10">
        <div className="mb-7">
          <p className="text-xs font-semibold uppercase tracking-[.16em] text-[#7180C8]">Kala / Sản phẩm</p>
          <h1 className="mt-2 text-2xl font-bold text-[#202642] sm:text-3xl">{title}</h1>
          <p className="mt-2 text-sm text-[#8A93A6]">Khám phá các sản phẩm đang được bán trên Kala.</p>
        </div>

        {loading && <div className="py-16 text-center text-sm text-[#8A93A6]">Đang tải sản phẩm...</div>}
        {!loading && error && <div role="alert" className="rounded-2xl bg-white p-8 text-center text-sm text-rose-500">{error}</div>}
        {!loading && !error && products.length === 0 && (
          <div className="rounded-2xl border border-dashed border-[#DCE5F5] bg-white p-12 text-center">
            <Search className="mx-auto mb-3 text-[#A4AEC2]" />
            <h2 className="font-semibold text-[#202642]">Không tìm thấy sản phẩm</h2>
            <p className="mt-1 text-sm text-[#8A93A6]">Hiện chưa có sản phẩm phù hợp để hiển thị.</p>
          </div>
        )}
        {!loading && !error && products.length > 0 && (
          <>
            <div className="mb-4 text-sm text-[#8A93A6]">{products.length} sản phẩm</div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5 lg:gap-5">
              {products.map((product) => <ProductCard key={product.id} product={product} />)}
            </div>
          </>
        )}
      </div>
    </main>
  );
}
