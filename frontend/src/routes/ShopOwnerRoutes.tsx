import { Route, Routes } from 'react-router-dom';
import ShopOwnerLayout from '../layouts/ShopOwnerLayout';
import ShopOwnerOrderStatusPage from '../pages/shop-owner/ShopOwnerOrderStatusPage';

export default function ShopOwnerRoutes() {
  return <Routes><Route element={<ShopOwnerLayout />}><Route path="/shop-owner/orders/status" element={<ShopOwnerOrderStatusPage />} /></Route></Routes>;
}
