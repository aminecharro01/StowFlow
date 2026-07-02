"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { en, statusEn } from "./en";
import { fr, statusFr } from "./fr";

export type Locale = "fr" | "en";
export type Translations = typeof fr;

const LOCALE_KEY = "stowflow_locale_v1";

const dictionaries = { fr, en } as const;

type LocaleContextValue = {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: Translations;
  statusLabel: (status: string) => string;
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

function readStoredLocale(): Locale {
  if (typeof window === "undefined") return "fr";
  const stored = localStorage.getItem(LOCALE_KEY);
  return stored === "en" ? "en" : "fr";
}

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("fr");

  useEffect(() => {
    setLocaleState(readStoredLocale());
  }, []);

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    localStorage.setItem(LOCALE_KEY, next);
    document.documentElement.lang = next;
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const value = useMemo<LocaleContextValue>(() => {
    const t = dictionaries[locale] as Translations;
    const statusLabel = locale === "en" ? statusEn : statusFr;
    return { locale, setLocale, t, statusLabel };
  }, [locale, setLocale]);

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useT() {
  const ctx = useContext(LocaleContext);
  if (!ctx) {
    return {
      locale: "fr" as Locale,
      setLocale: () => {},
      t: fr,
      statusLabel: statusFr,
    };
  }
  return ctx;
}
