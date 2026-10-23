import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import CustomerHeader from '../components/customer/header/CustomerHeader';
import CustomerFooter from '../components/customer/footer/CustomerFooter';

export default function PublicLayout() {
  const location = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [location.pathname, location.search]);

  return (
    <div className="flex min-h-screen flex-col bg-[#F7FAFF]">
      <CustomerHeader />
      <main className="w-full flex-1">
        <Outlet />
      </main>
      <CustomerFooter />
    </div>
  );
}
