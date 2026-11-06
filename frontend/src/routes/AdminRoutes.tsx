import { Route, Routes } from 'react-router-dom';
import AdminLayout from '../layouts/AdminLayout';
import RequireAdmin from './RequireAdmin';
import AdminPage from '../pages/admin/AdminPage';
import AdminBrandsPage from '../pages/admin/AdminBrandsPage';
import AdminCouponAbusePage from '../pages/admin/AdminCouponAbusePage';

export default function AdminRoutes() {
  return <Routes><Route element={<RequireAdmin />}><Route element={<AdminLayout />}>
    <Route path="/admin" element={<AdminPage section="dashboard" />} />
    <Route path="/admin/users" element={<AdminPage section="users" />} />
    <Route path="/admin/shops" element={<AdminPage section="shops" />} />
    <Route path="/admin/categories" element={<AdminPage section="categories" />} />
    <Route path="/admin/coupons" element={<AdminPage section="coupons" />} />
    <Route path="/admin/banners" element={<AdminPage section="banners" />} />
    <Route path="/admin/brands" element={<AdminBrandsPage />} />
    <Route path="/admin/coupon-abuse" element={<AdminCouponAbusePage />} />
  </Route></Route></Routes>;
}
