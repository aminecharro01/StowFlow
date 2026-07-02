"use client";

import { clearSession, getTenantSlug, loadSession } from "@/lib/auth";
import { useT } from "@/lib/i18n";
import { getDisplayName, initialsFromName } from "@/lib/userDisplay";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { Icons } from "./Icons";
import { Input } from "./ui/Input";
import { Btn } from "./ui/Btn";
import { T } from "@/lib/theme";
import { apiGet } from "@/lib/api";
import { canAccessNavKey } from "@/lib/rolePrivileges";

type AlertSummary = {
  outOfStock: number;
  critical: number;
  low: number;
  healthy: number;
};

function alertTotal(s: AlertSummary): number {
  return s.outOfStock + s.critical + s.low;
}

export function Topbar({ title, subtitle }: { title: string; subtitle?: string }) {
  const router = useRouter();
  const pathname = usePathname();
  const { t } = useT();
  const [q, setQ] = useState("");
  const [actor, setActor] = useState<string | null>(null);
  const [tenantSlug, setTenantSlug] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [alertCount, setAlertCount] = useState<number | null>(null);

  const showAlertsBell = canAccessNavKey(role, "inventory");
  const displayName = getDisplayName(actor);

  const refreshAlerts = useCallback(() => {
    if (!showAlertsBell) {
      setAlertCount(null);
      return;
    }
    apiGet<AlertSummary>("/api/alerts/summary")
      .then((s) => setAlertCount(alertTotal(s)))
      .catch(() => setAlertCount(null));
  }, [showAlertsBell]);

  useEffect(() => {
    const s = loadSession();
    setActor(s?.email ?? null);
    setTenantSlug(getTenantSlug());
    setRole(s?.role ?? null);
  }, []);

  useEffect(() => {
    setRole(loadSession()?.role ?? null);
    refreshAlerts();
  }, [pathname, refreshAlerts]);

  function logout() {
    clearSession();
    router.replace("/login");
  }

  const bellLabel =
    alertCount != null && alertCount > 0
      ? t.common.bellTitleCount(alertCount)
      : t.common.bellTitle;

  return (
    <header
      className="fixed top-0 left-60 right-0 h-16 bg-white z-20 flex items-center justify-between px-6"
      style={{ boxShadow: "0 1px 0 #E5E7EB, 0 2px 8px rgba(0,0,0,0.03)" }}
    >
      <div>
        <h1 className="text-base font-bold leading-tight" style={{ color: T.text }}>
          {title}
        </h1>
        {subtitle && (
          <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
            {subtitle}
          </p>
        )}
      </div>
      <div className="flex items-center gap-2">
        {tenantSlug && (
          <span
            className="hidden md:inline-flex text-xs font-semibold px-2.5 py-1 rounded-lg border max-w-[200px] truncate"
            style={{ color: T.primary, borderColor: T.border, background: "#EFF6FF" }}
            title={t.common.tenantScope(tenantSlug)}
          >
            {t.common.tenantScope(tenantSlug)}
          </span>
        )}
        <LanguageSwitcher compact />
        {actor && (
          <Btn variant="ghost" size="sm" onClick={logout} className="hidden sm:inline-flex">
            {t.common.logout}
          </Btn>
        )}
        <Input
          placeholder={t.common.search}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          icon={Icons.search}
          className="w-60 hidden lg:flex"
        />
        {showAlertsBell && (
          <Link
            href="/inventory"
            className="relative w-9 h-9 rounded-lg flex items-center justify-center hover:bg-gray-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            style={{ color: T.text }}
            title={bellLabel}
            aria-label={bellLabel}
          >
            {Icons.bell}
            {alertCount != null && alertCount > 0 && (
              <span
                className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full flex items-center justify-center text-[10px] font-bold text-white ring-2 ring-white"
                style={{ background: T.danger }}
              >
                {alertCount > 99 ? "99+" : alertCount}
              </span>
            )}
          </Link>
        )}
        <div
          className="w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold text-white cursor-default"
          style={{ background: T.primary }}
          title={displayName}
        >
          {actor ? initialsFromName(displayName) : "?"}
        </div>
      </div>
    </header>
  );
}
