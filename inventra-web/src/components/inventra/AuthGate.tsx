"use client";

import { apiGet } from "@/lib/api";
import { clearSession, loadSession } from "@/lib/auth";
import { useT } from "@/lib/i18n";
import { T } from "@/lib/theme";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export function AuthGate({ children }: { children: React.ReactNode }) {
  const { t } = useT();
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function verify() {
      const s = loadSession();
      if (!s) {
        if (!cancelled) {
          setAllowed(false);
          setReady(true);
          router.replace("/login");
        }
        return;
      }
      try {
        await apiGet<{ email: string; role: string; tenantSlug: string | null }>("/api/auth/me");
        if (!cancelled) {
          setAllowed(true);
          setReady(true);
        }
      } catch {
        clearSession();
        if (!cancelled) {
          setAllowed(false);
          setReady(true);
          router.replace("/login");
        }
      }
    }

    setReady(false);
    void verify();
    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  if (!ready) {
    return (
      <div
        className="min-h-screen flex items-center justify-center text-sm font-medium"
        style={{ color: T.text2, background: "#F8FAFC" }}
      >
        {t.common.loading}
      </div>
    );
  }

  if (!allowed) return null;
  return <>{children}</>;
}
