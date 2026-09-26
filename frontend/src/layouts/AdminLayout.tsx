import { Outlet } from "react-router-dom";

const AdminLayout = () => {
    return (
        <div className="min-h-screen flex flex-col">
            <header className="w-full bg-blue-500 text-white py-4">
                <div className="container mx-auto px-4">
                    <span className="text-xl font-bold">Kala - Admin</span>
                </div>
            </header>
            <main className="flex-grow w-full">
                <Outlet />
            </main>
        </div>
    );
};

export default AdminLayout;
