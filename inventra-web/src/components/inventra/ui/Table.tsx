"use client";

import { T } from "@/lib/theme";

export function TH({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <th
      className={`px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide ${className}`}
      style={{ color: T.text2, background: T.bg }}
    >
      {children}
    </th>
  );
}

export function TD({ children, className = "", style }: { children: React.ReactNode; className?: string; style?: React.CSSProperties }) {
  return (
    <td className={`px-4 py-3.5 text-sm ${className}`} style={{ color: T.text, ...style }}>
      {children}
    </td>
  );
}
