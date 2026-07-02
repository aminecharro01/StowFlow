"use client";

import { Icons } from "@/components/inventra/Icons";
import { T } from "@/lib/theme";

export type AlertVariant = "success" | "error" | "warning" | "info";

type AlertBannerProps = {
  variant: AlertVariant;
  children: React.ReactNode;
  className?: string;
  onDismiss?: () => void;
};

const config: Record<
  AlertVariant,
  { bg: string; border: string; color: string; icon: React.ReactNode }
> = {
  success: {
    bg: "#ECFDF5",
    border: "#A7F3D0",
    color: "#047857",
    icon: (
      <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
        <polyline points="22 4 12 14.01 9 11.01" />
      </svg>
    ),
  },
  error: {
    bg: "#FEF2F2",
    border: "#FECACA",
    color: T.danger,
    icon: (
      <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <circle cx="12" cy="12" r="10" />
        <line x1="15" y1="9" x2="9" y2="15" />
        <line x1="9" y1="9" x2="15" y2="15" />
      </svg>
    ),
  },
  warning: {
    bg: "#FFFBEB",
    border: "#FDE68A",
    color: "#B45309",
    icon: (
      <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
        <line x1="12" y1="9" x2="12" y2="13" />
        <line x1="12" y1="17" x2="12.01" y2="17" />
      </svg>
    ),
  },
  info: {
    bg: "#EFF6FF",
    border: "#BFDBFE",
    color: T.primary,
    icon: (
      <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="16" x2="12" y2="12" />
        <line x1="12" y1="8" x2="12.01" y2="8" />
      </svg>
    ),
  },
};

export function AlertBanner({ variant, children, className = "", onDismiss }: AlertBannerProps) {
  const c = config[variant];

  return (
    <div
      className={`flex items-start gap-3 text-sm rounded-xl px-4 py-3 border ${className}`}
      style={{ background: c.bg, borderColor: c.border, color: c.color }}
      role="alert"
    >
      <span className="shrink-0 mt-0.5">{c.icon}</span>
      <div className="flex-1 min-w-0 leading-relaxed">{children}</div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="shrink-0 w-7 h-7 rounded-lg flex items-center justify-center opacity-70 hover:opacity-100 transition-opacity"
          aria-label="Fermer"
        >
          {Icons.x}
        </button>
      )}
    </div>
  );
}
