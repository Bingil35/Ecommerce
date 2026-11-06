import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Users, Store, Tags, Image, Menu, Search, LogOut, ShieldCheck, TicketPercent } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import NotificationBell from '../components/notifications/NotificationBell';

const links = [
  { to:'/admin', label:'Tổng quan', icon:LayoutDashboard, end:true },
  { to:'/admin/users', label:'Người dùng', icon:Users },
  { to:'/admin/shops', label:'Cửa hàng', icon:Store },
  { to:'/admin/categories', label:'Danh mục', icon:Tags },
  { to:'/admin/coupons', label:'Mã giảm giá', icon:TicketPercent },
  { to:'/admin/banners', label:'Banner hệ thống', icon:Image },
  { to:'/admin/brands', label:'Thương hiệu', icon:Tags },
  { to:'/admin/coupon-abuse', label:'Rủi ro voucher', icon:ShieldCheck },
];

export default function AdminLayout() {
  const [open,setOpen]=useState(false); const {user,logout}=useAuth(); const navigate=useNavigate();
  return <div className="admin-shell">
    <aside className={`admin-sidebar ${open?'admin-sidebar-open':''}`}>
      <div className="admin-brand"><span className="admin-brand-mark">K</span><span>Kala<span className="admin-brand-sub">CONTROL CENTER</span></span></div>
      <div className="admin-workspace"><span className="admin-workspace-icon"><ShieldCheck size={17}/></span><span><b>Quản trị hệ thống</b><small>Không gian quản lý</small></span><span className="admin-online"/></div>
      <div className="admin-nav-label">WORKSPACE</div>
      <nav className="admin-nav">{links.map(({to,label,icon:Icon,end})=><NavLink to={to} end={end} key={to} onClick={()=>setOpen(false)} className={({isActive})=>`admin-nav-link ${isActive?'active':''}`}><Icon size={18}/><span>{label}</span>{to==='/admin/shops'&&<i>!</i>}</NavLink>)}</nav>
      <div className="admin-sidebar-bottom"><div className="admin-help-card"><div className="admin-help-icon">✦</div><b>Cần hỗ trợ?</b><p>Trung tâm trợ giúp dành cho quản trị viên.</p><button onClick={()=>navigate('/account/settings')}>Xem hướng dẫn <span>→</span></button></div><div className="admin-profile"><div className="admin-avatar">{user?.fullName?.slice(0,1)||'A'}</div><span><b>{user?.fullName||'Administrator'}</b><small>Quản trị viên</small></span><button title="Đăng xuất" onClick={()=>{logout();navigate('/login',{replace:true});}}><LogOut size={17}/></button></div></div>
    </aside>
    {open&&<button aria-label="Đóng menu" className="admin-backdrop" onClick={()=>setOpen(false)}/>}
    <div className="admin-main"><header className="admin-topbar"><button className="admin-mobile-menu" aria-label="Mở menu" onClick={()=>setOpen(!open)}><Menu size={20}/></button><div className="admin-breadcrumb"><span>Workspace</span><b>/</b><strong>Quản trị hệ thống</strong></div><div className="admin-top-actions"><label className="admin-global-search"><Search size={16}/><input placeholder="Tìm nhanh..."/><kbd>⌘ K</kbd></label><NotificationBell audience="admin"/><div className="admin-top-divider"/><span className="admin-date">Thứ tư, 04 tháng 11</span></div></header><main className="admin-content"><Outlet/></main><footer className="admin-footer">© 2026 Kala Commerce <span>Hệ thống quản trị · v1.0</span></footer></div>
  </div>;
}
