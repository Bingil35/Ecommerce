import { Eye, EyeOff } from 'lucide-react';

interface EyeIconProps { className?: string; visible?: boolean }

export default function EyeIcon({ className, visible = false }: EyeIconProps) {
  const Icon = visible ? Eye : EyeOff;
  return <Icon className={className} aria-hidden="true"/>;
}
