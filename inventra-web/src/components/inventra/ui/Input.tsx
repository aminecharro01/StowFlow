"use client";

import { T } from "@/lib/theme";

export function Input({
  placeholder,
  value,
  onChange,
  icon,
  className = "",
  type = "text",
  readOnly,
  disabled,
  autoComplete,
  required,
  min,
  max,
  list,
}: {
  placeholder?: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  icon?: React.ReactNode;
  className?: string;
  type?: string;
  readOnly?: boolean;
  disabled?: boolean;
  autoComplete?: string;
  required?: boolean;
  min?: number;
  max?: number;
  list?: string;
}) {
  return (
    <div className={`relative ${className}`}>
      {icon && (
        <span className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: T.text2 }}>
          {icon}
        </span>
      )}
      <input
        type={type}
        value={value}
        onChange={onChange}
        readOnly={readOnly}
        disabled={disabled}
        autoComplete={autoComplete}
        required={required}
        min={min}
        max={max}
        list={list}
        placeholder={placeholder}
        className="w-full rounded-lg border text-sm outline-none transition-all focus:ring-2 focus:ring-blue-100 disabled:opacity-60"
        style={{
          paddingLeft: icon ? 36 : 12,
          paddingRight: 12,
          paddingTop: 8,
          paddingBottom: 8,
          background: T.bg,
          borderColor: T.border,
          color: T.text,
        }}
      />
    </div>
  );
}
