import { useEffect, useState, type FormEvent } from 'react';
import { ChevronDown, Package, Search } from 'lucide-react';
import { dashboardApi, type ShopOwnerProductStats } from '../../services/seller/dashboardApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import { getApiError } from '../../services/checkout/checkoutApi';

const money = (value: number | string | null) => value == null ? '—' : `${Number(value).toLocaleString('vi-VN')} ₫`;
const styles = `.sps-page{padding:26px 0 40px;color:#17345d}.sps-heading{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:20px}.sps-title-group{min-width:230px}.sps-title{margin:0 0 5px;font-size:24px;font-weight:750;color:#17345d;white-space:nowrap}.sps-sub{margin:0;color:#70819c;font-size:13px;line-height:1.45}.sps-tools{display:flex;justify-content:flex-end;gap:10px;align-items:flex-end;margin:0;flex-wrap:nowrap}.sps-field{display:grid;gap:5px;color:#405776;font-size:12px}.sps-field input,.sps-field select{height:38px;min-width:180px;border:1px solid #cdd8e8;border-radius:6px;background:white;padding:0 12px;color:#29466e;font:inherit;outline:none}.sps-field input:focus,.sps-field select:focus{border-color:#8092df;box-shadow:0 0 0 3px #5367c91a}.sps-field select{min-width:210px;appearance:none;padding-right:36px;cursor:pointer}.sps-select-wrap{position:relative;display:block}.sps-select-chevron{position:absolute;right:12px;top:50%;transform:translateY(-50%);width:16px;height:16px;color:#536783;pointer-events:none}.sps-table-wrap{overflow:auto;border:1px solid #d8e3f2;border-radius:12px;background:#fff}.sps-table{width:100%;min-width:1180px;border-collapse:collapse;font-size:13px}.sps-table th{height:40px;background:#f3f7fc;color:#50698c;font-weight:600;text-align:right;padding:0 10px;white-space:nowrap;border-bottom:1px solid #dce6f2}.sps-table th:first-child{text-align:left;width:320px}.sps-table td{height:82px;text-align:right;padding:10px;border-bottom:1px solid #e5ebf3;white-space:nowrap}.sps-table tr:last-child td{border-bottom:0}.sps-table td:first-child{text-align:left;white-space:normal}.sps-product{display:flex;align-items:center;gap:12px;min-width:290px}.sps-thumb{width:50px;height:50px;flex:0 0 auto;border:1px solid #e3e9f1;border-radius:7px;display:grid;place-items:center;overflow:hidden;color:#91a1b8}.sps-thumb img{width:100%;height:100%;object-fit:cover}.sps-name{color:#1761ae;font-weight:600;line-height:1.45}.sps-muted{display:block;color:#8290a5;font-size:11px;margin-top:4px}.sps-pagination{display:flex;justify-content:space-between;align-items:center;padding:14px 2px;color:#64748b;font-size:13px}.sps-pagination button{border:1px solid #d6dfeb;background:#fff;border-radius:6px;padding:7px 12px;color:#36547c}.sps-pagination button:disabled{opacity:.45}.sps-state{text-align:center;padding:36px;color:#75839a}.sps-error{color:#b42318}.sps-note{font-size:12px;color:#8290a5;margin-top:10px}@media(max-width:960px){.sps-heading{align-items:stretch;flex-direction:column;gap:12px}.sps-tools{justify-content:flex-start;flex-wrap:wrap}}@media(max-width:700px){.sps-tools{justify-content:stretch}.sps-field{flex:1}.sps-field input,.sps-field select{width:100%;min-width:0}}`;

