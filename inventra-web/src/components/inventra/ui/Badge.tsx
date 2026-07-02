"use client";

import { statusFr } from "@/lib/i18n/fr";
import { useT } from "@/lib/i18n";
import { T } from "@/lib/theme";

const cfg: Record<string, { bg: string; color: string }> = {
  "In Stock": { bg: "rgba(34,197,94,0.1)", color: "#16A34A" },
  "Low Stock": { bg: "rgba(245,158,11,0.1)", color: "#D97706" },
  "Out of Stock": { bg: "rgba(239,68,68,0.12)", color: "#DC2626" },
  Active: { bg: "rgba(34,197,94,0.1)", color: "#16A34A" },
  Inactive: { bg: "rgba(156,163,175,0.15)", color: "#6B7280" },
  "On Hold": { bg: "rgba(245,158,11,0.1)", color: "#D97706" },
  Processing: { bg: "rgba(37,99,235,0.1)", color: "#2563EB" },
  Shipped: { bg: "rgba(20,184,166,0.1)", color: "#0D9488" },
  Delivered: { bg: "rgba(34,197,94,0.1)", color: "#16A34A" },
  Pending: { bg: "rgba(245,158,11,0.1)", color: "#D97706" },
  Cancelled: { bg: "rgba(239,68,68,0.12)", color: "#DC2626" },
  Critical: { bg: "rgba(239,68,68,0.12)", color: "#DC2626" },
  Low: { bg: "rgba(245,158,11,0.1)", color: "#D97706" },
  Received: { bg: "rgba(34,197,94,0.1)", color: "#16A34A" },
  Confirmed: { bg: "rgba(37,99,235,0.1)", color: "#2563EB" },
  "In Progress": { bg: "rgba(37,99,235,0.1)", color: "#2563EB" },
  Rejected: { bg: "rgba(239,68,68,0.12)", color: "#DC2626" },
};

export function Badge({ status }: { status: string }) {
  const { statusLabel } = useT();
  const s = cfg[status] ?? cfg.Active;
  const label = statusLabel(status);
  return (
    <span
      className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-xs font-semibold whitespace-nowrap"
      style={{ background: s.bg, color: s.color }}
    >
      <span className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: s.color }} />
      {label}
    </span>
  );
}

/** Server-safe fallback without hook */
export function badgeLabelFr(status: string) {
  return statusFr(status);
}
