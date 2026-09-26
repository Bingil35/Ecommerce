import { Outlet } from "react-router-dom";

const PublicLayout = () => {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100">
            <header className="w-full bg-blue-500 text-white py-4">
                <div className="container mx-auto px-4">
                    <span className="text-xl font-bold">Kala</span>
                </div>
            </header>

            <main className="flex-grow w-full">
                <Outlet />
            </main>

            <footer className="w-full bg-gray-800 text-white py-4">
                <div className="container mx-auto px-4 text-center">
                    &copy; {new Date().getFullYear()} Kala. All rights reserved.
                </div>
            </footer>
        </div>
    );
};

export default PublicLayout;