export default function ShopOwnerProductStatsPage() {
  const [data, setData] = useState<ShopOwnerProductStats | null>(null);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('revenueLifetime');
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void dashboardApi.getProductStats({ keyword: search || undefined, sort, page, size: 20 })
      .then(result => { if (active) { setData(result); setError(''); } })
      .catch(e => { if (active) setError(getApiError(e, 'Chưa tải được thống kê sản phẩm.')); });
    return () => { active = false; };
  }, [search, sort, page]);

  const submit = (event: FormEvent) => { event.preventDefault(); setPage(0); setSearch(keyword.trim()); };
  return <div className="seller-container sps-page"><style>{styles}</style>
    <div className="sps-heading">
      <div className="sps-title-group"><h1 className="sps-title">Thống kê sản phẩm</h1>
        <p className="sps-sub">Số liệu cửa hàng {data?.shopName ? <b>{data.shopName}</b> : 'của bạn'}. {'Doanh thu v\u00e0 l\u01b0\u1ee3ng b\u00e1n t\u00ednh t\u1eeb \u0111\u01a1n \u0111\u00e3 giao.'}</p>
      </div>
      <form className="sps-tools" onSubmit={submit}>
        <label className="sps-field">Tên sản phẩm<input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="Nhập tên sản phẩm" /></label>
        <button aria-label="Tìm kiếm" className="h-[38px] rounded-md bg-[#5367C9] px-3 text-white" type="submit"><Search size={17}/></button>
        <label className="sps-field">Sắp xếp kết quả<span className="sps-select-wrap"><select value={sort} onChange={e => { setSort(e.target.value); setPage(0); }}>
          <option value="revenueLifetime">Doanh thu lũy kế cao nhất</option><option value="revenue30Days">Doanh thu 30 ngày</option>
          <option value="soldLifetime">Đã bán lũy kế</option><option value="sold30Days">Đã bán 30 ngày</option><option value="salesPace">Nhịp bán cao nhất</option>
          <option value="rating">Đánh giá cao nhất</option><option value="priceAsc">Giá thấp đến cao</option><option value="priceDesc">Giá cao đến thấp</option><option value="name">Tên sản phẩm</option>
        </select><ChevronDown className="sps-select-chevron" aria-hidden="true" /></span></label>
      </form>
    </div>

    {error ? <div className="sps-state sps-error">{error}</div> : !data ? <div className="sps-state">Đang tải thống kê…</div> : <>
      <div className="sps-table-wrap"><table className="sps-table"><thead><tr><th>Sản phẩm</th><th>Giá hiện tại</th><th>Đã bán lũy kế</th><th>Đã bán 30 ngày</th><th>Doanh thu lũy kế</th><th>Doanh thu 30 ngày</th><th>Đánh giá</th><th>Lượt đánh giá</th><th>{'Nh\u1ecbp b\u00e1n'}</th></tr></thead>
        <tbody>{data.items.map(item => <tr key={item.id}><td><div className="sps-product"><span className="sps-thumb">{item.thumbnailUrl ? <img src={mediaUrl(item.thumbnailUrl)} alt=""/> : <Package size={22}/>}</span><span><span className="sps-name">{item.name}</span><span className="sps-muted">{item.slug}</span></span></div></td><td>{money(item.currentPrice)}</td><td>{item.soldLifetime.toLocaleString('vi-VN')}</td><td>{item.sold30Days.toLocaleString('vi-VN')}</td><td>{money(item.revenueLifetime)}</td><td>{money(item.revenue30Days)}</td><td>{Number(item.ratingAverage).toLocaleString('vi-VN')}</td><td>{item.ratingCount.toLocaleString('vi-VN')}</td><td>{Number(item.salesPace).toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td></tr>)}
        {!data.items.length && <tr><td colSpan={9} className="sps-state">Không tìm thấy sản phẩm.</td></tr>}</tbody></table></div>
      <div className="sps-pagination"><span>Hiển thị {data.totalElements === 0 ? 0 : page * data.size + 1}–{Math.min((page + 1) * data.size, data.totalElements)} / {data.totalElements} sản phẩm</span><div className="flex gap-2"><button disabled={page <= 0} onClick={() => setPage(p => p - 1)}>Trước</button><button disabled={page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Tiếp</button></div></div>
    </>}
  </div>;
}
