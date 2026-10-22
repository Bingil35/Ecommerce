import { PackageX, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const EmptyCart = () => {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-[500px] flex-col items-center justify-center rounded-2xl border border-[#E8EDF5] bg-white px-6 py-16 text-center shadow-[0_4px_18px_rgba(72,93,140,0.05)]">
      <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-[#F3F6FC]">
        <PackageX size={48} strokeWidth={1.5} className="text-[#9AA2B1]" />
      </div>

      <h2 className="mb-3 text-2xl font-bold text-[#202642]">
        Giỏ hàng trống
      </h2>

      <p className="mb-8 max-w-md text-sm leading-6 text-[#667085]">
        Bạn chưa có sản phẩm nào trong giỏ hàng. Hãy khám phá các sản phẩm tuyệt
        vời và thêm vào giỏ hàng của bạn!
      </p>

      <button
        type="button"
        onClick={() => navigate('/')}
        className="inline-flex items-center gap-2 rounded-full bg-[#5367C9] px-6 py-3 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(83,103,201,0.25)] transition-all duration-200 hover:-translate-y-0.5 hover:bg-[#4659B8] hover:shadow-[0_8px_22px_rgba(83,103,201,0.3)]"
      >
        Khám phá sản phẩm
        <ArrowRight size={17} strokeWidth={2} />
      </button>
    </div>
  );
};

export default EmptyCart;
