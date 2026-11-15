import { useDialog, useToast, getFeedbackError } from '../../context/FeedbackContext';
import { useCallback, useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { Check, Plus, Search, Tags, Upload, X } from 'lucide-react';
import axiosClient from '../../services/api/axiosClient';
import { mediaUrl } from '../../services/api/mediaUrl';

type Brand = { id: string; name: string; slug: string; logoUrl: string | null; createdAt: string };
type Envelope<T> = { data: T };

export default function AdminBrandsPage() {
  const toast = useToast();
  const { confirm: askConfirm } = useDialog();
  const [rows, setRows] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [modal, setModal] = useState(false);
  const [editing, setEditing] = useState<Brand | null>(null);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows((await axiosClient.get<Envelope<Brand[]>>('/admin/brands')).data.data || []);
    } catch (e: any) {
      toast.error(getFeedbackError(e));
      setError(e?.response?.data?.message || 'Không tải được thương hiệu.');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => rows.filter(r => `${r.name} ${r.slug}`.toLowerCase().includes(query.toLowerCase())), [rows, query]);
  const pageSize = 10;
  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const shown = filtered.slice((page - 1) * pageSize, page * pageSize);
  const open = (brand?: Brand) => { setEditing(brand || null); setLogoFile(null); setError(''); setModal(true); };

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    setBusy(true);
    setError('');
    try {
      let logoUrl = editing?.logoUrl || '';
      if (logoFile) {
        const uploadData = new FormData();
        uploadData.append('file', logoFile);
        const uploaded = await axiosClient.post<Envelope<{ url: string }>>('/admin/brands/upload', uploadData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        logoUrl = uploaded.data.data.url;
      }
      const body = {
        name: String(data.get('name') || '').trim(),
        slug: String(data.get('slug') || '').trim().toLowerCase().replace(/\s+/g, '-'),
        logoUrl: logoUrl || null,
      };
      if (editing) await axiosClient.put(`/admin/brands/${editing.id}`, body);
      else await axiosClient.post('/admin/brands', body);
      setModal(false);
      toast.success(editing ? 'Đã cập nhật thương hiệu.' : 'Đã tạo thương hiệu.');
      void load();
    } catch (err: any) {
      toast.error(getFeedbackError(err));
      setError(err?.response?.data?.message || 'Không lưu được thương hiệu.');
    } finally {
      setBusy(false);
    }
  };

  const remove = (brand: Brand) => askConfirm({
    title: 'Xóa thương hiệu?',
    message: `Xóa thương hiệu “${brand.name}”?`,
    confirmText: 'Xóa thương hiệu',
    variant: 'danger',
    successMessage: 'Đã xóa thương hiệu.',
    onConfirm: async () => { await axiosClient.delete(`/admin/brands/${brand.id}`); void load(); },
  });

  return <div className="admin-page">
    <div className="admin-page-heading"><div><div className="admin-eyebrow"><span>QUẢN TRỊ</span><b>/</b> THƯƠNG HIỆU</div><h1>Quản lý thương hiệu</h1><p>Quản lý danh sách thương hiệu gắn với sản phẩm trên sàn.</p></div><button className="admin-primary-btn" onClick={() => open()}><Plus size={17}/> Thêm thương hiệu</button></div>
    {error && !modal && <div className="admin-alert">{error}</div>}
    <section className="admin-panel">
      <div className="admin-panel-toolbar"><label className="admin-table-search"><Search size={17}/><input placeholder="Tìm theo tên hoặc slug…" value={query} onChange={e => { setQuery(e.target.value); setPage(1); }}/></label><span className="admin-total-chip"><b>{rows.length}</b> thương hiệu</span></div>
      {loading ? <div className="admin-loading"><span/>Đang tải danh sách</div> : <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>THƯƠNG HIỆU</th><th>SLUG</th><th>NGÀY TẠO</th><th/></tr></thead><tbody>{shown.map(brand => <tr key={brand.id}><td><div className="admin-identity">{brand.logoUrl ? <img className="admin-category-image" src={mediaUrl(brand.logoUrl)} alt={`Logo ${brand.name}`}/> : <span className="admin-category-image-placeholder"><Tags size={16}/></span>}<span><b>{brand.name}</b></span></div></td><td className="admin-muted">/{brand.slug}</td><td>{brand.createdAt ? new Date(brand.createdAt).toLocaleDateString('vi-VN') : '—'}</td><td><div className="admin-row-actions"><button onClick={() => open(brand)}>Sửa</button><button className="danger" onClick={() => void remove(brand)}>Xóa</button></div></td></tr>)}</tbody></table>
        {!shown.length && <div className="admin-empty"><span><Tags size={20}/></span><b>Chưa có thương hiệu</b><p>Tạo thương hiệu đầu tiên để phân loại sản phẩm.</p></div>}
      </div>}
      <div className="admin-table-footer"><span>Hiển thị <b>{filtered.length ? (page - 1) * pageSize + 1 : 0}–{Math.min(page * pageSize, filtered.length)}</b> / {filtered.length} kết quả</span><span><button disabled={page <= 1} onClick={() => setPage(page - 1)}>←</button><b className="current">{page} / {pageCount}</b><button disabled={page >= pageCount} onClick={() => setPage(page + 1)}>→</button></span></div>
    </section>
    {modal && <div className="admin-modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) setModal(false); }}><form className="admin-modal" onSubmit={submit}>
      <div className="admin-modal-head"><span><small>{editing ? 'CẬP NHẬT' : 'TẠO MỚI'}</small><b>Thương hiệu</b></span><button type="button" onClick={() => setModal(false)}><X size={19}/></button></div>
      <div className="admin-modal-body"><label className="admin-field"><span>Tên thương hiệu <i>*</i></span><input name="name" required defaultValue={editing?.name} placeholder="VD: Kala Studio"/></label><label className="admin-field"><span>Slug <i>*</i></span><input name="slug" required defaultValue={editing?.slug} placeholder="kala-studio"/></label>
        <BrandLogoPicker currentLogo={editing?.logoUrl} onChange={setLogoFile} required={!editing?.logoUrl}/>
        {error && <div className="admin-form-error">{error}</div>}
      </div>
      <div className="admin-modal-foot"><button type="button" className="admin-cancel-btn" onClick={() => setModal(false)}>Hủy</button><button className="admin-primary-btn" disabled={busy}>{busy ? 'Đang lưu…' : <><Check size={16}/> {editing ? 'Lưu thay đổi' : 'Tạo thương hiệu'}</>}</button></div>
    </form></div>}
  </div>;
}

function BrandLogoPicker({ currentLogo, onChange, required }: { currentLogo?: string | null; onChange: (file: File | null) => void; required: boolean }) {
  const [preview, setPreview] = useState('');
  useEffect(() => {
    if (!preview) return;
    return () => URL.revokeObjectURL(preview);
  }, [preview]);
  const choose = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] || null;
    setPreview(file ? URL.createObjectURL(file) : '');
    onChange(file);
  };
  const image = preview || (currentLogo ? mediaUrl(currentLogo) : '');
  return <div className="admin-field"><span>Ảnh thương hiệu <i>*</i></span><label className="admin-banner-dropzone admin-brand-logo-dropzone">
    <input type="file" name="logoFile" accept="image/*" required={required} onChange={choose}/>
    {image ? <img src={image} alt="Xem trước logo thương hiệu"/> : <><span className="admin-banner-upload-icon"><Upload size={19}/></span><b>Chọn ảnh thương hiệu</b><small>Định dạng ảnh PNG, JPG hoặc WEBP</small></>}
  </label></div>;
}
