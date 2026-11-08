import {
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { Loader2 } from 'lucide-react';

export function LoadingState({
  label = 'Đang xử lý...',
  size = 17,
}: {
  label?: string;
  size?: number;
}) {
  return (
    <span className="kala-loading" role="status">
      <Loader2 size={size} aria-hidden="true" />
      <span>{label}</span>
    </span>
  );
}

type LoadingButtonProps = Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'onClick'
> & {
  loading?: boolean;
  loadingText?: string;
  children: ReactNode;
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void | Promise<unknown>;
};
export function LoadingButton({
  loading = false,
  loadingText = 'Đang xử lý...',
  children,
  disabled,
  onClick,
  className = '',
  type = 'button',
  ...props
}: LoadingButtonProps) {
  const lock = useRef(false);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  const [pending, setPending] = useState(false);
  const busy = loading || pending;
  const click = async (event: MouseEvent<HTMLButtonElement>) => {
    if (lock.current || busy) {
      event.preventDefault();
      return;
    }
    if (!onClick) return;
    lock.current = true;
    try {
      const result = onClick(event);
      if (result && typeof result.then === 'function') {
        setPending(true);
        await result;
      }
    } finally {
      lock.current = false;
      if (mounted.current) setPending(false);
    }
  };
  return (
    <button
      {...props}
      type={type}
      className={`kala-loading-button ${className}`}
      disabled={disabled || busy}
      aria-busy={busy}
      onClick={click}
    >
      <span
        className="kala-button-layer"
        style={{ visibility: busy ? 'hidden' : 'visible' }}
        aria-hidden={busy}
      >
        {children}
      </span>
      <span
        className="kala-button-layer"
        style={{ visibility: busy ? 'visible' : 'hidden' }}
        aria-hidden={!busy}
      >
        <LoadingState label={loadingText} />
      </span>
    </button>
  );
}
