"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Btn } from "@/components/inventra/ui/Btn";
import { Card } from "@/components/inventra/ui/Card";
import { Input } from "@/components/inventra/ui/Input";
import { AlertBanner, useConfirm } from "@/components/inventra/ui/useConfirm";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet, apiSend } from "@/lib/api";
import { loadSession } from "@/lib/auth";

type UserRow = { id: number; email: string; role: string; enabled: boolean };

const BASE_ROLES = ["STOCK_MANAGER", "SALES", "MANAGER"] as const;

export function SettingsUsersPanel() {
  const { t } = useT();
  const session = loadSession();
  const { confirm, dialog } = useConfirm();
  const roleOptions = useMemo(() => {
    const r = session?.role;
    if (r === "SUPER_ADMIN") return ["TENANT_ADMIN", ...BASE_ROLES] as string[];
    return [...BASE_ROLES] as string[];
  }, [session?.role]);

  const [rows, setRows] = useState<UserRow[]>([]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [newRole, setNewRole] = useState<string>(roleOptions[0] ?? "STOCK_MANAGER");
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(() => {
    apiGet<UserRow[]>("/api/admin/users")
      .then(setRows)
      .catch(() => setRows([]));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (roleOptions.length && !roleOptions.includes(newRole)) {
      setNewRole(roleOptions[0]);
    }
  }, [roleOptions, newRole]);

  async function createUser(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setMsg(null);
    setBusy(true);
    try {
      await apiSend("/api/admin/users", "POST", {
        email: email.trim(),
        password,
        role: newRole,
      });
      setEmail("");
      setPassword("");
      setMsg(t.settings.usersCreated);
      reload();
    } catch (ex) {
      setErr((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function patchUser(id: number, body: { role?: string; enabled?: boolean }) {
    setErr(null);
    setMsg(null);
    try {
      await apiSend(`/api/admin/users/${id}`, "PATCH", body);
      setMsg(t.settings.usersUpdated);
      reload();
    } catch (ex) {
      setErr((ex as Error).message);
    }
  }

  async function toggleEnabled(u: UserRow) {
    if (u.enabled) {
      const ok = await confirm({
        title: t.confirm.disableUser.title,
        message: t.confirm.disableUser.message(u.email),
        confirmLabel: t.confirm.disableUser.confirm,
        variant: "danger",
        detail: u.email,
      });
      if (!ok) return;
    } else {
      const ok = await confirm({
        title: t.confirm.enableUser.title,
        message: t.confirm.enableUser.message(u.email),
        confirmLabel: t.confirm.enableUser.confirm,
        variant: "success",
        detail: u.email,
      });
      if (!ok) return;
    }
    await patchUser(u.id, { enabled: !u.enabled });
  }

  async function changeRole(u: UserRow, role: string) {
    if (role === u.role) return;
    const roleLabel = (t.roles as Record<string, string>)[role] ?? role;
    const ok = await confirm({
      title: t.confirm.changeUserRole.title,
      message: t.confirm.changeUserRole.message(u.email, roleLabel),
      confirmLabel: t.confirm.changeUserRole.confirm,
      variant: "warning",
      detail: u.email,
    });
    if (!ok) return;
    await patchUser(u.id, { role });
  }

  return (
    <>
      {dialog}
      <Card className="p-6 max-w-4xl">
        <h2 className="text-base font-bold mb-1" style={{ color: T.text }}>
          {t.settings.usersTitle}
        </h2>
        <p className="text-sm mb-6" style={{ color: T.text2 }}>
          {t.settings.usersLead}
        </p>

        {msg && (
          <AlertBanner variant="success" className="mb-3" onDismiss={() => setMsg(null)}>
            {msg}
          </AlertBanner>
        )}
        {err && (
          <AlertBanner variant="error" className="mb-3" onDismiss={() => setErr(null)}>
            {err}
          </AlertBanner>
        )}

        <form onSubmit={createUser} className="flex flex-col gap-3 mb-8 pb-8 border-b" style={{ borderColor: T.border }}>
          <p className="text-sm font-semibold" style={{ color: T.text }}>
            {t.settings.usersNew}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input placeholder={t.settings.usersEmailPh} value={email} onChange={(e) => setEmail(e.target.value)} required />
            <Input
              type="password"
              placeholder={t.settings.usersPasswordPh}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="new-password"
            />
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <div>
              <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
                {t.settings.usersRole}
              </label>
              <select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value)}
                className="rounded-lg border text-sm px-3 py-2 min-w-[200px]"
                style={{ borderColor: T.border, color: T.text, background: T.bg }}
              >
                {roleOptions.map((r) => (
                  <option key={r} value={r}>
                    {(t.roles as Record<string, string>)[r] ?? r}
                  </option>
                ))}
              </select>
              <p className="text-xs mt-1 max-w-md" style={{ color: T.text2 }}>
                {(t.roleScopes as Record<string, string>)[newRole] ?? ""}
              </p>
            </div>
            <Btn type="submit" disabled={busy}>
              {busy ? t.common.saving : t.settings.usersCreateBtn}
            </Btn>
          </div>
        </form>

        <div className="overflow-x-auto">
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="text-left border-b" style={{ borderColor: T.border }}>
                <th className="py-2 pr-4 font-semibold" style={{ color: T.text2 }}>
                  E-mail
                </th>
                <th className="py-2 pr-4 font-semibold" style={{ color: T.text2 }}>
                  {t.settings.usersRole}
                </th>
                <th className="py-2 pr-4 font-semibold" style={{ color: T.text2 }}>
                  {t.settings.usersActive}
                </th>
                <th className="py-2 font-semibold" style={{ color: T.text2 }}>
                  {t.settings.usersActions}
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <tr key={u.id} className="border-b" style={{ borderColor: T.border }}>
                  <td className="py-2 pr-4 font-medium" style={{ color: T.text }}>
                    {u.email}
                  </td>
                  <td className="py-2 pr-4">
                    {session?.role !== "SUPER_ADMIN" && u.role === "TENANT_ADMIN" ? (
                      <span style={{ color: T.text }}>{t.roles.TENANT_ADMIN}</span>
                    ) : (
                      <select
                        value={u.role}
                        onChange={(e) => void changeRole(u, e.target.value)}
                        className="rounded border text-xs px-2 py-1 max-w-[220px]"
                        style={{ borderColor: T.border, background: T.bg }}
                      >
                        {(() => {
                          const standard =
                            session?.role === "SUPER_ADMIN"
                              ? ["TENANT_ADMIN", "STOCK_MANAGER", "SALES", "MANAGER"]
                              : ["STOCK_MANAGER", "SALES", "MANAGER"];
                          const opts = standard.includes(u.role) ? standard : [...standard, u.role];
                          return opts.map((r) => (
                            <option key={r} value={r}>
                              {(t.roles as Record<string, string>)[r] ?? r}
                            </option>
                          ));
                        })()}
                      </select>
                    )}
                  </td>
                  <td className="py-2 pr-4">{u.enabled ? t.common.yes : t.common.no}</td>
                  <td className="py-2">
                    <Btn
                      variant={u.enabled ? "ghost" : "primary"}
                      size="sm"
                      onClick={() => void toggleEnabled(u)}
                    >
                      {u.enabled ? t.settings.usersDisable : t.settings.usersEnable}
                    </Btn>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </>
  );
}
