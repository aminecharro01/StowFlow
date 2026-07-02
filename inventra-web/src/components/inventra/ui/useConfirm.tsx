"use client";

import { AlertBanner } from "@/components/inventra/ui/AlertBanner";
import { ConfirmDialog, type ConfirmVariant } from "@/components/inventra/ui/ConfirmDialog";
import { useCallback, useRef, useState } from "react";

export type ConfirmOptions = {
  title: string;
  message: string;
  detail?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: ConfirmVariant;
};

type ActiveConfirm = ConfirmOptions & { open: boolean };

export function useConfirm() {
  const [state, setState] = useState<ActiveConfirm | null>(null);
  const resolver = useRef<((value: boolean) => void) | null>(null);

  const confirm = useCallback((options: ConfirmOptions) => {
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve;
      setState({ ...options, open: true });
    });
  }, []);

  const finish = useCallback((result: boolean) => {
    resolver.current?.(result);
    resolver.current = null;
    setState(null);
  }, []);

  const dialog = state ? (
    <ConfirmDialog
      open={state.open}
      title={state.title}
      message={state.message}
      detail={state.detail}
      confirmLabel={state.confirmLabel}
      cancelLabel={state.cancelLabel}
      variant={state.variant}
      onCancel={() => finish(false)}
      onConfirm={() => finish(true)}
    />
  ) : null;

  return { confirm, dialog };
}

export { AlertBanner };
