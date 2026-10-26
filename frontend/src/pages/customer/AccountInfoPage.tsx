import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, LoaderCircle, Mail, Phone, UserRound, Pencil, Save, X, LockKeyhole, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { customerApi } from '../../services/customer/customerApi';
import type { UserResponse } from '../../services/auth/authTypes';

const fieldClass = 'mt-1.5 min-h-11 w-full rounded-xl border border-[#E5EAF4] bg-[#F8FAFD] px-3.5 text-sm text-[#283044] outline-none focus:border-[#6979DF] focus:ring-2 focus:ring-[#6979DF]/15';
const AccountInfoPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading, user } = useAuth();
  const [profile, setProfile] = useState<UserResponse | null>(user);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordNotice, setPasswordNotice] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);
  useEffect(() => {
    if (authLoading) return;
    if (!isAuthenticated) { navigate('/login', { replace: true, state: { from: '/account', notice: 'Đăng nhập để xem thông tin tài khoản.' } }); return; }
    let active = true;
    customerApi.getProfile().then(data => { if (active) { setProfile(data); setFullName(data.fullName || ''); setEmail(data.email || ''); setPhone(data.phone || ''); } })
      .catch(() => { if (active) setError('Không thể tải thông tin mới nhất. Vui lòng thử lại sau.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [authLoading, isAuthenticated, navigate]);
  const value = (text: string | null | undefined) => text?.trim() || 'Chưa cập nhật';
  const beginEdit = () => { setFullName(profile?.fullName || ''); setEmail(profile?.email || ''); setPhone(profile?.phone || ''); setError(''); setNotice(''); setEditing(true); };
  const cancelEdit = () => { setFullName(profile?.fullName || ''); setEmail(profile?.email || ''); setPhone(profile?.phone || ''); setError(''); setEditing(false); };
  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setError(''); setNotice('');
    if (!fullName.trim()) { setError('Vui lòng nhập họ và tên.'); return; }
    if (!email.trim()) { setError('Vui lòng nhập email.'); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) { setError('Email không đúng định dạng.'); return;}
    if (phone.trim() && !/^[0-9+().\\s-]{7,20}$/.test(phone.trim())) { setError('Số điện thoại không đúng định dạng.'); return; }
    setSaving(true);
    try { const updated = await customerApi.updateProfile({ fullName: fullName.trim(), email: email.trim(), phone: phone.trim() || null }); setProfile(updated); setFullName(updated.fullName || ''); setEmail(updated.email || ''); setPhone(updated.phone || ''); setEditing(false); setNotice('Thông tin tài khoản đã được cập nhật.'); }
    catch (e: any) { setError(e?.response?.data?.message || 'Không thể cập nhật thông tin. Vui lòng thử lại.'); }
    finally { setSaving(false); }
  };
  const changePassword = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setPasswordError(''); setPasswordNotice('');
    if (newPassword.length < 8) { setPasswordError('Mật khẩu mới phải có ít nhất 8 ký tự.'); return; }
    if (newPassword !== confirmPassword) { setPasswordError('Mật khẩu xác nhận không khớp.'); return; }
    if (currentPassword === newPassword) { setPasswordError('Mật khẩu mới phải khác mật khẩu hiện tại.'); return; }
    setPasswordSaving(true);
    try { await customerApi.changePassword({ currentPassword, newPassword }); setCurrentPassword(''); setNewPassword(''); setConfirmPassword(''); setPasswordNotice('Đổi mật khẩu thành công.'); }
    catch (e: any) { setPasswordError(e?.response?.data?.message || 'Không thể đổi mật khẩu. Vui lòng thử lại.'); }
    finally { setPasswordSaving(false); }
  };
  return <main className="min-h-[65vh] bg-[#F7F9FD] px-3 py-8 sm:px-5 sm:py-10"><div className="mx-auto max-w-[1000px]">
    <div className="mb-6"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#6577D8]">Tài khoản Kala</p><h1 className="mt-2 text-2xl font-bold tracking-tight text-[#20263A] sm:text-3xl">Thông tin người dùng</h1><p className="mt-2 text-sm text-[#697386]">Xem và cập nhật thông tin cá nhân của bạn.</p></div>
    {error && <div role="alert" className="mb-5 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"><AlertCircle size={19} className="mt-0.5 shrink-0"/>{error}</div>}
    {notice && <div role="status" className="mb-5 flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"><CheckCircle2 size={18}/>{notice}</div>}
    <section className="overflow-hidden rounded-2xl border border-[#E6EAF3] bg-white shadow-[0_8px_28px_rgba(40,55,90,0.05)]">
      <div className="flex flex-col gap-4 border-b border-[#EEF1F6] bg-gradient-to-r from-[#F5F7FF] to-white px-5 py-6 sm:flex-row sm:items-center sm:px-7"><div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-[#E0E7FF] bg-[#EEF2FF] text-[#5869CF]">{profile?.avatarUrl ? <img src={profile.avatarUrl} alt="Ảnh đại diện" className="h-full w-full object-cover"/> : <UserRound size={29}/>}</div><div className="min-w-0 flex-1"><h2 className="truncate text-lg font-bold text-[#242A3D]">{value(profile?.fullName)}</h2><p className="mt-1 truncate text-sm text-[#6B7280]">{value(profile?.email)}</p><div className="mt-3 flex flex-wrap gap-2">{profile?.status && <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${profile.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}><CheckCircle2 size={13}/>{profile.status === 'ACTIVE' ? 'Đang hoạt động' : 'Không hoạt động'}</span>}</div></div>{loading && <div className="flex items-center gap-2 text-xs text-[#6B7280]"><LoaderCircle size={16} className="animate-spin"/>Đang đồng bộ</div>}</div>
      <div className="px-5 py-6 sm:px-7 sm:py-7"><div className="mb-5 flex flex-wrap items-center justify-between gap-3"><div><h3 className="font-semibold text-[#252B3D]">Thông tin cơ bản</h3><p className="mt-1 text-xs text-[#7A8293]">Bạn có thể cập nhật họ tên, email và số điện thoại của tài khoản.</p></div>{!editing && <button type="button" onClick={beginEdit} disabled={loading || !profile} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#5969D8] px-4 text-sm font-semibold text-white hover:bg-[#4858C8] disabled:opacity-50"><Pencil size={15}/>Chỉnh sửa</button>}</div>
      {editing ? <form onSubmit={save} className="space-y-5"><div className="grid gap-5 sm:grid-cols-2">
<div><label htmlFor="fullName" className="text-xs font-semibold text-[#697386]">Họ và tên <span className="text-rose-500">*</span></label><input id="fullName" className={fieldClass} value={fullName} onChange={e => setFullName(e.target.value)} maxLength={255} required autoComplete="name"/></div>
<div><label htmlFor="email" className="text-xs font-semibold text-[#697386]">Email <span className="text-rose-500">*</span></label><input id="email" type="email" className={fieldClass} value={email} onChange={e => setEmail(e.target.value)} maxLength={255} required autoComplete="email"/></div>
<div><label htmlFor="phone" className="text-xs font-semibold text-[#697386]">Số điện thoại</label><input id="phone" type="tel" className={fieldClass} value={phone} onChange={e => setPhone(e.target.value)} maxLength={20} autoComplete="tel"/></div>
</div><div className="flex flex-wrap gap-3"><button type="submit" disabled={saving} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#5969D8] px-4 text-sm font-semibold text-white hover:bg-[#4858C8] disabled:opacity-60">{saving ? <LoaderCircle size={16} className="animate-spin"/> : <Save size={16}/>} {saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button><button type="button" onClick={cancelEdit} disabled={saving} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-[#E1E6F0] px-4 text-sm font-semibold text-[#4B5568] hover:bg-gray-50"><X size={16}/>Hủy</button></div></form> : <div className="grid gap-5 sm:grid-cols-2"><div><label className="text-xs font-semibold text-[#697386]">Họ và tên</label><div className={fieldClass + ' flex items-center'}><UserRound size={16} className="mr-2.5 shrink-0 text-[#8A94A8]"/>{value(profile?.fullName)}</div></div><div><label className="text-xs font-semibold text-[#697386]">Email</label><div className={fieldClass + ' flex items-center'}><Mail size={16} className="mr-2.5 shrink-0 text-[#8A94A8]"/>{value(profile?.email)}</div></div><div><label className="text-xs font-semibold text-[#697386]">Số điện thoại</label><div className={fieldClass + ' flex items-center'}><Phone size={16} className="mr-2.5 shrink-0 text-[#8A94A8]"/>{value(profile?.phone)}</div></div></div>}
      </div></section>
    <section className="mt-6 overflow-hidden rounded-2xl border border-[#E6EAF3] bg-white shadow-[0_8px_28px_rgba(40,55,90,0.05)]">
      <div className="border-b border-[#EEF1F6] px-5 py-5 sm:px-7"><div className="flex items-center gap-3"><div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#EEF2FF] text-[#5969D8]"><LockKeyhole size={19}/></div><div><h2 className="font-semibold text-[#252B3D]">Đổi mật khẩu</h2><p className="mt-1 text-xs text-[#7A8293]">Nhập mật khẩu hiện tại để xác nhận danh tính của bạn.</p></div></div></div>
      <form onSubmit={changePassword} className="space-y-4 px-5 py-6 sm:px-7">
        {passwordError && <div role="alert" className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700"><AlertCircle size={17} className="mt-0.5 shrink-0"/>{passwordError}</div>}
        {passwordNotice && <div role="status" className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700"><CheckCircle2 size={17}/>{passwordNotice}</div>}
        <div className="grid gap-4 sm:grid-cols-2">
          <div><label htmlFor="currentPassword" className="text-xs font-semibold text-[#697386]">Mật khẩu hiện tại <span className="text-rose-500">*</span></label><input id="currentPassword" type={showPasswords ? 'text' : 'password'} className={fieldClass} value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required autoComplete="current-password"/></div>
          <div><label htmlFor="newPassword" className="text-xs font-semibold text-[#697386]">Mật khẩu mới <span className="text-rose-500">*</span></label><input id="newPassword" type={showPasswords ? 'text' : 'password'} className={fieldClass} value={newPassword} onChange={e => setNewPassword(e.target.value)} minLength={8} maxLength={72} required autoComplete="new-password"/><p className="mt-1 text-xs text-[#7A8293]">Từ 8 đến 72 ký tự.</p></div>
          <div><label htmlFor="confirmPassword" className="text-xs font-semibold text-[#697386]">Xác nhận mật khẩu mới <span className="text-rose-500">*</span></label><input id="confirmPassword" type={showPasswords ? 'text' : 'password'} className={fieldClass} value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} minLength={8} maxLength={72} required autoComplete="new-password"/></div>
        </div>
        <button type="button" onClick={() => setShowPasswords(v => !v)} className="inline-flex items-center gap-2 text-xs font-medium text-[#5969D8]">{showPasswords ? <EyeOff size={15}/> : <Eye size={15}/>} {showPasswords ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}</button>
        <div><button type="submit" disabled={passwordSaving} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#5969D8] px-4 text-sm font-semibold text-white hover:bg-[#4858C8] disabled:opacity-60">{passwordSaving ? <LoaderCircle size={16} className="animate-spin"/> : <LockKeyhole size={16}/>} {passwordSaving ? 'Đang cập nhật...' : 'Đổi mật khẩu'}</button></div>
      </form>
    </section>
  </div></main>;
};
export default AccountInfoPage;
