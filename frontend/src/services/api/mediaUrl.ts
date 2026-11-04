import axiosClient from './axiosClient';

export function mediaUrl(value: string | null | undefined): string {
  if (!value) return '';
  if (/^(https?:|data:|blob:)/i.test(value)) return value;
  try {
    const origin = new URL(axiosClient.defaults.baseURL || 'http://localhost:8080/api').origin;
    return new URL(value.startsWith('/') ? value : `/${value}`, origin).toString();
  } catch {
    return value;
  }
}
