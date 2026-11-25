import { Route, Routes } from 'react-router-dom';
import ShopOwnerLayout from '../layouts/ShopOwnerLayout';
import ShopOwnerOrderStatusPage from '../pages/shop-owner/ShopOwnerOrderStatusPage';
import ShopOwnerOrdersPage from '../pages/shop-owner/ShopOwnerOrdersPage';
import ShopOwnerOrderDetailPage from '../pages/shop-owner/ShopOwnerOrderDetailPage';
import ShopOwnerReviewsPage from '../pages/shop-owner/ShopOwnerReviewsPage';
import ShopOwnerProductStatsPage from '../pages/shop-owner/ShopOwnerProductStatsPage';
import RequireShopOwner from './RequireShopOwner';

export default function ShopOwnerRoutes() {
  return <Routes><Route element={<RequireShopOwner />}><Route element={<ShopOwnerLayout />}>
    <Route path="/shop-owner/orders" element={<ShopOwnerOrdersPage />} />
    <Route path="/shop-owner/orders/:orderId" element={<ShopOwnerOrderDetailPage />} />
    <Route path="/shop-owner/reviews" element={<ShopOwnerReviewsPage />} />
    <Route path="/shop-owner/product-stats" element={<ShopOwnerProductStatsPage />} />
    <Route path="/shop-owner/orders/status" element={<ShopOwnerOrderStatusPage />} />
  </Route></Route></Routes>;
}
