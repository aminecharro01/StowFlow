"use client";

import { T } from "@/lib/theme";

export function Btn({
  children,
  variant = "primary",
  size = "md",
  onClick,
  className = "",
  type = "button",
  disabled = false,
}: {
  children: React.ReactNode;
  variant?: "primary" | "ghost" | "danger" | "warning" | "success";
  size?: "sm" | "md" | "lg";
  onClick?: () => void;
  className?: string;
  type?: "button" | "submit";
  disabled?: boolean;
}) {
  const base =
    "inline-flex items-center gap-2 font-semibold rounded-lg transition-all duration-150 cursor-pointer";
  const sizes = { sm: "px-3 py-1.5 text-xs", md: "px-4 py-2 text-sm", lg: "px-5 py-2.5 text-sm" };
  const variants = {
    primary: `text-white hover:opacity-90 active:scale-95`,
    ghost: `border hover:bg-gray-50 active:scale-95`,
    danger: `text-white hover:opacity-90 active:scale-95`,
    warning: `text-white hover:opacity-90 active:scale-95`,
    success: `text-white hover:opacity-90 active:scale-95`,
  };
  const styles: Record<string, React.CSSProperties> = {
    primary: { background: T.primary },
    ghost: { borderColor: T.border, color: T.text2 },
    danger: { background: T.danger },
    warning: { background: T.warning },
    success: { background: T.success },
  };
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`${base} ${sizes[size]} ${variants[variant]} ${className} disabled:opacity-50 disabled:pointer-events-none`}
      style={styles[variant]}
    >
      {children}
    </button>
  );
}
