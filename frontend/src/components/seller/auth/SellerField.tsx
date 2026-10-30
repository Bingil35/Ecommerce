import type { InputHTMLAttributes, ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';

interface SellerFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'className'> {
  id: string;
  label: string;
  icon: LucideIcon;
  trailing?: ReactNode;
}

export default function SellerField({
  id,
  label,
  icon: Icon,
  trailing,
  ...inputProps
}: SellerFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="sr-only">
        {label}
      </label>

      <div className="relative">
        <span className="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-[#52638F]">
          <Icon className="h-[22px] w-[22px]" strokeWidth={1.7} />
        </span>

        <input
          id={id}
          {...inputProps}
          className={`h-[62px] w-full rounded-2xl border border-[#D5E0F5] bg-[#F3F6FD] pl-12 text-sm sm:pl-14 sm:text-[15px] text-[#172033] outline-none transition placeholder:text-[#8E9BBB] focus:border-[#3B7BF6] focus:bg-white focus:ring-4 focus:ring-[#3B7BF6]/10 ${
            trailing ? 'pr-14' : 'pr-4'
          }`}
        />

        {trailing && (
          <div className="absolute right-4 top-1/2 -translate-y-1/2">
            {trailing}
          </div>
        )}
      </div>
    </div>
  );
}
