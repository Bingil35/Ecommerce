import { useState } from 'react';
import { FileSpreadsheet, UploadCloud } from 'lucide-react';
import { getFeedbackError } from '../../context/FeedbackContext';
import { sellerProductApi, type ExcelPreview } from '../../services/seller/productApi';

export default function ProductExcelImportDialog({ onClose, onImported }: { onClose: () => void; onImported: (count: number) => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ExcelPreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [dragging, setDragging] = useState(false);

  const selectFile = (selected: File | null) => {
    setPreview(null);
    if (selected && (!selected.name.toLowerCase().endsWith('.xlsx') || selected.size > 5 * 1024 * 1024 || selected.size === 0)) {
      setFile(null);
      setError('Vui lòng chọn file .xlsx không rỗng, dung lượng tối đa 5 MiB.');
      return;
    }
    setError('');
    setFile(selected);
  };

  const download = async () => {
    setBusy(true); setError('');
    try {
      const url = URL.createObjectURL(await sellerProductApi.excelTemplate());
      const link = document.createElement('a'); link.href = url; link.download = 'mau-nhap-san-pham.xlsx'; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) { setError(getFeedbackError(e)); } finally { setBusy(false); }
  };
  const check = async () => {
    if (!file) return;
    setBusy(true); setError(''); setPreview(null);
    try { setPreview(await sellerProductApi.excelPreview(file)); }
    catch (e) { setError(getFeedbackError(e)); } finally { setBusy(false); }
  };
  const confirm = async () => {
    if (!preview?.previewToken) return;
    setBusy(true); setError('');
    try { const result = await sellerProductApi.excelConfirm(preview.previewToken); onImported(result.created); }
    catch (e) {
      const response = (e as { response?: { data?: { data?: ExcelPreview } } }).response?.data?.data;
      if (response?.errors) setPreview(response);
      setError(getFeedbackError(e));
    } finally { setBusy(false); }
  };
  return <div className="kp-overlay" onMouseDown={e => { if (!busy && e.target === e.currentTarget) onClose(); }}>
    <div className="kp-dialog" role="dialog" aria-modal="true" aria-labelledby="excel-import-title">
      <div className="kp-dialog-head"><h2 id="excel-import-title">Nhập sản phẩm từ Excel</h2><button type="button" onClick={onClose} disabled={busy} aria-label="Đóng">×</button></div>
      <div className="kp-form">
        <p>Tải file mẫu, điền dữ liệu theo hướng dẫn trong sheet Guide, sau đó kiểm tra trước khi nhập. Tối đa 100 sản phẩm và 5 MiB.</p>
        <div><button type="button" className="kp-btn" onClick={() => void download()} disabled={busy}>Tải file mẫu</button></div>
        <div>
          <span className="mb-2 block text-sm font-semibold text-[#2e3a5e]">File Excel</span>
          <label
            className={`relative flex min-h-[150px] cursor-pointer items-center justify-center rounded-xl border-2 border-dashed px-5 py-5 text-center transition-colors focus-within:ring-2 focus-within:ring-[#5367C9] ${dragging ? 'border-[#5367C9] bg-[#EEF2FF]' : 'border-[#CCD6ED] bg-[#F8FAFF] hover:border-[#5367C9] hover:bg-[#F2F5FF]'} ${busy ? 'cursor-not-allowed opacity-60' : ''}`}
            onDragOver={e => { e.preventDefault(); if (!busy) setDragging(true); }}
            onDragLeave={e => { if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setDragging(false); }}
            onDrop={e => { e.preventDefault(); setDragging(false); if (!busy) selectFile(e.dataTransfer.files[0] ?? null); }}
          >
            <input className="sr-only" type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" disabled={busy} onChange={e => { selectFile(e.target.files?.[0] ?? null); e.target.value = ''; }}/>
            <span className="mx-auto flex h-11 w-11 items-center justify-center rounded-xl bg-[#E8EDFF] text-[#5367C9]">{file ? <FileSpreadsheet size={22} /> : <UploadCloud size={22} />}</span>
            <span className="mt-1 block max-w-full break-all text-sm font-semibold text-[#26355C]">{file ? file.name : 'Chọn file hoặc kéo thả vào đây'}</span>
            <span className="block text-xs font-normal text-[#77839B]">{file ? `${(file.size / 1024).toFixed(1)} KB · Nhấn để chọn file khác` : 'Định dạng .xlsx · Tối đa 5 MiB'}</span>
          </label>
        </div>
        {error && <p className="kp-alert kp-error" role="alert">{error}</p>}
        {preview && <div aria-live="polite"><p>Tổng: {preview.total} sản phẩm · Hợp lệ: {preview.valid} · Lỗi: {preview.invalid} · Biến thể: {preview.variantCount}</p>
          {preview.errors.length > 0 && <ul style={{ maxHeight: 180, overflow: 'auto', paddingLeft: 20 }}>{preview.errors.map((issue, i) => <li key={i}>{issue.sheet}{issue.row ? `, dòng ${issue.row}` : ''}{issue.column ? `, cột ${issue.column}` : ''}: {issue.message}</li>)}</ul>}
          {preview.products.length > 0 && <ul style={{ maxHeight: 140, overflow: 'auto', paddingLeft: 20 }}>{preview.products.map(p => <li key={p.productCode}>{p.productCode} — {p.name} ({p.variantCount} biến thể)</li>)}</ul>}
          {preview.previewToken && <p className="kp-hint">Sản phẩm sau khi nhập sẽ ở trạng thái chờ duyệt.</p>}
        </div>}
        <div className="kp-dialog-actions"><button type="button" className="kp-btn" onClick={onClose} disabled={busy}>Hủy</button><button type="button" className="kp-btn" onClick={() => void check()} disabled={busy || !file}>Kiểm tra file</button><button type="button" className="kp-btn kp-primary" onClick={() => void confirm()} disabled={busy || !preview?.previewToken}>Xác nhận nhập</button></div>
      </div>
    </div>
  </div>;
}
