import { API_BASE, clearSession, getAccessToken, getTenantSlug } from "./auth";

export { getTenantSlug };

function authHeaders(): Record<string, string> {
  const token = getAccessToken();
  if (!token) return {};
  return { Authorization: `Bearer ${token}` };
}

function handleUnauthorized(status: number) {
  if (status === 401 && typeof window !== "undefined") {
    clearSession();
    window.location.href = "/login";
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "X-Tenant-Slug": getTenantSlug(), ...authHeaders() },
    cache: "no-store",
  });
  if (!res.ok) {
    handleUnauthorized(res.status);
    const err = await res.json().catch(() => ({}));
    throw new Error((err as { error?: string }).error ?? res.statusText);
  }
  return res.json() as Promise<T>;
}

export async function apiGetBlob(path: string): Promise<Blob> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "X-Tenant-Slug": getTenantSlug(), ...authHeaders() },
    cache: "no-store",
  });
  if (!res.ok) {
    handleUnauthorized(res.status);
    const err = await res.json().catch(() => ({}));
    throw new Error((err as { error?: string }).error ?? res.statusText);
  }
  return res.blob();
}

/** Ouvre un PDF reçu du backend (auth Bearer / session). Révoque l’URL objet après délai. */
export function openPdfBlob(blob: Blob) {
  const url = URL.createObjectURL(blob);
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 120_000);
}

export async function apiSend<T>(
  path: string,
  method: "POST" | "PUT" | "PATCH" | "DELETE",
  body?: unknown
): Promise<T | void> {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      "X-Tenant-Slug": getTenantSlug(),
      ...authHeaders(),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    handleUnauthorized(res.status);
    const err = await res.json().catch(() => ({}));
    throw new Error((err as { error?: string }).error ?? res.statusText);
  }
  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return;
  }
  const text = await res.text();
  if (!text) return;
  return JSON.parse(text) as T;
}
