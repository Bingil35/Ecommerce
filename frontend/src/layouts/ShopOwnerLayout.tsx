import { Outlet, Link } from 'react-router-dom';
const ShopOwnerLayout = () => (
  <div className="min-h-screen flex flex-col bg-[#F7F9FD]">
    <header className="w-full border-b border-[#E7EBF3] bg-white">
      <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-3 px-4 py-4">
        <Link to="/shop-owner/orders/status" className="text-lg font-bold text-[#5367C9]">Kala · Kênh người bán</Link>
        <nav className="flex gap-4 text-sm font-medium text-[#58647B]"><Link to="/shop-owner/orders/status" className="hover:text-[#5367C9]">Cập nhật trạng thái đơn</Link><Link to="/" className="hover:text-[#5367C9]">Xem website</Link></nav>
      </div>
    </header>
    <main className="flex-grow w-full"><Outlet /></main>
  </div>
);
export default ShopOwnerLayout;
