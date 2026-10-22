import {
  Mail,
  MapPin,
  Phone,
} from 'lucide-react';

import logo from '../../../assets/logo.png';
import FacebookIcon from '../../icons/FacebookIcon';
import InstagramIcon from '../../icons/InstagramIcon';

const CustomerFooter = () => {
  return (
    <footer className="border-t border-[#E4EAF5] bg-white">
      <div className="mx-auto w-full max-w-[1200px] px-4">
        {/* Main footer */}
        <div
          className="
            grid
            gap-10
            py-10
            sm:grid-cols-2
            lg:grid-cols-[1.5fr_1fr_1fr_1.2fr]
            lg:gap-12
            lg:py-12
          "
        >
          {/* Brand */}
          <div>
            <div className="mb-4">
              <img
                src={logo}
                alt="Kala"
                className="h-10 w-auto object-contain"
              />
            </div>

            <p
              className="
                max-w-[320px]
                text-sm
                leading-6
                text-[#7B8498]
              "
            >
              Kala mang đến trải nghiệm mua sắm trực tuyến
              đơn giản, thuận tiện và đa dạng sản phẩm.
            </p>

            {/* Social */}
            <div className="mt-5 flex items-center gap-2">
              <a
                href="#"
                aria-label="Facebook"
                className="
                  flex
                  h-9
                  w-9
                  items-center
                  justify-center
                  rounded-full
                  border
                  border-[#E2E8F4]
                  bg-white
                  text-[#66739A]
                  transition-all
                  duration-200
                  hover:-translate-y-0.5
                  hover:border-[#C7D4F0]
                  hover:bg-[#F3F6FF]
                  hover:text-[#5367C9]
                "
              >
                <FacebookIcon size={17} />
              </a>

              <a
                href="#"
                aria-label="Instagram"
                className="
                  flex
                  h-9
                  w-9
                  items-center
                  justify-center
                  rounded-full
                  border
                  border-[#E2E8F4]
                  bg-white
                  text-[#66739A]
                  transition-all
                  duration-200
                  hover:-translate-y-0.5
                  hover:border-[#C7D4F0]
                  hover:bg-[#F3F6FF]
                  hover:text-[#5367C9]
                "
              >
                <InstagramIcon size={17}  />
              </a>
            </div>
          </div>

          {/* Customer links */}
          <div>
            <h3
              className="
                mb-4
                text-sm
                font-semibold
                text-[#202642]
              "
            >
              Hỗ trợ khách hàng
            </h3>

            <ul className="space-y-3">
              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Trung tâm trợ giúp
                </a>
              </li>

              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Hướng dẫn mua hàng
                </a>
              </li>

              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Chính sách đổi trả
                </a>
              </li>

              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Chính sách bảo mật
                </a>
              </li>
            </ul>
          </div>

          {/* About Kala */}
          <div>
            <h3
              className="
                mb-4
                text-sm
                font-semibold
                text-[#202642]
              "
            >
              Về Kala
            </h3>

            <ul className="space-y-3">
              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Giới thiệu
                </a>
              </li>

              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Điều khoản sử dụng
                </a>
              </li>

              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Quy chế hoạt động
                </a>
              </li>

              <li>
                <a
                  href="#"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  Tuyển dụng
                </a>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3
              className="
                mb-4
                text-sm
                font-semibold
                text-[#202642]
              "
            >
              Liên hệ
            </h3>

            <ul className="space-y-4">
              <li className="flex items-start gap-3">
                <MapPin
                  size={17}
                  strokeWidth={1.8}
                  className="mt-0.5 shrink-0 text-[#5367C9]"
                />

                <span
                  className="
                    text-sm
                    leading-5
                    text-[#7B8498]
                  "
                >
                  Hà Nội, Việt Nam
                </span>
              </li>

              <li className="flex items-center gap-3">
                <Phone
                  size={17}
                  strokeWidth={1.8}
                  className="shrink-0 text-[#5367C9]"
                />

                <a
                  href="tel:19000000"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  1900 0000
                </a>
              </li>

              <li className="flex items-center gap-3">
                <Mail
                  size={17}
                  strokeWidth={1.8}
                  className="shrink-0 text-[#5367C9]"
                />

                <a
                  href="mailto:support@kala.vn"
                  className="
                    text-sm
                    text-[#7B8498]
                    transition-colors
                    hover:text-[#5367C9]
                  "
                >
                  support@kala.vn
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom */}
        <div
          className="
            flex
            flex-col
            gap-3
            border-t
            border-[#E8EDF6]
            py-5
            text-xs
            text-[#939BAD]
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <p>
            © {new Date().getFullYear()} Kala. All rights reserved.
          </p>

          <p>
            Nền tảng thương mại điện tử Kala
          </p>
        </div>
      </div>
    </footer>
  );
};

export default CustomerFooter;