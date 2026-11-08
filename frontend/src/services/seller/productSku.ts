// Match ProductVariant.sku (varchar(255)); manual SKUs may also use underscores.
export const SKU_MAX_LENGTH = 255;
export const SKU_PATTERN = /^[A-Za-z0-9][A-Za-z0-9_-]*$/;

// keepTrailingSeparator: giữ dấu "-" ở cuối khi đang gõ dở (vd "IP17-"),
// để người dùng gõ tiếp được; khi blur gọi lại không tham số để cắt sạch.
export function normalizeSkuPart(value: string, keepTrailingSeparator = false): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'D')
    .toUpperCase()
    .trim()
    .replace(/(\d)\s+(MM|CM|GB|TB|MB|KG|G|ML|L)\b/g, '$1$2')
    .replace(/[^A-Z0-9]+/g, '-')
    .replace(keepTrailingSeparator ? /^-+/g : /^-+|-+$/g, '');
}

export function generateVariantSku(base: string, values: string[]): string {
  // Không dùng .map(normalizeSkuPart): map truyền thêm index làm tham số thứ 2,
  // index > 0 là truthy và sẽ vô tình bật keepTrailingSeparator.
  const parts = [base, ...values].map(part => normalizeSkuPart(part));
  if (parts.some(part => !part))
    throw new Error('Mã SKU gốc hoặc giá trị thuộc tính không có ký tự hợp lệ để tạo SKU.');
  return parts.join('-');
}

export function validateVariantSkus<T extends { key: string; sku: string }>(variants: T[]): Map<string, string> {
  const errors = new Map<string, string>();
  const bySku = new Map<string, string[]>();
  for (const variant of variants) {
    const sku = variant.sku.trim();
    if (!sku) errors.set(variant.key, 'SKU không được để trống.');
    else if (sku.length > SKU_MAX_LENGTH) errors.set(variant.key, `SKU không được vượt quá ${SKU_MAX_LENGTH} ký tự.`);
    else if (!SKU_PATTERN.test(sku)) errors.set(variant.key, 'SKU phải bắt đầu bằng chữ/số và chỉ chứa chữ Latin, số, dấu - hoặc _.');
    if (sku) {
      const key = sku.toUpperCase();
      bySku.set(key, [...(bySku.get(key) ?? []), variant.key]);
    }
  }
  for (const [sku, keys] of bySku) {
    if (keys.length > 1) keys.forEach(key => errors.set(key, `SKU trùng: ${sku}. Hãy sửa SKU hoặc giá trị thuộc tính.`));
  }
  return errors;
}