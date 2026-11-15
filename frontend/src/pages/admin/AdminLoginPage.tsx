import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast, getFeedbackError } from '../../context/FeedbackContext';
import { hasRole, ROLES } from '../../services/auth/roles';
import type { LoginResponse } from '../../services/auth/authTypes';
import axiosClient from '../../services/api/axiosClient';
import logo from '../../assets/logo.png';

export default function AdminLoginPage() {
  const { user, isLoading: authLoading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const from = (location.state as { from?: string } | null)?.from || '/admin';

  if (authLoading) return null;
  if (hasRole(user, ROLES.ADMIN)) return <Navigate to={from} replace />;

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    if (!identifier.trim() || !password) { setError('Nhập email hoặc số điện thoại và mật khẩu để tiếp tục.'); return; }
    setSubmitting(true);
    try {
      const response = await axiosClient.post<{ data: LoginResponse }>('/auth/login', { identifier: identifier.trim(), password });
      const result = response.data.data;
      if (!hasRole(result.user, ROLES.ADMIN)) { setError('Tài khoản này không có quyền quản trị.'); return; }
      login(result.accessToken, result.user, remember);
      toast.success('Đăng nhập quản trị thành công.');
      navigate(from, { replace: true });
    } catch (cause) {
      toast.error(getFeedbackError(cause));
      setError((cause as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Không thể đăng nhập. Vui lòng kiểm tra thông tin.');
    } finally { setSubmitting(false); }
  };

  return <main className="admin-login-page"><section className="admin-login-card"><div className="admin-login-brand"><img src={logo} alt="Kala"/><span><ShieldCheck size={17}/> CỔNG QUẢN TRỊ</span></div><div className="admin-login-heading"><div className="admin-login-icon"><LockKeyhole size={22}/></div><h1>Đăng nhập quản trị</h1><p>Sử dụng tài khoản quản trị Kala để tiếp tục.</p></div>
    <form onSubmit={submit} className="admin-login-form"><label>Email hoặc số điện thoại<input autoComplete="username" value={identifier} onChange={event => setIdentifier(event.target.value)} placeholder="admin@kala.vn"/></label><label>Mật khẩu<span className="admin-login-password"><input autoComplete="current-password" type={showPassword ? 'text' : 'password'} value={password} onChange={event => setPassword(event.target.value)} placeholder="Nhập mật khẩu"/><button type="button" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShowPassword(value => !value)}>{showPassword ? <EyeOff size={17}/> : <Eye size={17}/>}</button></span></label>
      <label className="admin-login-remember"><input type="checkbox" checked={remember} onChange={event => setRemember(event.target.checked)}/> Ghi nhớ đăng nhập</label>{error && <p role="alert" className="admin-login-error">{error}</p>}<button disabled={submitting} className="admin-login-submit">{submitting ? 'Đang xác thực...' : 'Đăng nhập'}</button>
    </form><p className="admin-login-footer">Truy cập dành riêng cho quản trị viên · Kala Commerce</p></section></main>;
}
