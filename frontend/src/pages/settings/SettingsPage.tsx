import { ArrowLeft, Check, Palette } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTheme, type ThemeName } from '../../context/ThemeContext';

const themes: { id: ThemeName; name: string; color: string; soft: string }[] = [
  { id: 'indigo', name: 'Tím chàm', color: '#5367C9', soft: '#EEF1FF' },
  { id: 'ocean', name: 'Đại dương', color: '#087E9B', soft: '#E5F7FA' },
  { id: 'forest', name: 'Rừng xanh', color: '#39805B', soft: '#EAF6EF' },
  { id: 'rose', name: 'Hoa hồng', color: '#B84F7A', soft: '#FCEEF4' },
  { id: 'amber', name: 'Nắng ấm', color: '#B76B15', soft: '#FFF4E4' },
];

export default function SettingsPage({ audience = 'customer' }: { audience?: 'customer' | 'shop-owner' }) {
  const { theme, setTheme } = useTheme();
  const owner = audience === 'shop-owner';
  const selected = themes.find(item => item.id === theme) ?? themes[0];

  return <main className="mx-auto w-full max-w-[900px] px-4 py-7 sm:px-6 sm:py-10">
    <Link to={owner ? '/seller' : '/account'} className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-[var(--kala-primary)]"><ArrowLeft size={17}/> Quay lại</Link>
    <header className="mt-5 flex items-center gap-4">
      <span className="grid h-12 w-12 place-items-center rounded-2xl bg-[var(--kala-soft)] text-[var(--kala-primary)]"><Palette size={22}/></span>
      <div><h1 className="text-2xl font-extrabold tracking-tight text-[#202642]">Cài đặt giao diện</h1><p className="mt-1 text-sm text-slate-500">Chọn màu sắc bạn muốn sử dụng.</p></div>
    </header>

    <section className="mt-6 rounded-2xl border border-[#e6eaf2] bg-white p-5 shadow-sm sm:p-6">
      <h2 className="text-sm font-bold text-[#202642]">Màu chủ đạo</h2>
      <div className="mt-4 grid gap-2 sm:grid-cols-2">
        {themes.map(option => <button key={option.id} type="button" aria-pressed={theme === option.id} onClick={() => setTheme(option.id)} className={`flex items-center gap-3 rounded-xl border p-3 text-left transition hover:bg-slate-50 ${theme === option.id ? 'border-[var(--kala-primary)] ring-2 ring-[var(--kala-primary)]/15' : 'border-slate-200'}`}>
          <span className="grid h-10 w-10 place-items-center rounded-xl" style={{ background: option.soft, color: option.color }}><span className="h-5 w-5 rounded-full" style={{ background: option.color }}/></span>
          <span className="flex-1 text-sm font-semibold text-[#303950]">{option.name}</span>
          {theme === option.id && <Check size={17} style={{ color: option.color }}/>}
        </button>)}
      </div>
      <div className="mt-5 flex flex-wrap items-center gap-3 rounded-xl p-4" style={{ background: selected.soft }}>
        <span className="text-sm font-semibold text-slate-600">Xem trước</span>
        <button type="button" className="rounded-lg px-4 py-2 text-sm font-bold text-white" style={{ background: selected.color }}>Nút chính</button>
        <span className="text-sm font-semibold" style={{ color: selected.color }}>Liên kết</span>
      </div>
      <p className="mt-3 text-xs text-slate-400">Lựa chọn được lưu trên thiết bị này.</p>
    </section>
  </main>;
}
