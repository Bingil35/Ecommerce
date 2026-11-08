import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence } from 'framer-motion';
import ConfirmDialog, {
  type DialogInput,
  type DialogVariant,
} from '../components/common/ConfirmDialog';
import ToastNotice, {
  type ToastVariant,
} from '../components/common/ToastNotice';
import '../components/common/feedback.css';

export interface ToastOptions {
  duration?: number;
  actionLabel?: string;
  actionTo?: string;
}
export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: DialogVariant;
  loading?: boolean;
  loadingText?: string;
  successMessage?: string;
  onConfirm?: (value: string) => unknown | Promise<unknown>;
  onCancel?: () => void;
}
export interface PromptOptions extends ConfirmOptions {
  input?: DialogInput;
}
type Toast = ToastOptions & {
  id: number;
  message: string;
  variant: ToastVariant;
};
type Request = {
  id: number;
  options: PromptOptions;
  prompt: boolean;
  resolve: (value: string | boolean | null) => void;
  signal: AbortSignal;
  detach: () => void;
};
type ToastApi = Record<
  ToastVariant,
  (message: string, options?: ToastOptions) => void
>;
type FeedbackApi = {
  toast: ToastApi;
  request: (
    options: PromptOptions,
    prompt: boolean,
    signal: AbortSignal,
  ) => Promise<string | boolean | null>;
};
const FeedbackContext = createContext<FeedbackApi | null>(null);
export const getFeedbackError = (error: unknown) => {
  const value = error as {
    response?: { data?: { message?: string } };
    message?: string;
  };
  return (
    value?.response?.data?.message ||
    value?.message ||
    'Không thể thực hiện thao tác. Vui lòng thử lại.'
  );
};
function ToastItem({
  item,
  remove,
}: {
  item: Toast;
  remove: (id: number) => void;
}) {
  const close = useCallback(() => remove(item.id), [remove, item.id]);
  return <ToastNotice {...item} onClose={close} />;
}
export function FeedbackProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [current, setCurrent] = useState<Request | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const mounted = useRef(true),
    active = useRef<Request | null>(null),
    running = useRef(false);
  const queue = useRef<Request[]>([]),
    sequence = useRef(0),
    recent = useRef(new Map<string, number>());
  const remove = useCallback(
    (id: number) =>
      setToasts((items) => items.filter((item) => item.id !== id)),
    [],
  );
  const show = useCallback(
    (variant: ToastVariant, message: string, options: ToastOptions = {}) => {
      if (!mounted.current || !message.trim()) return;
      const key = `${variant}:${message}`,
        now = Date.now();
      if (now - (recent.current.get(key) ?? 0) < 4000) return;
      recent.current.forEach((time, key) => {
        if (now - time >= 4000) recent.current.delete(key);
      });
      recent.current.set(key, now);
      const item = {
        ...options,
        id: ++sequence.current,
        message,
        variant,
        duration: Math.min(5000, Math.max(3000, options.duration ?? 4000)),
      };
      setToasts((items) => [...items, item].slice(-4));
    },
    [],
  );
  const toast = useMemo<ToastApi>(
    () => ({
      success: (m, o) => show('success', m, o),
      error: (m, o) => show('error', m, o),
      warning: (m, o) => show('warning', m, o),
      info: (m, o) => show('info', m, o),
    }),
    [show],
  );
  const advance = useCallback(() => {
    if (!mounted.current || active.current) return;
    const next = queue.current.shift() ?? null;
    active.current = next;
    setError('');
    setCurrent(next);
  }, []);
  const finish = useCallback(
    (item: Request, value: string | boolean | null) => {
      item.detach();
      item.resolve(value);
      if (active.current?.id === item.id) {
        active.current = null;
        if (mounted.current) {
          setCurrent(null);
          setError('');
          setLoading(false);
        }
        running.current = false;
        advance();
      } else
        queue.current = queue.current.filter((entry) => entry.id !== item.id);
    },
    [advance],
  );
  const request = useCallback(
    (options: PromptOptions, prompt: boolean, signal: AbortSignal) =>
      new Promise<string | boolean | null>((resolve) => {
        if (!mounted.current || signal.aborted) {
          resolve(prompt ? null : false);
          return;
        }
        const item: Request = {
          id: ++sequence.current,
          options,
          prompt,
          resolve,
          signal,
          detach: () => {},
        };
        const abort = () => {
          if (active.current?.id === item.id && running.current) return;
          finish(item, prompt ? null : false);
        };
        signal.addEventListener('abort', abort, { once: true });
        item.detach = () => signal.removeEventListener('abort', abort);
        queue.current.push(item);
        advance();
      }),
    [advance, finish],
  );
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      const entries = [
        ...queue.current,
        ...(active.current ? [active.current] : []),
      ];
      queue.current = [];
      active.current = null;
      entries.forEach((item) => {
        item.detach();
        item.resolve(item.prompt ? null : false);
      });
    };
  }, []);
  const cancel = () => {
    const item = active.current;
    if (!item || running.current || item.options.loading) return;
    try {
      item.options.onCancel?.();
    } finally {
      finish(item, item.prompt ? null : false);
    }
  };
  const accept = async (value: string) => {
    const item = active.current;
    if (!item || running.current || item.options.loading) return;
    running.current = true;
    setLoading(true);
    setError('');
    try {
      const result = await item.options.onConfirm?.(value);
      if (result === false) {
        running.current = false;
        if (item.signal.aborted) {
          finish(item, item.prompt ? null : false);
          return;
        }
        if (mounted.current) {
          setLoading(false);
          setError(
            'Chưa thể hoàn tất thao tác. Vui lòng kiểm tra dữ liệu và thử lại.',
          );
        }
        return;
      }
      if (item.options.successMessage)
        toast.success(item.options.successMessage);
      finish(item, item.prompt ? value : true);
    } catch (cause) {
      const message = getFeedbackError(cause);
      if (mounted.current) {
        setError(message);
        setLoading(false);
        toast.error(message);
      }
      running.current = false;
      if (item.signal.aborted) finish(item, item.prompt ? null : false);
    }
  };
  const api = useMemo(() => ({ toast, request }), [toast, request]);
  return (
    <FeedbackContext.Provider value={api}>
      {children}
      {createPortal(
        <div className="kala-toast-stack" aria-label="Thông báo">
          <AnimatePresence initial={false}>
            {toasts.map((item) => (
              <ToastItem key={item.id} item={item} remove={remove} />
            ))}
          </AnimatePresence>
        </div>,
        document.body,
      )}
      {current && (
        <ConfirmDialog
          key={current.id}
          {...current.options}
          input={current.prompt ? (current.options.input ?? {}) : undefined}
          loading={loading || current.options.loading}
          error={error}
          onConfirm={accept}
          onCancel={cancel}
        />
      )}
    </FeedbackContext.Provider>
  );
}
function useFeedback() {
  const context = useContext(FeedbackContext);
  if (!context) throw Error('Feedback hooks require FeedbackProvider');
  return context;
}
export function useToast() {
  return useFeedback().toast;
}
export function useDialog() {
  const { request } = useFeedback();
  const controller = useRef(new AbortController());
  useEffect(() => {
    if (controller.current.signal.aborted)
      controller.current = new AbortController();
    return () => controller.current.abort();
  }, []);
  return useMemo(
    () => ({
      confirm: (options: ConfirmOptions) =>
        request(options, false, controller.current.signal) as Promise<boolean>,
      prompt: (options: PromptOptions) =>
        request(options, true, controller.current.signal) as Promise<
          string | null
        >,
    }),
    [request],
  );
}
