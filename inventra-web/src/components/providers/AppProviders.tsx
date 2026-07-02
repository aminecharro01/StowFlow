"use client";

import { LocaleProvider } from "@/lib/i18n/LocaleProvider";
import { ToastProvider } from "@/components/providers/ToastProvider";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <LocaleProvider>
      <ToastProvider>{children}</ToastProvider>
    </LocaleProvider>
  );
}
