import {
  Search,
  ShoppingCart,
  Heart,
  User,
  ChevronDown,
  Package,
  LogOut,
  UserRound,
  Settings,
  Store,
} from 'lucide-react';

import {
  motion,
  useMotionValueEvent,
  useScroll,
} from 'framer-motion';

import { useEffect, useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { isShopOwner } from '../../../services/auth/roles';
import { BECOME_SELLER_ENTRY } from '../../../routes/BecomeSellerGuard';
import { useCart } from '../../../context/CartContext';
import { useWishlist } from '../../../context/WishlistContext';

import logo from '../../../assets/logo.png';

const CustomerHeader = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user, logout } = useAuth();
  const { itemCount } = useCart();
  const { count: wishlistCount } = useWishlist();
  const { scrollY } = useScroll();

  const [isHidden, setIsHidden] = useState(false);
  const [searchQuery, setSearchQuery] = useState(() => new URLSearchParams(location.search).get('q') ?? '');
  const [isAccountOpen, setIsAccountOpen] = useState(false);

  useEffect(() => {
    setSearchQuery(new URLSearchParams(location.search).get('q') ?? '');
  }, [location.search]);

  useMotionValueEvent(scrollY, 'change', (current) => {
    const previous = scrollY.getPrevious() ?? 0;
    const diff = current - previous;

    // Không xử lý các chuyển động quá nhỏ
    if (Math.abs(diff) < 2) {
      return;
    }

    // Đang ở gần đầu trang -> luôn hiện Header
    if (current <= 80) {
      setIsHidden(false);
      return;
    }

    // Cuộn xuống -> ẩn
    if (diff > 0) {
      setIsHidden(true);
    }

    // Cuộn lên -> hiện
    if (diff < 0) {
      setIsHidden(false);
    }
  });

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const query = searchQuery.trim();

    if (!query) {
      navigate('/search');
      return;
    }

    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  return (
    <motion.header
      className="sticky top-0 z-50 w-full border-b border-white/60 bg-white/70 backdrop-blur-xl backdrop-saturate-150"
      animate={{
        y: isHidden ? '-100%' : '0%',
      }}
      transition={{
        duration: 0.25,
        ease: 'easeOut',
      }}
    >
        <div className="mx-auto w-full max-w-[1200px] px-3 sm:px-4">
        <div className="flex h-[68px] items-center gap-3 sm:h-[72px] sm:gap-5 lg:gap-8">

          {/* Logo */}
          <div className="shrink-0">
            <img
              src={logo}
              alt="Kala"
              className="h-8 w-auto sm:h-9 lg:h-10"
              onClick={() => navigate('/')}
            />
          </div>

          {/* Search */}
            <div className="flex min-w-0 flex-1 justify-center">
            <form
                onSubmit={handleSearch}
                className="w-full max-w-[560px]"
            >
              <div className="relative">

                <button
                  type="submit"
                  aria-label="Tìm kiếm"
                  className="absolute left-3.5 top-1/2 z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-gray-400 transition-colors hover:text-[#171B7A]"
                >
                  <Search
                    size={19}
                    strokeWidth={1.8}
                  />
                </button>

                <input
                  type="text"
                  value={searchQuery}
                  onChange={(event) => {
                    setSearchQuery(event.target.value);
                  }}
                  placeholder="Tìm kiếm sản phẩm..."
                  aria-label="Tìm kiếm sản phẩm"
                    className="
                    h-10
                    w-full
                    rounded-full
                    border
                    border-[#DCE5F5]
                    bg-white/80
                    pl-11
                    pr-4
                    text-sm
                    text-[#2B2F42]
                    outline-none
                    transition-all
                    duration-200
                    placeholder:text-gray-400
                    hover:border-[#C5D4EE]
                    focus:border-[#9FB8E8]
                    focus:bg-white
                    focus:ring-2
                    focus:ring-[#DCE5F5]
                    sm:h-11
                    sm:pl-12
                    sm:pr-5
                    "
                />

              </div>
            </form>
          </div>

          {/* Actions */}
          <div className="flex shrink-0 items-center gap-0.5 sm:gap-2">

            {/* Cart */}
            <div className="relative">
            <button
                type="button"
                aria-label="Giỏ hàng"
                onClick={() => isAuthenticated ? navigate('/cart') : navigate('/login', { state: { from: location.pathname, notice: 'Đăng nhập để sử dụng giỏ hàng.' } })}
                className="flex h-9 w-9 sm:h-10 sm:w-10 items-center justify-center rounded-full
                text-[#4B5563]
                transition-colors
                duration-200
                hover:bg-white
                hover:text-[#171B7A]
                "
            >
                <ShoppingCart
                size={20}
                strokeWidth={1.8}
                />
            </button>

            {itemCount > 0 && (
            <span
                className="absolute -right-0.5 -top-0.5 flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-[#5B6FF5] px-1
                text-[10px]
                font-semibold
                leading-none
                text-white
                ring-2
                ring-white/70
                "
            >
                {itemCount > 99 ? '99+' : itemCount}
            </span>
            )}
            </div>

            {/* Wishlist */}
            <div className="relative">
              <button
                type="button"
                aria-label="Yêu thích"
                onClick={() => isAuthenticated ? navigate('/wishlist') : navigate('/login', { state: { from: '/wishlist', notice: 'Đăng nhập để xem danh sách yêu thích.' } })}
                className="
                  flex
                  h-9
                  w-9
                  sm:h-10
                  sm:w-10
                  items-center
                  justify-center
                  rounded-full
                  text-[#4B5563]
                  transition-colors
                  duration-200
                  hover:bg-white
                  hover:text-[#171B7A]
                "
              >
                <Heart
                  size={20}
                  strokeWidth={1.8}
                />
              </button>

              {wishlistCount > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-[#E5334F] px-1 text-[10px] font-semibold leading-none text-white ring-2 ring-white/70">
                  {wishlistCount > 99 ? '99+' : wishlistCount}
                </span>
              )}
            </div>

            {/* Account / Guest login */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  type="button"
                  aria-label="Tài khoản"
                  aria-expanded={isAccountOpen}
                  onClick={() => setIsAccountOpen((previous) => !previous)}
                  className="flex h-9 items-center gap-2 rounded-full px-2.5 text-[#4B5563] transition-colors duration-200 hover:bg-white hover:text-[#171B7A] sm:h-10"
                >
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#EEF3FF] text-[#5B6FF5]">
                    <User size={17} strokeWidth={1.8} />
                  </span>
                  <span className="hidden max-w-[120px] truncate text-sm font-medium text-[#374151] lg:block">
                    {user?.fullName || 'Tài khoản'}
                  </span>
                  <ChevronDown size={15} strokeWidth={1.8} className={`hidden text-gray-400 transition-transform duration-200 lg:block ${isAccountOpen ? 'rotate-180' : ''}`} />
                </button>

                {isAccountOpen && (
                  <div className="absolute right-0 top-[calc(100%+10px)] z-50 w-[260px] overflow-hidden rounded-2xl border border-[#E5EAF4] bg-white/95 shadow-[0_12px_40px_rgba(61,82,120,0.12)] backdrop-blur-xl">
                    <div className="border-b border-[#EEF1F6] px-4 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#EEF3FF] text-[#5B6FF5]">
                          <UserRound size={19} strokeWidth={1.8} />
                        </div>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-[#252A3A]">{user?.fullName || 'Tài khoản Kala'}</p>
                          <p className="truncate text-xs text-gray-500">{user?.email || 'Chưa có email'}</p>
                        </div>
                      </div>
                    </div>
                    <div className="p-1.5">
                      <button type="button" onClick={() => { setIsAccountOpen(false); navigate('/account'); }} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-[#4B5563] transition-colors hover:bg-[#F5F7FC] hover:text-[#171B7A]">
                        <UserRound size={17} strokeWidth={1.8} /><span>Thông tin</span>
                      </button>
                      <button type="button" onClick={() => { setIsAccountOpen(false); navigate('/orders'); }} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-[#4B5563] transition-colors hover:bg-[#F5F7FC] hover:text-[#171B7A]">
                        <Package size={17} strokeWidth={1.8} /><span>Đơn mua</span>
                      </button>
                      <button type="button" onClick={() => { setIsAccountOpen(false); navigate('/account/settings'); }} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-[#4B5563] transition-colors hover:bg-[#F5F7FC] hover:text-[#171B7A]">
                        <Settings size={17} strokeWidth={1.8} /><span>Cài đặt</span>
                      </button>
                    </div>
                    <div className="border-t border-[#EEF1F6] p-1.5">
                      <button
                        type="button"
                        onClick={() => {
                          setIsAccountOpen(false);
                          if (isShopOwner(user)) {
                            navigate('/seller');
                          } else {
                            navigate('/seller/register', { state: { entry: BECOME_SELLER_ENTRY } });
                          }
                        }}
                        className="flex w-full items-center gap-3 rounded-xl border border-[#CFE0FB] bg-[#F1F6FF] px-3 py-2.5 text-left text-sm font-medium text-[#2F6BEA] transition-colors hover:bg-[#E6EFFF]"
                      >
                        <Store size={17} strokeWidth={1.8} /><span>{isShopOwner(user) ? 'Kênh người bán' : 'Mở gian hàng'}</span>
                      </button>
                    </div>
                    <div className="border-t border-[#EEF1F6] p-1.5">
                      <button type="button" onClick={() => { setIsAccountOpen(false); logout(); navigate('/', { replace: true }); }} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-[#6B7280] transition-colors hover:bg-[#FFF5F5] hover:text-[#DC5A5A]">
                        <LogOut size={17} strokeWidth={1.8} /><span>Đăng xuất</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <button
                type="button"
                onClick={() => navigate('/login')}
                className="ml-1 rounded-full bg-[#5367C9] px-3.5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-[#4659B8] sm:px-4"
              >
                Đăng nhập
              </button>
            )}
          </div>
        </div>
      </div>
    </motion.header>
  );
};

export default CustomerHeader;