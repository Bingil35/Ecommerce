import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Check, ChevronDown, Search } from 'lucide-react';

export interface SearchableOption { value: string; label: string; }
const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLocaleLowerCase('vi');

export default function SearchableSelect({
  value, options, onChange, placeholder, searchPlaceholder, disabled = false, required = false, className = '',
}: {
  value: string;
  options: SearchableOption[];
  onChange: (value: string, option?: SearchableOption) => void;
  placeholder: string;
  searchPlaceholder: string;
  disabled?: boolean;
  required?: boolean;
  className?: string;
}) {
  const root = useRef<HTMLDivElement>(null);
  const listId = useId();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const selected = options.find(option => option.value === value);
  const filtered = useMemo(() => {
    const normalized = normalize(query.trim());
    if (!normalized) return options;
    return options.filter(option => normalize(option.label).includes(normalized));
  }, [options, query]);

  useEffect(() => {
    const closeOutside = (event: MouseEvent) => {
      if (root.current && !root.current.contains(event.target as Node)) {
        setOpen(false);
        setQuery(selected?.label || '');
      }
    };
    document.addEventListener('mousedown', closeOutside);
    return () => document.removeEventListener('mousedown', closeOutside);
  }, [selected?.label]);

  const openMenu = () => {
    if (disabled) return;
    setQuery('');
    setOpen(true);
  };

  return <div ref={root} className="relative min-w-0">
    <div className="relative">
      <input
        role="combobox"
        aria-expanded={open}
        aria-autocomplete="list"
        aria-controls={listId}
        autoComplete="off"
        disabled={disabled}
        required={required}
        className={`${className} pr-9`}
        value={open ? query : (selected?.label || '')}
        placeholder={open ? searchPlaceholder : placeholder}
        onFocus={openMenu}
        onClick={() => { if (!open) openMenu(); }}
        onChange={event => { setQuery(event.target.value); setOpen(true); }}
        onKeyDown={event => {
          if (event.key === 'Escape') { setOpen(false); setQuery(selected?.label || ''); }
          if (event.key === 'Enter' && open) {
            event.preventDefault();
            if (filtered.length === 1) {
              onChange(filtered[0].value, filtered[0]);
              setQuery(filtered[0].label);
              setOpen(false);
            }
          }
        }}
      />
      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">{open ? <Search size={15}/> : <ChevronDown size={16}/>}</span>
    </div>
    {open && <div id={listId} role="listbox" className="absolute inset-x-0 top-full z-[80] mt-1 max-h-60 overflow-y-auto rounded-xl border border-slate-200 bg-white py-1 shadow-xl">
      {filtered.length ? filtered.map(option => <button
        key={option.value}
        type="button"
        role="option"
        aria-selected={option.value === value}
        onMouseDown={event => event.preventDefault()}
        onClick={() => { onChange(option.value, option); setQuery(option.label); setOpen(false); }}
        className="flex w-full items-center justify-between gap-3 px-3 py-2.5 text-left text-sm text-slate-700 transition hover:bg-indigo-50 hover:text-indigo-700"
      ><span>{option.label}</span>{option.value === value && <Check size={15} className="shrink-0 text-indigo-600"/>}</button>) : <p className="px-3 py-3 text-sm text-slate-500">Không tìm thấy địa chỉ phù hợp.</p>}
    </div>}
  </div>;
}
