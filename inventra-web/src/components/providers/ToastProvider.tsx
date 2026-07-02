"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { T } from "@/lib/theme";
import { CheckCircle2, XCircle, X } from "lucide-react";

type ToastKind = "success" | "error";

type ToastItem = {
  id: number;
  kind: ToastKind;
  message: string;
};

type ToastContextValue = {
  toast: (message: string, kind?: ToastKind) => void;
  success: (message: string) => void;
  error: (message: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: number) => {
    setItems((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const push = useCallback(
    (message: string, kind: ToastKind = "success") => {
      const id = Date.now() + Math.random();
      setItems((prev) => [...prev, { id, kind, message }]);
      window.setTimeout(() => dismiss(id), 4000);
    },
    [dismiss],
  );

  const value = useMemo(
    () => ({
      toast: push,
      success: (message: string) => push(message, "success"),
      error: (message: string) => push(message, "error"),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none max-w-sm w-full"
        aria-live="polite"
      >
        {items.map((item) => (
          <div
            key={item.id}
            className="pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg animate-[modal-slide-in_0.22s_ease-out]"
            style={{
              background: T.surface,
              borderColor: T.border,
              boxShadow: "0 10px 40px rgba(15,23,42,0.12)",
            }}
          >
            {item.kind === "success" ? (
              <CheckCircle2 size={18} className="shrink-0 mt-0.5" style={{ color: T.success }} />
            ) : (
              <XCircle size={18} className="shrink-0 mt-0.5" style={{ color: T.danger }} />
            )}
            <p className="text-sm flex-1 leading-snug" style={{ color: T.text }}>
              {item.message}
            </p>
            <button
              type="button"
              onClick={() => dismiss(item.id)}
              className="shrink-0 rounded-md p-0.5 hover:bg-gray-100"
              aria-label="Close"
            >
              <X size={14} style={{ color: T.text2 }} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    return {
      toast: () => {},
      success: () => {},
      error: () => {},
    };
  }
  return ctx;
}
