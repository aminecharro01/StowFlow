"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Icons } from "./Icons";
import { Logo } from "./Logo";
import { NAV, type NavItem } from "./nav";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet } from "@/lib/api";
import { loadSession } from "@/lib/auth";
import { getDisplayName, initialsFromName } from "@/lib/userDisplay";
import { canAccessNavKey, canAccessAdminSettings } from "@/lib/rolePrivileges";
import { useEffect, useMemo, useState } from "react";

type Summary = { outOfStock: number; critical: number; low: number; healthy: number };

function alertTotal(s: Summary): number {
  return s.outOfStock + s.critical + s.low;
}

function NavLink({ n, pathname, invBadge, label }: { n: NavItem; pathname: string; invBadge: number | null; label: string }) {
  const active =
    pathname === n.href
    || (n.href === "/pos" && pathname === "/pos")
    || (n.href !== "/dashboard" && n.href !== "/pos" && pathname.startsWith(n.href));

  return (
    <Link
      href={n.href}
      className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 text-left relative focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40"
      style={
        active
          ? { background: "rgba(37,99,235,0.3)", color: "#fff", borderLeft: "3px solid #14B8A6" }
          : { color: "#BFDBFE" }
      }
    >
      <span style={{ color: active ? "#fff" : "#93C5FD" }}>{Icons[n.icon]}</span>
      {label}
      {n.href === "/inventory" && invBadge != null && invBadge > 0 && (
        <span
          className="ml-auto text-xs font-bold px-1.5 py-0.5 rounded-md"
          style={{ background: T.danger, color: "#fff" }}
        >
          {invBadge}
        </span>
      )}
    </Link>
  );
}

export function Sidebar() {
  const pathname = usePathname();
  const { t } = useT();
  const [invBadge, setInvBadge] = useState<number | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);

  const navItems = useMemo(() => {
    const r = role ?? loadSession()?.role;
    if (!r) return NAV;
    return NAV.filter((n) => canAccessNavKey(r, n.key));
  }, [role]);

  const mainItems = useMemo(() => navItems.filter((n) => n.section !== "purchasing"), [navItems]);
  const purchasingItems = useMemo(() => navItems.filter((n) => n.section === "purchasing"), [navItems]);

  useEffect(() => {
    const s = loadSession();
    setRole(s?.role ?? null);
    setEmail(s?.email ?? null);
  }, [pathname]);

  useEffect(() => {
    apiGet<Summary>("/api/alerts/summary")
      .then((s) => setInvBadge(alertTotal(s)))
      .catch(() => setInvBadge(null));
  }, [role, pathname]);

  const displayName = getDisplayName(email);
  const roleLabel =
    role && role in t.roles
      ? t.roles[role as keyof typeof t.roles]
      : t.common.administrator;

  return (
    <aside
      className="fixed left-0 top-0 h-screen w-60 flex flex-col z-30 select-none"
      style={{ background: T.sidebar }}
    >
      <div
        className="px-4 py-5 border-b"
        style={{ borderColor: "rgba(255,255,255,0.08)" }}
      >
        <Logo href="/dashboard" size={36} variant="light" />
      </div>

      <div className="sidebar-scroll flex-1 overflow-y-auto overflow-x-hidden px-3 py-4 flex flex-col gap-6">
        <div>
          <p
            className="px-3 mb-2 text-xs font-semibold tracking-widest"
            style={{ color: "#93C5FD", letterSpacing: "0.12em" }}
          >
            {t.nav.mainMenu}
          </p>
          <nav className="flex flex-col gap-0.5">
            {mainItems.map((n) => (
              <NavLink
                key={n.key}
                n={n}
                pathname={pathname}
                invBadge={invBadge}
                label={t.nav[n.key as keyof typeof t.nav] ?? n.label}
              />
            ))}
          </nav>
        </div>

        {purchasingItems.length > 0 && (
          <div>
            <p
              className="px-3 mb-2 text-xs font-semibold tracking-widest"
              style={{ color: "#93C5FD", letterSpacing: "0.12em" }}
            >
              {t.nav.purchasingSection}
            </p>
            <nav className="flex flex-col gap-0.5">
              {purchasingItems.map((n) => (
                <NavLink
                  key={n.key}
                  n={n}
                  pathname={pathname}
                  invBadge={invBadge}
                  label={t.nav[n.key as keyof typeof t.nav] ?? n.label}
                />
              ))}
            </nav>
          </div>
        )}

        <div>
          <p
            className="px-3 mb-2 text-xs font-semibold tracking-widest"
            style={{ color: "#93C5FD", letterSpacing: "0.12em" }}
          >
            {t.nav.settingsSection}
          </p>
          <nav className="flex flex-col gap-0.5">
            {canAccessAdminSettings(role ?? loadSession()?.role) && (
              <Link
                href="/settings"
                className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium transition-all hover:bg-white/10 text-left"
                style={{ color: "#BFDBFE" }}
              >
                <span style={{ color: "#93C5FD" }}>{Icons.settings}</span>
                {t.nav.settings}
              </Link>
            )}
            <Link
              href="/help"
              className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium transition-all hover:bg-white/10 text-left"
              style={{ color: "#BFDBFE" }}
            >
              <span style={{ color: "#93C5FD" }}>{Icons.help}</span>
              {t.nav.help}
            </Link>
          </nav>
        </div>
      </div>

      <div
        className="mx-3 mb-4 p-3 rounded-xl flex items-center gap-3"
        style={{ background: "rgba(255,255,255,0.07)" }}
      >
        <div
          className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
          style={{ background: T.primary }}
        >
          {email ? initialsFromName(displayName) : "?"}
        </div>
        <div className="min-w-0">
          <p className="text-white text-xs font-semibold truncate">{displayName}</p>
          <p className="text-xs truncate" style={{ color: "#93C5FD" }} title={email ?? undefined}>
            {roleLabel}
          </p>
        </div>
      </div>
    </aside>
  );
}
