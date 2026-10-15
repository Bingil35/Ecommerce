import { Outlet } from 'react-router-dom';

import CustomerHeader from '../components/customer/header/CustomerHeader';
import CustomerFooter from '../components/customer/footer/CustomerFooter';

const CustomerLayout = () => {
  return (
    <div className="min-h-screen flex flex-col bg-[#F7FAFF]">
      <CustomerHeader />

      <main className="flex-grow w-full">
        <Outlet />
      </main>

      <CustomerFooter />
    </div>
  );
};

export default CustomerLayout;