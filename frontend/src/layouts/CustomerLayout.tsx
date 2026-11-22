import { Outlet } from 'react-router-dom';

import CustomerHeader from '../components/customer/header/CustomerHeader';
import CustomerFooter from '../components/customer/footer/CustomerFooter';
import ChatWidget from '../components/customer/chat/ChatWidget';

const CustomerLayout = () => {
  return (
    <div className="min-h-screen flex flex-col bg-[#F7FAFF]">
      <CustomerHeader />

      <main className="min-w-0 w-full flex-grow">
        <Outlet />
      </main>

      <CustomerFooter />
      <ChatWidget />
    </div>
  );
};

export default CustomerLayout;
