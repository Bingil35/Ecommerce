// Keep the whitespace set and NFC/lowercase order aligned with ProductOptionValidation.
export const cleanOptionText = (value: string) => value.normalize('NFC').replace(/[\u0009-\u000D\u0020\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000\uFEFF]+/g, ' ').trim();
export const optionKey = (value: string) => cleanOptionText(value).toLowerCase().normalize('NFC');
export const optionTextError = (value: string, others: string[], label: string) => !optionKey(value)
  ? `${label} không được để trống.`
  : others.some(other => optionKey(other) === optionKey(value)) ? `${label} '${cleanOptionText(value)}' đã tồn tại.` : '';
export const optionErrors = (options: {name: string; values: string[]}[]) => options.map((option, index) => ({
  name: optionTextError(option.name, options.filter((_, i) => i !== index).map(o => o.name), 'Thuộc tính trong sản phẩm'),
  values: !option.values.length ? 'Cần ít nhất một giá trị thuộc tính.' : option.values.map((value, i) => optionTextError(value, option.values.filter((_, j) => i !== j), `Giá trị trong thuộc tính '${option.name}'`)).find(Boolean) || '',
}));
