export type MediaType = 'IMAGE' | 'VIDEO';

export function validateProductMedia(files: File[], type: MediaType, existingCount: number): string {
  const image = type === 'IMAGE';
  const types = image ? ['image/jpeg', 'image/png', 'image/webp'] : ['video/mp4', 'video/webm'];
  const extensions = image ? /\.(jpe?g|png|webp)$/i : /\.(mp4|webm)$/i;
  if (files.length + existingCount > (image ? 10 : 3)) return `Tối đa ${image ? 10 : 3} ${image ? 'ảnh' : 'video'}.`;
  if (files.some(file => !extensions.test(file.name) || (file.type && !types.includes(file.type))))
    return image ? 'Chỉ chấp nhận ảnh JPG, JPEG, PNG, WEBP.' : 'Chỉ chấp nhận video MP4, WEBM.';
  if (files.some(file => file.size === 0 || file.size > (image ? 5 : 50) * 1024 * 1024))
    return `Tệp phải có dữ liệu và không vượt quá ${image ? 5 : 50} MB.`;
  return '';
}
