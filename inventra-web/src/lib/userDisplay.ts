/** Primary presenter name for demo / project branding. */
export const APP_OWNER_NAME = "Amine Charro";

const DEMO_EMAIL_SUFFIXES = ["@stowflow.demo", "@default.demo"];

function formatFromEmail(email: string): string {
  const local = email.split("@")[0] ?? email;
  const parts = local.split(/[.\-_]/).filter(Boolean);
  if (parts.length >= 2) {
    return parts.map((p) => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase()).join(" ");
  }
  return local.charAt(0).toUpperCase() + local.slice(1).toLowerCase();
}

/** Display name for sidebar and dashboard greeting. Demo accounts show Amine Charro. */
export function getDisplayName(email?: string | null): string {
  if (!email) return APP_OWNER_NAME;
  const lower = email.toLowerCase();
  if (DEMO_EMAIL_SUFFIXES.some((s) => lower.endsWith(s)) || lower.includes(".demo")) {
    return APP_OWNER_NAME;
  }
  return formatFromEmail(email);
}

export function initialsFromName(name: string): string {
  const bits = name.split(/\s+/).filter(Boolean);
  if (bits.length >= 2) return (bits[0][0] + bits[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}
