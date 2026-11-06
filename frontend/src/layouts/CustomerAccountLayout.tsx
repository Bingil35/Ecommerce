import { Navigate, NavLink, Outlet, useLocation } from 'react-router-dom';
import { MapPin, Settings, Store, UserRound, Eye, Bell } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const tabs=[
  {to:'/account',label:'Thông tin',icon:UserRound,end:true},
  {to:'/account/addresses',label:'Địa chỉ',icon:MapPin},
  {to:'/account/followed-shops',label:'Shop đang theo dõi',icon:Store},
  {to:'/account/recently-viewed',label:'Đã xem gần đây',icon:Eye},
  {to:'/account/notifications',label:'Thông báo',icon:Bell},
  {to:'/account/settings',label:'Cài đặt',icon:Settings},
];

export default function CustomerAccountLayout(){
 const {isAuthenticated,isLoading}=useAuth();const location=useLocation();
 if(isLoading)return null;
 if(!isAuthenticated)return <Navigate to="/login" replace state={{from:location.pathname,notice:'Đăng nhập để quản lý tài khoản.'}}/>;
 return <main className="min-h-[70vh] bg-[#F7F9FD] px-3 py-7 sm:px-5 sm:py-9"><div className="mx-auto grid w-full max-w-[1160px] items-start gap-5 lg:grid-cols-[230px_minmax(0,1fr)] lg:gap-7"><aside className="rounded-2xl border border-[#E6EAF3] bg-white p-3 shadow-[0_6px_24px_rgba(40,55,90,.04)]"><div className="px-3 pb-3 pt-2"><p className="text-xs font-bold uppercase tracking-wider text-[#7C87A1]">Tài khoản của tôi</p></div><nav className="flex gap-1 overflow-x-auto lg:flex-col">{tabs.map(({to,label,icon:Icon,end})=><NavLink key={to} to={to} end={end} className={({isActive})=>`flex shrink-0 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition lg:w-full ${isActive?'bg-[#F0F2FF] text-[#596BD0]':'text-[#697386] hover:bg-slate-50 hover:text-[#35405A]'}`}><Icon size={17}/><span>{label}</span></NavLink>)}</nav></aside><section className="min-w-0"><Outlet/></section></div></main>;
}
