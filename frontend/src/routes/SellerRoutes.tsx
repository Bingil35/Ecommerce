import { Route, Routes } from 'react-router-dom';

import AuthLayout from '../layouts/AuthLayout';
import ShopOwnerLayout from '../layouts/ShopOwnerLayout';

import SellerLoginPage from '../pages/seller/SellerLoginPage';
import SellerRegisterShopPage from '../pages/seller/SellerRegisterShopPage';
import SellerDashboardPage from '../pages/seller/SellerDashboardPage';
import SellerProductsPage from '../pages/seller/SellerProductsPage';
import SellerProductDetailPage from '../pages/seller/SellerProductDetailPage';
import SellerCreateProductPage from '../pages/seller/SellerCreateProductPage';
import SellerEditProductPage from '../pages/seller/SellerEditProductPage';
import SellerInventoryPage from '../pages/seller/SellerInventoryPage';
import SellerInventoryDetailPage from '../pages/seller/SellerInventoryDetailPage';
import SellerPromotionsPage from '../pages/seller/SellerPromotionsPage';
import SellerShopInfoPage from '../pages/seller/SellerShopInfoPage';
import ChatPage from '../pages/chat/ChatPage';
import SettingsPage from '../pages/settings/SettingsPage';

import BecomeSellerGuard from './BecomeSellerGuard';
import RequireShopOwner from './RequireShopOwner';

const SellerRoutes = () => {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/shop-owner/login" element={<SellerLoginPage />} />
        <Route
          path="/shop-owner/register"
          element={
            <BecomeSellerGuard>
              <SellerRegisterShopPage />
            </BecomeSellerGuard>
          }
        />
      </Route>

      <Route element={<RequireShopOwner />}>
        <Route element={<ShopOwnerLayout />}>
          <Route path="/shop-owner" element={<SellerDashboardPage />} />
          <Route path="/shop-owner/products" element={<SellerProductsPage />} />
          <Route path="/shop-owner/products/:productId" element={<SellerProductDetailPage />} />
          <Route path="/shop-owner/products/new" element={<SellerCreateProductPage />} />
          <Route path="/shop-owner/products/:productId/edit" element={<SellerEditProductPage />} />
          <Route path="/shop-owner/inventory" element={<SellerInventoryPage />} />
          <Route path="/shop-owner/inventory/:productId" element={<SellerInventoryDetailPage />} />
          <Route path="/shop-owner/promotions" element={<SellerPromotionsPage />} />
          <Route path="/shop-owner/shop" element={<SellerShopInfoPage />} />
          <Route path="/shop-owner/chat" element={<ChatPage role="shop-owner" />} />
          <Route path="/shop-owner/settings" element={<SettingsPage audience="shop-owner" />} />
        </Route>
      </Route>
    </Routes>
  );
};

export default SellerRoutes;
