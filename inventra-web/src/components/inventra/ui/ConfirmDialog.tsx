"use client";

import { Btn } from "@/components/inventra/ui/Btn";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";

export type ConfirmVariant = "default" | "danger" | "warning" | "success" | "phone" | "email";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  message: string;
  detail?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: ConfirmVariant;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

const variantStyles: Record<
  ConfirmVariant,
  { accent: string; iconBg: string; Icon: () => React.ReactNode }
> = {
  danger: {
    accent: T.danger,
    iconBg: "#FEE2E2",
    Icon: () => (
      <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke={T.danger} strokeWidth={2}>
        <circle cx="12" cy="12" r="10" />
        <line x1="15" y1="9" x2="9" y2="15" />
        <line x1="9" y1="9" x2="15" y2="15" />
      </svg>
    ),
  },
  warning: {
    accent: T.warning,
    iconBg: "#FEF3C7",
    Icon: () => (
      <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke={T.warning} strokeWidth={2}>
        <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
        <line x1="12" y1="9" x2="12" y2="13" />
        <line x1="12" y1="17" x2="12.01" y2="17" />
      </svg>
    ),
  },
  success: {
    accent: T.success,
    iconBg: "#DCFCE7",
    Icon: () => (
      <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke={T.success} strokeWidth={2}>
        <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
        <polyline points="22 4 12 14.01 9 11.01" />
      </svg>
    ),
  },
  phone: {
    accent: T.primary,
    iconBg: "#DBEAFE",
    Icon: () => (
      <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke={T.primary} strokeWidth={2}>
        <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z" />
      </svg>
    ),
  },
  email: {
    accent: T.accent,
    iconBg: "#CCFBF1",
    Icon: () => (
      <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke={T.accent} strokeWidth={2}>
        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
        <polyline points="22,6 12,13 2,6" />
      </svg>
    ),
  },
  default: {
    accent: T.primary,
    iconBg: "#DBEAFE",
    Icon: () => (
      <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke={T.primary} strokeWidth={2}>
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="16" x2="12" y2="12" />
        <line x1="12" y1="8" x2="12.01" y2="8" />
      </svg>
    ),
  },
};

function btnVariant(v: ConfirmVariant): "primary" | "danger" | "warning" | "success" {
  if (v === "danger") return "danger";
  if (v === "warning") return "warning";
  if (v === "success") return "success";
  return "primary";
}

export function ConfirmDialog({
  open,
  title,
  message,
  detail,
  confirmLabel,
  cancelLabel,
  variant = "default",
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const { t } = useT();
  const resolvedConfirm = confirmLabel ?? t.confirm.confirm;
  const resolvedCancel = cancelLabel ?? t.common.cancel;
  const { accent, iconBg, Icon } = variantStyles[variant];

  if (!open) return null;

  return (
    <div className="modal-root fixed inset-0 z-[60] flex items-center justify-center p-4 sm:p-6">
      <button
        type="button"
        aria-label="Fermer"
        className="modal-backdrop absolute inset-0 border-0 cursor-default"
        onClick={loading ? undefined : onCancel}
      />
      <div
        className="modal-panel confirm-panel relative w-full max-w-[420px] overflow-hidden"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-message"
      >
        <div className="px-6 pt-6 pb-5">
          <div className="flex gap-4">
            <div
              className="w-12 h-12 rounded-2xl flex items-center justify-center shrink-0"
              style={{ background: iconBg, color: accent }}
            >
              <Icon />
            </div>
            <div className="min-w-0 pt-0.5">
              <h3 id="confirm-title" className="text-[17px] font-bold leading-snug tracking-tight" style={{ color: T.text }}>
                {title}
              </h3>
              <p id="confirm-message" className="text-sm mt-2 leading-relaxed whitespace-pre-line" style={{ color: T.text2 }}>
                {message}
              </p>
            </div>
          </div>
          {detail && (
            <div
              className="mt-4 px-4 py-3 rounded-xl text-sm font-semibold text-center tracking-wide"
              style={{ background: "#F8FAFC", border: `1px solid ${T.border}`, color: T.text }}
            >
              {detail}
            </div>
          )}
        </div>
        <div className="confirm-footer flex items-center justify-end gap-2.5 px-5 py-4">
          <Btn
            type="button"
            variant="ghost"
            onClick={onCancel}
            disabled={loading}
            className="confirm-btn-ghost rounded-full px-5"
          >
            {resolvedCancel}
          </Btn>
          <Btn
            type="button"
            variant={btnVariant(variant)}
            onClick={onConfirm}
            disabled={loading}
            className="confirm-btn-primary rounded-full px-5 min-w-[7rem] justify-center"
          >
            {loading ? t.common.saving : resolvedConfirm}
          </Btn>
        </div>
      </div>
    </div>
  );
}
