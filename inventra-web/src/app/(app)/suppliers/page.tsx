"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { Badge } from "@/components/inventra/ui/Badge";
import { TH, TD } from "@/components/inventra/ui/Table";
import { Icons } from "@/components/inventra/Icons";
import { T } from "@/lib/theme";
import { apiGet, apiSend } from "@/lib/api";
import { canEditSuppliers } from "@/lib/rolePrivileges";
import { loadSession } from "@/lib/auth";
import { useT } from "@/lib/i18n";
import { Modal } from "@/components/inventra/ui/Modal";
import { AlertBanner, useConfirm } from "@/components/inventra/ui/useConfirm";
import { ExternalActionLink } from "@/components/inventra/ExternalActionLink";

type Supplier = {
  id: number;
  name: string;
  contact: string;
  country: string;
  products: number;
  lead: string;
  status: string;
};

type SupplierStatusApi = "ACTIVE" | "INACTIVE" | "ON_HOLD";

function statusLabelToApi(status: string): SupplierStatusApi {
  switch (status) {
    case "Active":
      return "ACTIVE";
    case "Inactive":
      return "INACTIVE";
    case "On Hold":
      return "ON_HOLD";
    default:
      return status as SupplierStatusApi;
  }
}

function SupplierModal({
  supplier,
  onClose,
  onSaved,
}: {
  supplier: Supplier | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t } = useT();
  const isEdit = supplier != null;
  const empty = { name: "", email: "", country: "", lead: "7", status: "ACTIVE" as SupplierStatusApi };
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const statusOptions = useMemo(
    () =>
      [
        { api: "ACTIVE" as SupplierStatusApi, label: t.suppliers.statusActive },
        { api: "INACTIVE" as SupplierStatusApi, label: t.suppliers.statusInactive },
        { api: "ON_HOLD" as SupplierStatusApi, label: t.suppliers.statusOnHold },
      ],
    [t],
  );

  useEffect(() => {
    if (supplier) {
      setForm({
        name: supplier.name,
        email: supplier.contact,
        country: supplier.country,
        lead: String(parseInt(supplier.lead, 10) || 7),
        status: statusLabelToApi(supplier.status),
      });
    } else {
      setForm(empty);
    }
  }, [supplier]);

  const submit = async () => {
    setErr(null);
    setSaving(true);
    try {
      const body = {
        name: form.name.trim(),
        contactEmail: form.email.trim() || null,
        country: form.country.trim(),
        leadTimeDays: Number(form.lead) || 0,
        status: form.status,
      };
      if (isEdit && supplier) {
        await apiSend(`/api/suppliers/${supplier.id}`, "PUT", body);
      } else {
        await apiSend("/api/suppliers", "POST", body);
      }
      onSaved();
      onClose();
    } catch (e) {
      setErr((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      title={isEdit ? t.suppliers.modalEditTitle : t.suppliers.modalAddTitle}
      width="520px"
      footer={
        <>
          <Btn variant="ghost" onClick={onClose}>
            {t.common.cancel}
          </Btn>
          <Btn onClick={() => void submit()} disabled={saving || !form.name.trim()}>
            {saving ? t.common.saving : t.common.save}
          </Btn>
        </>
      }
    >
      <div className="px-6 py-5 flex flex-col gap-4">
        {err && <AlertBanner variant="error">{err}</AlertBanner>}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium" style={{ color: T.text2 }}>
              {t.suppliers.fieldName}
            </label>
            <Input placeholder={t.suppliers.phName} value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium" style={{ color: T.text2 }}>
              {t.suppliers.fieldEmail}
            </label>
            <Input
              type="email"
              placeholder={t.suppliers.phEmail}
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.suppliers.fieldCountry}
              </label>
              <Input
                placeholder={t.suppliers.phCountry}
                value={form.country}
                onChange={(e) => setForm((f) => ({ ...f, country: e.target.value }))}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.suppliers.fieldLead}
              </label>
              <Input
                type="number"
                min={0}
                placeholder="7"
                value={form.lead}
                onChange={(e) => setForm((f) => ({ ...f, lead: e.target.value }))}
              />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium" style={{ color: T.text2 }}>
              {t.suppliers.fieldStatus}
            </label>
            <select
              value={form.status}
              onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as SupplierStatusApi }))}
              className="w-full rounded-lg border text-sm px-3 py-2 outline-none focus:ring-2 focus:ring-blue-100"
              style={{ background: T.bg, borderColor: T.border, color: T.text }}
            >
              {statusOptions.map((o) => (
                <option key={o.api} value={o.api}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
      </div>
    </Modal>
  );
}

export default function SuppliersPage() {
  const { t } = useT();
  const canEdit = canEditSuppliers(loadSession()?.role);
  const { confirm, dialog } = useConfirm();
  const [search, setSearch] = useState("");
  const [rows, setRows] = useState<Supplier[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(() => {
    const q = search.trim() ? `?q=${encodeURIComponent(search.trim())}` : "";
    apiGet<Supplier[]>(`/api/suppliers${q}`).then(setRows).catch(() => setRows([]));
  }, [search]);

  useEffect(() => {
    const t = window.setTimeout(load, search ? 250 : 0);
    return () => window.clearTimeout(t);
  }, [load, search]);

  const stats = useMemo(() => {
    const active = rows.filter((r) => r.status === "Active").length;
    const avgLead =
      rows.length === 0
        ? "—"
        : `${(rows.reduce((s, r) => s + (parseInt(r.lead, 10) || 0), 0) / rows.length).toFixed(1)}j`;
    return [
      { label: t.suppliers.statTotal, value: String(rows.length), sub: t.suppliers.statTotalSub, color: T.primary },
      { label: t.suppliers.statActive, value: String(active), sub: t.suppliers.statActiveSub, color: T.success },
      { label: t.suppliers.statAvgLead, value: avgLead, sub: t.suppliers.statAvgLeadSub, color: T.accent },
    ];
  }, [rows]);

  function openCreate() {
    setEditing(null);
    setModalOpen(true);
  }

  function openEdit(s: Supplier) {
    setEditing(s);
    setModalOpen(true);
  }

  async function archive(id: number) {
    const ok = await confirm({
      title: t.confirm.archiveSupplier.title,
      message: t.confirm.archiveSupplier.message,
      confirmLabel: t.confirm.archiveSupplier.confirm,
      variant: "danger",
    });
    if (!ok) return;
    setErr(null);
    setMsg(null);
    try {
      await apiSend(`/api/suppliers/${id}`, "DELETE");
      setMsg(t.suppliers.archived);
      load();
    } catch (e) {
      setErr((e as Error).message);
    }
  }

  return (
    <>
      {dialog}
      <Topbar title={t.suppliers.title} subtitle={t.suppliers.subtitle} />
      <Page>
        <div className="flex items-start justify-between flex-col sm:flex-row gap-3">
          <div>
            <h2 className="text-xl font-bold" style={{ color: T.text }}>
              {t.suppliers.heading}
            </h2>
            <p className="text-sm mt-1" style={{ color: T.text2 }}>
              {t.suppliers.registered(rows.length)}
            </p>
            {!canEdit && (
              <p className="text-xs mt-2 rounded-lg px-3 py-2 inline-block max-w-xl" style={{ background: "#EFF6FF", color: T.text2 }}>
                {t.suppliers.readOnlyHint}
              </p>
            )}
          </div>
          {canEdit && (
            <Btn type="button" onClick={openCreate}>
              {Icons.plus} {t.suppliers.add}
            </Btn>
          )}
        </div>

        {msg && (
          <AlertBanner variant="success" className="mb-4 mt-4" onDismiss={() => setMsg(null)}>
            {msg}
          </AlertBanner>
        )}
        {err && (
          <AlertBanner variant="error" className="mb-4" onDismiss={() => setErr(null)}>
            {err}
          </AlertBanner>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {stats.map((s) => (
            <Card key={s.label} className="p-5 flex flex-col gap-1 overflow-hidden relative">
              <div className="absolute left-0 top-0 bottom-0 w-1 rounded-r-full" style={{ background: s.color }} />
              <p className="text-2xl font-bold pl-3" style={{ color: T.text }}>
                {s.value}
              </p>
              <p className="text-xs font-semibold pl-3" style={{ color: T.text }}>
                {s.label}
              </p>
              <p className="text-xs pl-3" style={{ color: T.text2 }}>
                {s.sub}
              </p>
            </Card>
          ))}
        </div>

        <Input
          placeholder={t.suppliers.searchPh}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          icon={Icons.search}
          className="max-w-sm"
        />

        <Card className="overflow-hidden overflow-x-auto">
          <table className="w-full border-collapse min-w-[800px]">
            <thead>
              <tr>
                <TH>{t.suppliers.colName}</TH>
                <TH>{t.suppliers.colContact}</TH>
                <TH>{t.suppliers.colCountry}</TH>
                <TH>{t.suppliers.colProducts}</TH>
                <TH>{t.suppliers.colLead}</TH>
                <TH>{t.suppliers.colStatus}</TH>
                {canEdit && <TH className="text-center">{t.suppliers.colActions}</TH>}
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id} className="border-t hover:bg-gray-50 transition-colors" style={{ borderColor: T.border }}>
                  <TD>
                    <Link
                      href={`/suppliers/${s.id}`}
                      className="font-semibold hover:underline"
                      style={{ color: T.primary }}
                    >
                      {s.name}
                    </Link>
                  </TD>
                  <TD>
                    {s.contact ? (
                      <ExternalActionLink
                        kind="email"
                        value={s.contact}
                        href={`mailto:${s.contact}`}
                        className="hover:underline text-xs bg-transparent border-0 p-0 cursor-pointer font-inherit text-left"
                        style={{ color: T.primary }}
                      >
                        {s.contact}
                      </ExternalActionLink>
                    ) : (
                      <span style={{ color: T.text2 }}>—</span>
                    )}
                  </TD>
                  <TD>{s.country || "—"}</TD>
                  <TD>
                    <span className="font-medium">{s.products}</span>
                  </TD>
                  <TD>
                    <span
                      className="font-medium"
                      style={{ color: parseInt(s.lead, 10) > 7 ? T.warning : T.success }}
                    >
                      {s.lead}
                    </span>
                  </TD>
                  <TD>
                    <Badge status={s.status} />
                  </TD>
                  {canEdit && (
                    <TD>
                      <div className="flex items-center justify-center gap-1.5">
                        <Link
                          href={`/suppliers/${s.id}`}
                          className="w-7 h-7 rounded-lg flex items-center justify-center"
                          style={{ color: T.accent, background: "rgba(124,58,237,0.1)" }}
                          title={t.suppliers.viewDetail}
                        >
                          {Icons.eye}
                        </Link>
                        <button
                          type="button"
                          className="w-7 h-7 rounded-lg flex items-center justify-center"
                          style={{ color: T.primary, background: "rgba(37,99,235,0.08)" }}
                          title={t.products.edit}
                          onClick={() => openEdit(s)}
                        >
                          {Icons.edit}
                        </button>
                        <button
                          type="button"
                          className="w-7 h-7 rounded-lg flex items-center justify-center"
                          style={{ color: T.danger, background: "rgba(239,68,68,0.08)" }}
                          title={t.suppliers.archive}
                          onClick={() => void archive(s.id)}
                        >
                          {Icons.trash}
                        </button>
                      </div>
                    </TD>
                  )}
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={canEdit ? 7 : 6} className="py-10 text-center text-sm" style={{ color: T.text2 }}>
                    {t.suppliers.empty}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          <div className="flex items-center justify-between px-4 py-3 border-t" style={{ borderColor: T.border }}>
            <span className="text-xs" style={{ color: T.text2 }}>
              {t.suppliers.showing(rows.length)}
            </span>
          </div>
        </Card>
      </Page>

      {modalOpen && (
        <SupplierModal
          supplier={editing}
          onClose={() => setModalOpen(false)}
          onSaved={() => {
            setMsg(t.suppliers.saved);
            load();
          }}
        />
      )}
    </>
  );
}
