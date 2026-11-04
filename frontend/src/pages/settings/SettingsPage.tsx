import { ArrowLeft, Bell, Check, ChevronRight, Languages, LockKeyhole, Palette, Type } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTheme, type ThemeName } from '../../context/ThemeContext';

const themes: { id: ThemeName; name: string; description: string; primary: string; soft: string }[] = [
  { id: 'indigo', name: 'Indigo Kala', description: 'Màu xanh tím hiện tại', primary: '#5367C9', soft: '#EEF1FF' },
  { id: 'ocean', name: 'Đại dương', description: 'Xanh biển dịu mắt', primary: '#087E9B', soft: '#E5F7FA' },
  { id: 'forest', name: 'Rừng xanh', description: 'Xanh lá thanh nhã', primary: '#39805B', soft: '#EAF6EF' },
  { id: 'rose', name: 'Hoa hồng', description: 'Hồng tím ấm áp', primary: '#B84F7A', soft: '#FCEEF4' },
  { id: 'amber', name: 'Nắng ấm', description: 'Cam vàng nổi bật', primary: '#B76B15', soft: '#FFF4E4' },
];

const suggestions = [
  { icon: Bell, title: 'Thông báo', description: 'Chọn thông báo đơn hàng, tin nhắn và khuyến mãi muốn nhận.' },
  { icon: LockKeyhole, title: 'Bảo mật tài khoản', description: 'Đổi mật khẩu, xác thực hai bước và quản lý phiên đăng nhập.' },
  { icon: Languages, title: 'Ngôn ngữ và khu vực', description: 'Tùy chỉnh ngôn ngữ, tiền tệ và cách hiển thị ngày giờ.' },
  { icon: Type, title: 'Trợ năng hiển thị', description: 'Điều chỉnh cỡ chữ, độ tương phản và chuyển động giao diện.' },
];

export default function SettingsPage({ audience = 'customer' }: { audience?: 'customer' | 'shop-owner' }) {
  const { theme, setTheme } = useTheme();
  const isOwner = audience === 'shop-owner';
  const backPath = isOwner ? '/seller' : '/';
  return <main className="mx-auto w-full max-w-[1120px] px-4 py-8 sm:px-6 sm:py-11">
    <Link to={backPath} className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-[var(--kala-primary)]"><ArrowLeft size={17}/> Quay lại {isOwner ? 'kênh người bán' : 'trang chủ'}</Link>
    <header className="mt-6 rounded-[26px] border border-[#e5e9f2] bg-[linear-gradient(120deg,#fff,#f2f5ff)] p-6 shadow-[0_12px_36px_rgba(34,51,95,.06)] sm:p-9">
      <span className="grid h-12 w-12 place-items-center rounded-2xl bg-[var(--kala-soft)] text-[var(--kala-primary)]"><Palette size={23}/></span>
      <h1 className="mt-4 text-2xl font-extrabold tracking-tight text-[#202642] sm:text-3xl">Cài đặt {isOwner ? 'cửa hàng' : 'tài khoản'}</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">Tùy chỉnh giao diện Kala theo sở thích. Màu sắc sẽ được lưu trên thiết bị này và áp dụng cho cả khu vực mua sắm lẫn quản lý cửa hàng.</p>
    </header>

    <section className="mt-7 rounded-[22px] border border-[#e6eaf2] bg-white p-5 shadow-[0_6px_24px_rgba(28,44,83,.04)] sm:p-7">
      <div className="flex items-start gap-3"><span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-[var(--kala-soft)] text-[var(--kala-primary)]"><Palette size={19}/></span><div><h2 className="text-base font-bold text-[#202642]">Màu giao diện</h2><p className="mt-1 text-sm text-slate-500">Chọn màu nhấn cho nút, liên kết và các chi tiết chính.</p></div></div>
      <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{themes.map(option => <button key={option.id} type="button" aria-pressed={theme === option.id} onClick={() => setTheme(option.id)} className={`flex items-center gap-3 rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 hover:shadow-md ${theme === option.id ? 'border-[var(--kala-primary)] ring-2 ring-[var(--kala-primary)]/15' : 'border-slate-200 hover:border-slate-300'}`}><span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl" style={{ background: option.soft, color: option.primary }}><span className="h-5 w-5 rounded-full" style={{ background: option.primary }}/></span><span className="min-w-0 flex-1"><b className="block text-sm text-[#202642]">{option.name}</b><span className="mt-0.5 block text-xs text-slate-500">{option.description}</span></span><span className="grid h-6 w-6 place-items-center rounded-full" style={theme === option.id ? { background: option.primary, color: 'white' } : { background: '#f1f3f7', color: '#94a3b8' }}>{theme === option.id ? <Check size={14}/> : <span className="h-1.5 w-1.5 rounded-full bg-current"/>}</span></button>)}</div>
      <div className="mt-5 flex flex-wrap items-center gap-3 rounded-2xl p-4" style={{ background: themes.find(item => item.id === theme)?.soft }}><span className="text-sm font-semibold text-slate-600">Xem trước</span><button className="rounded-xl px-4 py-2 text-sm font-bold text-white shadow-sm" style={{ background: themes.find(item => item.id === theme)?.primary }}>Nút chính</button><span className="text-sm font-semibold" style={{ color: themes.find(item => item.id === theme)?.primary }}>Liên kết Kala</span></div>
    </section>

    <section className="mt-7"><div className="mb-3"><h2 className="text-base font-bold text-[#202642]">Có thể bổ sung thêm</h2><p className="mt-1 text-sm text-slate-500">Một số cài đặt hữu ích cho các phiên bản tiếp theo.</p></div><div className="grid gap-3 sm:grid-cols-2">{suggestions.map(item => <article key={item.title} className="flex items-start gap-3 rounded-2xl border border-[#e6eaf2] bg-white p-4"><span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-slate-50 text-slate-500"><item.icon size={18}/></span><div className="min-w-0 flex-1"><h3 className="text-sm font-semibold text-[#202642]">{item.title}</h3><p className="mt-1 text-xs leading-5 text-slate-500">{item.description}</p></div><ChevronRight size={16} className="mt-1 text-slate-300"/></article>)}</div></section>
  </main>;
}
