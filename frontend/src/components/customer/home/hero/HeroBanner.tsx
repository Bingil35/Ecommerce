import { ArrowRight } from 'lucide-react';

const HeroBanner = () => {
  return (
    <section className="pt-5 sm:pt-6">
      <div
        className="
          relative
          overflow-hidden
          rounded-3xl
          bg-gradient-to-r
          from-[#EEF4FF]
          via-[#E9F0FF]
          to-[#DDE8FF]
          shadow-[0_8px_30px_rgba(80,105,160,0.08)]
        "
      >
        <div
          className="
            grid
            min-h-[260px]
            grid-cols-1
            items-center
            lg:min-h-[340px]
            lg:grid-cols-[1fr_0.9fr]
          "
        >
          {/* Content */}
          <div className="relative z-10 px-6 py-10 sm:px-10 lg:px-12">
            <span
              className="
                inline-flex
                items-center
                rounded-full
                bg-white/80
                px-3.5
                py-1.5
                text-xs
                font-semibold
                tracking-wide
                text-[#5367C9]
                shadow-sm
                sm:text-sm
              "
            >
              KHÁM PHÁ MỖI NGÀY
            </span>

            <h1
              className="
                mt-4
                max-w-[520px]
                text-3xl
                font-bold
                leading-tight
                tracking-tight
                text-[#202642]
                sm:text-4xl
                lg:text-[42px]
              "
            >
              Mua sắm dễ dàng,
              <br />
              <span className="text-[#5367C9]">
                chọn lựa thật vui
              </span>
            </h1>

            <p
              className="
                mt-4
                max-w-[470px]
                text-sm
                leading-6
                text-[#667085]
                sm:text-base
              "
            >
              Khám phá hàng nghìn sản phẩm từ những cửa hàng
              yêu thích và tìm thấy điều phù hợp dành cho bạn.
            </p>

            <button
              type="button"
              className="
                mt-6
                inline-flex
                items-center
                gap-2
                rounded-full
                bg-[#5367C9]
                px-5
                py-2.5
                text-sm
                font-semibold
                text-white
                shadow-[0_6px_18px_rgba(83,103,201,0.25)]
                transition-all
                duration-200
                hover:-translate-y-0.5
                hover:bg-[#4659B8]
                hover:shadow-[0_8px_22px_rgba(83,103,201,0.3)]
              "
            >
              Khám phá ngay
              <ArrowRight
                size={17}
                strokeWidth={2}
              />
            </button>
          </div>

          {/* Visual placeholder */}
          <div
            className="
              relative
              hidden
              h-full
              min-h-[300px]
              lg:block
            "
          >
            <div
              className="
                absolute
                right-[10%]
                top-1/2
                h-[230px]
                w-[230px]
                -translate-y-1/2
                rounded-full
                bg-white/40
                blur-[1px]
              "
            />

            <div
              className="
                absolute
                right-[14%]
                top-1/2
                h-[190px]
                w-[190px]
                -translate-y-1/2
                rounded-full
                border
                border-white/60
              "
            />
          </div>
        </div>
      </div>
    </section>
  );
};

export default HeroBanner;