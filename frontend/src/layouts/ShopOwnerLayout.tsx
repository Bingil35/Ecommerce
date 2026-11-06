import { useEffect, useRef, useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { ChevronDown, ClipboardList, Home, LogOut, MessageCircle, Package, Settings, Star, Store, Tag, Truck, Warehouse } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { shopApi } from '../services/shop/shopApi';
import type { ShopResponse } from '../services/shop/shopTypes';
import { mediaUrl } from '../services/api/mediaUrl';
import { useChatUnread } from '../hooks/useChatUnread';
import NotificationBell from '../components/notifications/NotificationBell';

const items = [
  { to: '/seller', text: 'Trang chủ', icon: Home },
  { to: '/seller/products', text: 'Sản phẩm', icon: Package },
  { to: '/shop-owner/orders', text: 'Đơn hàng', icon: ClipboardList },
  { to: '/shop-owner/reviews', text: 'Đánh giá', icon: Star },
  { to: '/seller/inventory', text: 'Kho hàng', icon: Warehouse },
  { to: '/seller/promotions', text: 'Khuyến mãi', icon: Tag },
  { to: '/seller/shop', text: 'Thông tin cửa hàng', icon: Store },
];
const future = [
  { text: 'Vận chuyển', icon: Truck },
];

// Container dùng chung cho Header, Navigation và nội dung các trang Shop Owner (cùng max-width 1200px với trang khách hàng).
const layoutStyles = '.seller-container{width:100%;max-width:1200px;margin-inline:auto;padding-inline:24px;box-sizing:border-box}.kso-root{min-height:100vh;background:#F7F9FC;color:#111827}.kso-header{background:#fff;border-bottom:1px solid #E2E8F0}.kso-header-in{height:58px;display:flex;align-items:center;justify-content:space-between;gap:16px}.kso-logo{color:#5367C9;font-size:28px;font-weight:800;letter-spacing:-1.6px;line-height:1;text-decoration:none}.kso-right{display:flex;align-items:center;gap:14px}.kso-icon-btn{width:36px;height:36px;display:grid;place-items:center;border:0;border-radius:50%;background:transparent;color:#334155;cursor:pointer}.kso-icon-btn:hover{background:#F1F5F9}.kso-account{position:relative}.kso-account-btn{display:flex;align-items:center;gap:10px;border:0;background:transparent;padding:4px 6px;border-radius:999px;cursor:pointer;color:inherit;text-align:left}.kso-account-btn:hover{background:#F1F5F9}.kso-avatar{display:grid;place-items:center;width:34px;height:34px;border-radius:50%;background:#EEF3FF;color:#5367C9;flex-shrink:0}.kso-who{font-size:13px;line-height:1.3;display:flex;flex-direction:column}.kso-who b{font-weight:700}.kso-who span{color:#64748B;font-size:12px}.kso-menu{position:absolute;right:0;top:calc(100% + 6px);min-width:200px;background:#fff;border:1px solid #E2E8F0;border-radius:12px;box-shadow:0 10px 30px #0f172a1a;padding:6px;z-index:50}.kso-menu button{display:flex;align-items:center;gap:10px;width:100%;border:0;background:transparent;padding:9px 12px;border-radius:8px;font-size:14px;color:#334155;cursor:pointer;text-align:left}.kso-menu button:hover{background:#F1F5F9}.kso-nav{background:#fff;border-bottom:1px solid #E2E8F0}.kso-nav-in{height:44px;display:flex;align-items:center;justify-content:space-between;gap:4px;overflow-x:auto;white-space:nowrap;scrollbar-width:thin}.kso-nav-item{flex:0 0 auto;display:inline-flex;align-items:center;gap:7px;height:34px;padding:0 12px;border-radius:8px;text-decoration:none;font-size:13px;font-weight:600;color:#334155}.kso-nav-item:hover{background:#F8FAFC}.kso-nav-item.active{color:#5367C9;background:#EEF3FF}.kso-nav-item.disabled{color:#94A3B8;font-weight:500;cursor:not-allowed}.kso-nav-item.disabled:hover{background:transparent}@media(max-width:1279px){.seller-container{padding-inline:20px}}@media(max-width:767px){.seller-container{padding-inline:16px}.kso-who{display:none}.kso-logo{font-size:24px}.kso-nav-in{justify-content:flex-start}}';

export default function ShopOwnerLayout() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const hasUnreadChat = useChatUnread('shop-owner');
  const [shop, setShop] = useState<ShopResponse | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const accountRef = useRef<HTMLDivElement>(null);

  useEffect(() => { void shopApi.getMyShop().then(setShop).catch(() => {}); }, []);

  useEffect(() => {
    if (!menuOpen) return;
    const close = (event: MouseEvent) => { if (!accountRef.current?.contains(event.target as Node)) setMenuOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [menuOpen]);

  return <div className="kso-root">
    <style>{layoutStyles}</style>
    <header className="kso-header">
      <div className="seller-container kso-header-in">
        <span className="kso-logo">Kala°</span>
        <div className="kso-right">
          <button type="button" className="kso-icon-btn relative" aria-label={hasUnreadChat ? 'Tin nhắn mới chưa đọc' : 'Tin nhắn'} title={hasUnreadChat ? 'Bạn có tin nhắn mới' : 'Tin nhắn'} onClick={() => navigate('/seller/chat')}><MessageCircle size={20}/>{hasUnreadChat && <span aria-hidden="true" className="absolute right-0.5 top-0.5 h-2.5 w-2.5 rounded-full border-2 border-white bg-rose-500"/>}</button>
          <NotificationBell audience="shop-owner"/>
          <div className="kso-account" ref={accountRef}>
            <button type="button" className="kso-account-btn" aria-haspopup="menu" aria-expanded={menuOpen} onClick={() => setMenuOpen(o => !o)}>
              <span className="kso-avatar">{shop?.logoUrl ? <img src={mediaUrl(shop.logoUrl)} alt="" className="h-full w-full rounded-full object-cover"/> : <Store size={18}/>}</span>
              <span className="kso-who"><b>{shop?.shopName || 'Cửa hàng'}</b><span>{user?.fullName || 'Chủ cửa hàng'}</span></span>
              <ChevronDown size={16}/>
            </button>
            {menuOpen && <div className="kso-menu" role="menu">
              <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); navigate('/'); }}><Home size={16}/> Trang khách hàng</button>
              <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); navigate('/seller/settings'); }}><Settings size={16}/> Cài đặt</button>
              <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); logout(); navigate('/seller/login', { replace: true }); }}><LogOut size={16}/> Đăng xuất</button>
            </div>}
          </div>
        </div>
      </div>
    </header>
    <nav className="kso-nav">
      <div className="seller-container kso-nav-in">
        {items.map(({ to, text, icon: Icon }) => <NavLink key={to} to={to} end={to === '/seller'} className={({ isActive }) => 'kso-nav-item' + (isActive ? ' active' : '')}><Icon size={17}/>{text}</NavLink>)}
        {future.map(({ text, icon: Icon }) => <span key={text} title="Chưa triển khai trang này" className="kso-nav-item disabled"><Icon size={17}/>{text}</span>)}
      </div>
    </nav>
    <main><Outlet/></main>
  </div>;
}
