import { Outlet } from "react-router-dom";

const ShopOwnerLayout = () => {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100">
            <header className="w-full bg-green-500 text-white py-4">
                <div className="container mx-auto px-4">
                    <span className="text-xl font-bold">Kala - Shop Owner</span>
                </div>
            </header>
            <main className="flex-grow w-full">
                <Outlet />
            </main>
        </div>
    );
};

export default ShopOwnerLayout;
