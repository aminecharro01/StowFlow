"use client";

import { useCallback, useEffect } from "react";
import { Card } from "@/components/inventra/ui/Card";
import { Icons } from "@/components/inventra/Icons";
import { T } from "@/lib/theme";

type ModalProps = {
  open: boolean;
  onClose?: () => void;
  title?: string;
  subtitle?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  width?: string;
  closeOnBackdrop?: boolean;
};

export function Modal({
  open,
  onClose,
  title,
  subtitle,
  children,
  footer,
  width = "540px",
  closeOnBackdrop = true,
}: ModalProps) {
  const handleKey = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Escape" && onClose) onClose();
    },
    [onClose]
  );

  useEffect(() => {
    if (!open) return;
    document.addEventListener("keydown", handleKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKey);
      document.body.style.overflow = "";
    };
  }, [open, handleKey]);

  if (!open) return null;

  return (
    <div className="modal-root fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        aria-label="Fermer"
        className="modal-backdrop absolute inset-0 border-0 cursor-default"
        onClick={closeOnBackdrop ? onClose : undefined}
      />
      <div
        className="modal-panel relative w-full max-h-[90vh]"
        style={{ width, maxWidth: "100%" }}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? "modal-title" : undefined}
      >
        <Card
          className="max-h-[90vh] overflow-hidden flex flex-col h-full rounded-2xl"
          style={{
            boxShadow: "0 25px 50px -12px rgba(15, 23, 42, 0.28)",
          }}
        >
        {(title || onClose) && (
          <div
            className="flex items-start justify-between px-6 py-4 border-b shrink-0"
            style={{ borderColor: T.border }}
          >
            <div className="min-w-0 pr-4">
              {title && (
                <h3 id="modal-title" className="text-base font-semibold" style={{ color: T.text }}>
                  {title}
                </h3>
              )}
              {subtitle && (
                <p className="text-xs mt-1 leading-relaxed" style={{ color: T.text2 }}>
                  {subtitle}
                </p>
              )}
            </div>
            {onClose && (
              <button
                type="button"
                onClick={onClose}
                className="modal-close w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                style={{ color: T.text2 }}
              >
                {Icons.x}
              </button>
            )}
          </div>
        )}
        <div className="overflow-y-auto flex-1">{children}</div>
        {footer && (
          <div
            className="flex items-center justify-end gap-2 px-6 py-4 border-t shrink-0"
            style={{ borderColor: T.border, background: T.surface }}
          >
            {footer}
          </div>
        )}
        </Card>
      </div>
    </div>
  );
}
