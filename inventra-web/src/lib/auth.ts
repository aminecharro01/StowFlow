const STORAGE_KEY = "stowflow_session_v2";
const LEGACY_SESSION_KEYS = [
  "stocki_session_v2",
  "stocki_session_v1",
  "stowflow_session_v1",
  "inventra_session_v1",
] as const;
const PLATFORM_TENANT_KEY = "stowflow_platform_tenant";
const LEGACY_PLATFORM_TENANT_KEYS = ["stocki_platform_tenant"] as const;

/** URL API — vide = proxy Next.js `/api` → backend (voir next.config.ts). */
export const API_BASE = process.env.NEXT_PUBLIC_API_URL?.trim() || "";

export type SessionPayload = {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
  email: string;
  role: string;
  tenantSlug: string | null;
};

export function loadSession(): SessionPayload | null {
  if (typeof window === "undefined") return null;
  try {
    let raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      for (const legacyKey of LEGACY_SESSION_KEYS) {
        raw = sessionStorage.getItem(legacyKey);
        if (raw) {
          sessionStorage.removeItem(legacyKey);
          break;
        }
      }
    }
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<SessionPayload>;
    if (!parsed.accessToken || !parsed.email || !parsed.role) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    if (parsed.expiresAt && Date.now() >= parsed.expiresAt) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed as SessionPayload;
  } catch {
    return null;
  }
}

export function saveSession(payload: SessionPayload) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

export function clearSession() {
  sessionStorage.removeItem(STORAGE_KEY);
  for (const legacyKey of LEGACY_SESSION_KEYS) {
    sessionStorage.removeItem(legacyKey);
  }
}

export async function remoteLogin(email: string, password: string): Promise<SessionPayload> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: email.trim(), password }),
    });
  } catch {
    throw new Error(
      "Impossible de joindre l’API. Démarrez le backend : cd stowflow-api puis .\\run-dev.ps1"
    );
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    const msg = (err as { error?: string }).error;
    if (res.status === 401) {
      throw new Error(
        msg ??
          "Identifiants invalides. Vérifiez votre e-mail et mot de passe."
      );
    }
    throw new Error(msg ?? `Connexion impossible (${res.status})`);
  }
  const data = (await res.json()) as {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    email: string;
    role: string;
    tenantSlug: string | null;
  };
  return {
    accessToken: data.accessToken,
    tokenType: data.tokenType || "Bearer",
    expiresAt: Date.now() + data.expiresIn * 1000,
    email: data.email,
    role: data.role,
    tenantSlug: data.tenantSlug,
  };
}

/** Contexte tenant choisi par le super admin (gestion d’un espace client). */
export function getPlatformTenantSlug(): string | null {
  if (typeof window === "undefined") return null;
  let v = sessionStorage.getItem(PLATFORM_TENANT_KEY);
  if (!v) {
    for (const legacyKey of LEGACY_PLATFORM_TENANT_KEYS) {
      v = sessionStorage.getItem(legacyKey);
      if (v) {
        sessionStorage.removeItem(legacyKey);
        sessionStorage.setItem(PLATFORM_TENANT_KEY, v);
        break;
      }
    }
  }
  return v && v.length > 0 ? v : null;
}

export function setPlatformTenantSlug(slug: string) {
  sessionStorage.setItem(PLATFORM_TENANT_KEY, slug.trim().toLowerCase());
}

/** Slug tenant pour l’en-tête API (super admin → contexte plateforme ou défaut démo). */
export function getTenantSlug(): string {
  const s = loadSession();
  if (s?.role === "SUPER_ADMIN") {
    const platform = getPlatformTenantSlug();
    if (platform) return platform;
  }
  if (s?.tenantSlug && s.tenantSlug.length > 0) return s.tenantSlug;
  return process.env.NEXT_PUBLIC_TENANT_SLUG ?? "default";
}

export function getAccessToken(): string | null {
  return loadSession()?.accessToken ?? null;
}
