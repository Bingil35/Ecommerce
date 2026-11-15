export interface VietnamProvince { code: string; name: string; }
export interface VietnamWard { code: string; name: string; type: string; }

const API = 'https://www.tinhthanhpho.com/api/v1';
let provincesPromise: Promise<VietnamProvince[]> | undefined;
const wardsByProvince = new Map<string, Promise<VietnamWard[]>>();

async function getData<T>(url: string): Promise<T[]> {
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('Không tải được dữ liệu địa giới hành chính.');
  const payload = await response.json() as { success?: boolean; data?: T[] };
  if (!payload.success || !Array.isArray(payload.data)) throw new Error('Dữ liệu địa giới hành chính không hợp lệ.');
  return payload.data;
}

export function getVietnamProvinces(): Promise<VietnamProvince[]> {
  provincesPromise ??= getData<{ code: string; name: string }>(`${API}/new-provinces?limit=100`)
    .then(items => items.map(item => ({ code: item.code, name: item.name })).sort((a, b) => a.name.localeCompare(b.name, 'vi')))
    .catch(error => { provincesPromise = undefined; throw error; });
  return provincesPromise;
}

export function getVietnamWards(provinceCode: string): Promise<VietnamWard[]> {
  if (!provinceCode) return Promise.resolve([]);
  let request = wardsByProvince.get(provinceCode);
  if (!request) {
    request = getData<{ code: string; name: string; type: string }>(`${API}/new-provinces/${encodeURIComponent(provinceCode)}/wards?limit=5000`)
      .then(items => items.map(item => ({ code: item.code, name: item.name, type: item.type })))
      .catch(error => { wardsByProvince.delete(provinceCode); throw error; });
    wardsByProvince.set(provinceCode, request);
  }
  return request;
}
