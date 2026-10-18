import { Outlet } from 'react-router-dom';
import CustomerHeader from '../components/customer/header/CustomerHeader';
import CustomerFooter from '../components/customer/footer/CustomerFooter';
export default function PublicLayout() { 
    return <div className="flex min-h-screen flex-col bg-[#F7FAFF]">
        <CustomerHeader/>
        <main className="w-full flex-1">
            <Outlet/>
        </main>
        <CustomerFooter/>
    </div>; 
}
