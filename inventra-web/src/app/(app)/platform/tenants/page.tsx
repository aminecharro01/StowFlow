"use client";

import { useCallback, useEffect, useState } from "react";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { TH, TD } from "@/components/inventra/ui/Table";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet, apiSend } from "@/lib/api";
import { getPlatformTenantSlug, setPlatformTenantSlug } from "@/lib/auth";
import { CircleCheck, Info } from "lucide-react";

type TenantRow = { id: number; slug: string; name: string; userCount: number; ready: boolean };

function FieldLabel({ children, hint }: { children: React.ReactNode; hint?: string }) {
  return (
    <div>
      <span className="text-xs font-semibold block mb-1" style={{ color: T.text }}>
        {children}
      </span>
      {hint ? (
        <span className="text-xs block mb-1.5 leading-relaxed" style={{ color: T.text2 }}>
          {hint}
        </span>
      ) : null}
    </div>
  );
}

export default function PlatformTenantsPage() {
  const { t } = useT();
  const [rows, setRows] = useState<TenantRow[]>([]);
  const [slug, setSlug] = useState("");
  const [name, setName] = useState("");
  const [adminEmail, setAdminEmail] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [activeSlug, setActiveSlug] = useState<string | null>(null);

  const reload = useCallback(() => {
    apiGet<TenantRow[]>("/api/platform/tenants")
      .then(setRows)
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    reload();
    setActiveSlug(getPlatformTenantSlug());
  }, [reload]);

  function selectTenantContext(tenantSlug: string) {
    setPlatformTenantSlug(tenantSlug);
    setActiveSlug(tenantSlug);
  }

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setMsg(null);
    setBusy(true);
    try {
      const trimmedName = name.trim();
      const trimmedSlug = slug.trim().toLowerCase();
      const trimmedAdminEmail = adminEmail.trim().toLowerCase();
      await apiSend("/api/platform/tenants", "POST", {
        slug: trimmedSlug,
        name: trimmedName,
        adminEmail: trimmedAdminEmail,
        adminPassword,
      });
      setSlug("");
      setName("");
      setAdminEmail("");
      setAdminPassword("");
      setMsg(t.platform.created(trimmedName, trimmedAdminEmail));
      selectTenantContext(trimmedSlug);
      reload();
    } catch (ex) {
      setErr((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Topbar title={t.platform.title} subtitle={t.platform.subtitle} />
      <Page>
        {activeSlug && (
          <div
            className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 rounded-xl border px-4 py-3"
            style={{ borderColor: T.border, background: "#EFF6FF" }}
          >
            <p className="text-sm" style={{ color: T.text }}>
              <span className="font-semibold">{t.platform.contextLabel} : </span>
              {t.platform.contextHint(activeSlug)}
            </p>
            <span
              className="text-xs font-semibold px-2.5 py-1 rounded-lg shrink-0"
              style={{ background: T.primary, color: "#fff" }}
            >
              {t.platform.selectedTenant} — {activeSlug}
            </span>
          </div>
        )}

        <form onSubmit={onCreate} className="mb-8">
          <Card className="p-6 max-w-2xl">
            <h2 className="text-sm font-bold mb-2" style={{ color: T.text }}>
              {t.platform.createSectionTitle}
            </h2>
            <div
              className="flex gap-2.5 rounded-lg px-3 py-3 mb-5 text-sm leading-relaxed"
              style={{ background: "#F0F9FF", color: T.text }}
            >
              <Info size={18} className="shrink-0 mt-0.5" style={{ color: T.primary }} aria-hidden />
              <p>{t.platform.createLead}</p>
            </div>

            <div className="flex flex-col gap-4">
              <div>
                <FieldLabel hint={t.platform.slugHint}>{t.platform.slugLabel}</FieldLabel>
                <Input
                  placeholder={t.platform.slugPh}
                  value={slug}
                  onChange={(e) => setSlug(e.target.value)}
                  required
                  autoComplete="off"
                />
              </div>
              <div>
                <FieldLabel>{t.platform.nameLabel}</FieldLabel>
                <Input
                  placeholder={t.platform.namePh}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
              <div>
                <FieldLabel hint={t.platform.adminEmailHint}>{t.platform.adminEmailLabel}</FieldLabel>
                <Input
                  type="email"
                  placeholder={t.platform.adminEmailPh}
                  value={adminEmail}
                  onChange={(e) => setAdminEmail(e.target.value)}
                  required
                  autoComplete="off"
                />
              </div>
              <div>
                <FieldLabel>{t.platform.adminPasswordLabel}</FieldLabel>
                <Input
                  type="password"
                  placeholder={t.platform.adminPasswordPh}
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                />
              </div>
              {msg && (
                <p
                  className="text-sm rounded-lg px-3 py-2 flex gap-2 items-start"
                  style={{ background: "#ECFDF5", color: T.success }}
                >
                  <CircleCheck size={18} className="shrink-0 mt-0.5" aria-hidden />
                  <span>{msg}</span>
                </p>
              )}
              {err && (
                <p className="text-sm rounded-lg px-3 py-2" style={{ background: "#FEF2F2", color: T.danger }}>
                  {err}
                </p>
              )}
              <Btn type="submit" disabled={busy}>
                {busy ? t.common.saving : t.platform.createBtn}
              </Btn>
            </div>
          </Card>
        </form>

        <Card className="overflow-hidden overflow-x-auto">
          <div className="px-6 py-4 border-b" style={{ borderColor: T.border }}>
            <h2 className="text-base font-bold" style={{ color: T.text }}>
              {t.platform.listTitle}
            </h2>
          </div>
          <table className="w-full border-collapse min-w-[640px]">
            <thead>
              <tr className="border-b" style={{ borderColor: T.border }}>
                <TH>Slug</TH>
                <TH>Nom</TH>
                <TH>{t.platform.colStatus}</TH>
                <TH>Comptes</TH>
                <TH>{t.platform.colAction}</TH>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const isActive = activeSlug === row.slug;
                return (
                  <tr
                    key={row.id}
                    className="border-t hover:bg-gray-50"
                    style={{
                      borderColor: T.border,
                      background: isActive ? "rgba(37, 99, 235, 0.04)" : undefined,
                    }}
                  >
                    <TD>
                      <span className="font-mono text-sm font-semibold" style={{ color: T.primary }}>
                        {row.slug}
                      </span>
                    </TD>
                    <TD className="font-medium">{row.name}</TD>
                    <TD>
                      <span
                        className="text-xs font-semibold px-2 py-1 rounded-lg inline-flex items-center gap-1"
                        style={{
                          background: row.ready ? "rgba(34,197,94,0.12)" : "rgba(245,158,11,0.12)",
                          color: row.ready ? T.success : T.warning,
                        }}
                      >
                        {row.ready ? (
                          <>
                            <CircleCheck size={14} aria-hidden />
                            {t.platform.statusReady}
                          </>
                        ) : (
                          t.platform.statusPending
                        )}
                      </span>
                    </TD>
                    <TD style={{ color: T.text2 }}>{t.platform.usersCount(row.userCount)}</TD>
                    <TD>
                      <Btn
                        type="button"
                        size="sm"
                        variant={isActive ? "ghost" : "primary"}
                        onClick={() => selectTenantContext(row.slug)}
                      >
                        {isActive ? t.platform.selectedTenant : t.platform.selectTenant}
                      </Btn>
                    </TD>
                  </tr>
                );
              })}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-10 text-center text-sm" style={{ color: T.text2 }}>
                    {t.platform.empty}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Card>
      </Page>
    </>
  );
}
