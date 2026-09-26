import { Outlet } from 'react-router-dom';

const AuthLayout = () => {
  return (
    <main className="min-h-screen bg-[#F7F9FC]">
      <Outlet />
    </main>
  );
};

export default AuthLayout;