import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, Info, X } from 'lucide-react';
import { LoadingButton } from './LoadingState';

export type DialogVariant = 'danger' | 'warning' | 'info';
export interface DialogInput {
  label?: string;
  defaultValue?: string;
  placeholder?: string;
  required?: boolean;
  maxLength?: number;
  validate?: (value: string) => string;
}
export interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: DialogVariant;
  loading?: boolean;
  loadingText?: string;
  error?: string;
  input?: DialogInput;
  onConfirm: (value: string) => void | Promise<unknown>;
  onCancel: () => void;
}
export default function ConfirmDialog({
  title,
  message,
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  variant = 'warning',
  loading = false,
  loadingText = 'Đang xử lý...',
  error,
  input,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialog = useRef<HTMLDialogElement>(null);
  const cancel = useRef<HTMLButtonElement>(null);
  const field = useRef<HTMLInputElement>(null);
  const id = useId();
  const [value, setValue] = useState(input?.defaultValue || '');
  const lock = useRef(false);
  const mounted = useRef(true);
  const [pending, setPending] = useState(false);
  const busy = loading || pending;
  const inputError = value.length ? input?.validate?.(value) || '' : '';
  useEffect(() => {
    mounted.current = true;
    const node = dialog.current;
    const previous =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    node?.showModal();
    (field.current || cancel.current)?.focus();
    return () => {
      mounted.current = false;
      node?.close();
      document.body.style.overflow = overflow;
      if (previous?.isConnected) previous.focus();
    };
  }, []);
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy || lock.current || inputError) return;
    lock.current = true;
    setPending(true);
    try {
      await onConfirm(value);
    } finally {
      lock.current = false;
      if (mounted.current) setPending(false);
    }
  };
  const Icon = variant === 'info' ? Info : AlertTriangle;
  return createPortal(
    <dialog
      ref={dialog}
      className={`kala-dialog kala-dialog-${variant}`}
      aria-labelledby={`${id}-title`}
      aria-describedby={`${id}-description`}
      aria-busy={busy}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy && !lock.current) onCancel();
      }}
    >
      <form onSubmit={submit}>
        <div className="kala-dialog-heading">
          <span className="kala-dialog-icon">
            <Icon size={25} />
          </span>
          <button
            type="button"
            className="kala-dialog-close"
            disabled={busy}
            aria-label="Đóng hộp thoại"
            onClick={onCancel}
          >
            <X size={20} />
          </button>
        </div>
        <h2 id={`${id}-title`}>{title}</h2>
        <p id={`${id}-description`}>{message}</p>
        {input && (
          <label className="kala-dialog-field">
            {input.label || title}
            <input
              aria-invalid={!!inputError || !!error}
              aria-describedby={inputError ? `${id}-input-error` : undefined}
              style={inputError || error ? {borderColor:'#B42336'} : undefined}
              ref={field}
              value={value}
              onChange={(event) => setValue(event.target.value)}
              placeholder={input.placeholder}
              required={input.required}
              maxLength={input.maxLength ?? 500}
              disabled={busy}
            />
            {inputError && <small id={`${id}-input-error`} className="kala-dialog-error" role="alert">{inputError}</small>}
          </label>
        )}
        {error && (
          <p className="kala-dialog-error" role="alert">
            {error}
          </p>
        )}
        <div className="kala-dialog-actions">
          <button ref={cancel} type="button" disabled={busy} onClick={onCancel}>
            {cancelText}
          </button>
          <LoadingButton
            type="submit"
            className="kala-dialog-confirm"
            loading={busy}
            loadingText={loadingText}
          >
            {confirmText}
          </LoadingButton>
        </div>
      </form>
    </dialog>,
    document.body,
  );
}
