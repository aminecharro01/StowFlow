"use client";

import { useT, type Locale } from "@/lib/i18n";
import { T } from "@/lib/theme";
import { Languages } from "lucide-react";

export function LanguageSwitcher({ compact }: { compact?: boolean }) {
  const { locale, setLocale, t } = useT();

  function toggle() {
    setLocale((locale === "fr" ? "en" : "fr") as Locale);
  }

  const label = locale === "fr" ? t.common.languageEn : t.common.languageFr;

  return (
    <button
      type="button"
      onClick={toggle}
      className="inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-semibold transition-colors hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1"
      style={{ borderColor: T.border, color: T.text }}
      title={t.common.language}
      aria-label={`${t.common.language}: ${label}`}
    >
      <Languages size={14} strokeWidth={2} aria-hidden />
      {!compact && <span>{label}</span>}
      {compact && <span className="uppercase">{locale === "fr" ? "EN" : "FR"}</span>}
    </button>
  );
}
