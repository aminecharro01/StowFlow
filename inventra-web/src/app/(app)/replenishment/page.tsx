"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { Badge } from "@/components/inventra/ui/Badge";
import { TH, TD } from "@/components/inventra/ui/Table";
import { Icons } from "@/components/inventra/Icons";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet, apiSend } from "@/lib/api";
import { fetchAllArticles } from "@/lib/types/article";
import { loadSession } from "@/lib/auth";
import { canCreateReplenishment, canProcessReplenishment } from "@/lib/rolePrivileges";
import { assertWithinMaxOrderable, maxOrderableQuantity } from "@/lib/stockLimits";
import { AlertBanner, useConfirm } from "@/components/inventra/ui/useConfirm";
import type { Article } from "@/app/(app)/products/page";

type Row = {
  id: number;
  reference: string;
  articleId: number;
  articleName: string;
  articleSku: string;
  supplierId: number;
  supplierName: string;
  quantity: number;
  status: string;
  note: string;
  requestedBy: string;
  createdAt: string;
};

type Summary = { pending: number; inProgress: number; received: number };
type SupplierOption = { id: number; name: string };

const FILTER_PILLS = ["All", "Pending", "In Progress", "Received", "Rejected", "Cancelled"] as const;

export default function ReplenishmentPage() {
  const { t, statusLabel } = useT();
  const router = useRouter();
  const searchParams = useSearchParams();
  const deepLinkHandled = useRef(false);
  const canCreate = canCreateReplenishment(loadSession()?.role);
  const canProcess = canProcessReplenishment(loadSession()?.role);
  const { confirm, dialog } = useConfirm();
  const [rows, setRows] = useState<Row[]>([]);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [filter, setFilter] = useState<string>("All");
  const [showCreate, setShowCreate] = useState(false);
  const [articles, setArticles] = useState<Article[]>([]);
  const [suppliers, setSuppliers] = useState<SupplierOption[]>([]);
  const [articleId, setArticleId] = useState("");
  const [supplierId, setSupplierId] = useState("");
  const [qty, setQty] = useState("1");
  const [note, setNote] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(() => {
    const q = filter === "All" ? "" : `?status=${encodeURIComponent(filter)}`;
    apiGet<Row[]>(`/api/replenishment-requests${q}`).then(setRows).catch(() => setRows([]));
    apiGet<Summary>("/api/replenishment-requests/summary").then(setSummary).catch(() => setSummary(null));
  }, [filter]);

  useEffect(() => {
    reload();
  }, [reload]);

  const replenishmentArticles = useMemo(
    () => articles.filter((a) => a.replenishmentEligible !== false),
    [articles],
  );

  useEffect(() => {
    if (!showCreate) return;
    fetchAllArticles()
      .then((list) =>
        setArticles(
          list.map((r) => ({
            id: r.id,
            name: r.name,
            sku: r.sku,
            category: r.category,
            qty: r.qty,
            price: typeof r.price === "number" ? r.price : Number(r.price),
            status: r.status,
            minThreshold: r.minThreshold,
            maxThreshold: r.maxThreshold,
            replenishmentEligible: r.replenishmentEligible,
            purchaseOrderEligible: r.purchaseOrderEligible,
          }))
        )
      )
      .catch(() => setArticles([]));
    apiGet<SupplierOption[]>("/api/suppliers")
      .then((s) => {
        setSuppliers(s.map((x) => ({ id: x.id, name: x.name })));
        if (s.length > 0) setSupplierId(String(s[0].id));
      })
      .catch(() => setSuppliers([]));
  }, [showCreate]);

    useEffect(() => {
    if (deepLinkHandled.current) return;
    if (searchParams.get("create") !== "1") return;
    const aid = searchParams.get("articleId");
    if (!aid) return;
    deepLinkHandled.current = true;
    const qQty = searchParams.get("qty");
    router.replace("/replenishment");
    if (!canCreate) return;
    setArticleId(aid);
    if (qQty && /^\d+$/.test(qQty)) setQty(qQty);
    setShowCreate(true);
  }, [searchParams, canCreate, router]);

  useEffect(() => {
    const a = articles.find((x) => String(x.id) === articleId);
    if (a?.suggestedReplenishmentQty != null) {
      setQty(String(a.suggestedReplenishmentQty));
    }
  }, [articleId, articles]);

  const kpis = useMemo(
    () =>
      summary
        ? [
            { label: t.replenishment.kpiPending, value: String(summary.pending), color: T.warning },
            { label: t.replenishment.kpiInProgress, value: String(summary.inProgress), color: T.primary },
            { label: t.replenishment.kpiReceived, value: String(summary.received), color: T.success },
          ]
        : [
            { label: t.replenishment.kpiPending, value: "—", color: T.warning },
            { label: t.replenishment.kpiInProgress, value: "—", color: T.primary },
            { label: t.replenishment.kpiReceived, value: "—", color: T.success },
          ],
    [summary],
  );

  async function createRequest(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setMsg(null);
    const aid = Number(articleId);
    const sid = Number(supplierId);
    if (!aid || !sid) {
      setErr(t.replenishment.formInvalid);
      return;
    }
    const quantity = Math.floor(Number(qty.replace(",", ".")));
    if (!Number.isFinite(quantity) || quantity < 1) {
      setErr(t.replenishment.invalidQty);
      return;
    }
    const article = articles.find((a) => a.id === aid);
    if (article) {
      try {
        assertWithinMaxOrderable(
          article,
          quantity,
          t.replenishment.qtyExceedsMax(article.sku, maxOrderableQuantity(article), article.maxThreshold ?? 0)
        );
      } catch (ex) {
        setErr((ex as Error).message);
        return;
      }
    }
    setBusy(true);
    try {
      await apiSend("/api/replenishment-requests", "POST", {
        articleId: aid,
        supplierId: sid,
        quantity,
        note: note.trim() || null,
      });
      setShowCreate(false);
      setArticleId("");
      setQty("1");
      setNote("");
      setMsg(t.replenishment.created);
      reload();
    } catch (ex) {
      setErr((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function patchStatus(id: number, status: string) {
    setErr(null);
    try {
      await apiSend(`/api/replenishment-requests/${id}/status`, "PATCH", { status });
      setMsg(t.replenishment.updated);
      reload();
    } catch (ex) {
      setErr((ex as Error).message);
    }
  }

  async function receive(id: number) {
    const ok = await confirm({
      title: t.confirm.receiveReplenishment.title,
      message: t.confirm.receiveReplenishment.message,
      confirmLabel: t.confirm.receiveReplenishment.confirm,
      variant: "success",
    });
    if (!ok) return;
    setErr(null);
    try {
      await apiSend(`/api/replenishment-requests/${id}/receive`, "POST");
      setMsg(t.replenishment.received);
      reload();
    } catch (ex) {
      setErr((ex as Error).message);
    }
  }

  return (
    <>
      {dialog}
      <Topbar title={t.replenishment.title} subtitle={t.replenishment.subtitle} />
      <Page>
        <div className="rounded-lg px-4 py-3 mb-4 text-sm leading-relaxed border" style={{ background: "#F0F9FF", borderColor: T.border, color: T.text }}>
          {t.replenishment.createContextHint}
          <span className="block mt-2 text-xs" style={{ color: T.text2 }}>
            {t.replenishment.onlyAlertProducts}
          </span>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 mb-4">
          <div>
            <h2 className="text-xl font-bold" style={{ color: T.text }}>
              {t.replenishment.heading}
            </h2>
            {!canProcess && canCreate && (
              <p className="text-xs mt-2 rounded-lg px-3 py-2 inline-block" style={{ background: "#EFF6FF", color: T.text2 }}>
                {t.replenishment.processReadOnly}
              </p>
            )}
          </div>
          {canCreate && (
            <Btn type="button" onClick={() => setShowCreate(true)}>
              {Icons.plus} {t.replenishment.add}
            </Btn>
          )}
        </div>

        {msg && (
          <AlertBanner variant="success" className="mb-4" onDismiss={() => setMsg(null)}>
            {msg}
          </AlertBanner>
        )}
        {err && (
          <AlertBanner variant="error" className="mb-4" onDismiss={() => setErr(null)}>
            {err}
          </AlertBanner>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
          {kpis.map((k) => (
            <Card key={k.label} className="p-5">
              <p className="text-2xl font-bold" style={{ color: k.color }}>
                {k.value}
              </p>
              <p className="text-xs mt-1" style={{ color: T.text2 }}>
                {k.label}
              </p>
            </Card>
          ))}
        </div>

        <Card className="px-3 py-2 mb-4 flex flex-wrap gap-1.5">
          {FILTER_PILLS.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => setFilter(p)}
              className="px-3 py-1.5 rounded-lg text-xs font-semibold"
              style={
                filter === p ? { background: T.primary, color: "#fff" } : { background: T.bg, color: T.text2 }
              }
            >
              {p === "All" ? t.filters.all : statusLabel(p)}
            </button>
          ))}
        </Card>

        {showCreate && (
          <Card className="p-6 mb-6 max-w-xl">
            <h3 className="text-sm font-bold mb-4" style={{ color: T.text }}>
              {t.replenishment.createTitle}
            </h3>
            <form onSubmit={createRequest} className="flex flex-col gap-3">
              <div>
                <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
                  {t.replenishment.fieldProduct}
                </label>
                <select
                  value={articleId}
                  onChange={(e) => setArticleId(e.target.value)}
                  required
                  className="w-full rounded-lg border text-sm px-3 py-2"
                  style={{ borderColor: T.border, background: T.bg, color: T.text }}
                >
                  <option value="">{t.replenishment.chooseProduct}</option>
                  {replenishmentArticles.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name} ({a.sku}) — {a.qty} {t.dashboard.units} — {a.status}
                    </option>
                  ))}
                </select>
                {replenishmentArticles.length === 0 && (
                  <p className="text-xs mt-1" style={{ color: T.warning }}>
                    {t.replenishment.onlyAlertProducts}
                  </p>
                )}
              </div>
              <div>
                <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
                  {t.replenishment.fieldSupplier}
                </label>
                <select
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value)}
                  required
                  className="w-full rounded-lg border text-sm px-3 py-2"
                  style={{ borderColor: T.border, background: T.bg, color: T.text }}
                >
                  {suppliers.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
                  {t.replenishment.fieldQty}
                </label>
                <Input
                  value={qty}
                  type="number"
                  min={1}
                  onChange={(e) => setQty(e.target.value)}
                />
                {(() => {
                  const picked = articles.find((a) => String(a.id) === articleId);
                  if (!picked || picked.maxThreshold == null) return null;
                  return (
                    <p className="text-[10px] mt-1" style={{ color: T.text2 }}>
                      {t.replenishment.maxOrderableHint(
                        picked.qty ?? 0,
                        maxOrderableQuantity(picked),
                        picked.maxThreshold
                      )}
                    </p>
                  );
                })()}
                <p className="text-[10px] mt-1" style={{ color: T.text2 }}>
                  {t.replenishment.autoQtyHint}
                </p>
              </div>
              <Input value={note} onChange={(e) => setNote(e.target.value)} placeholder={t.replenishment.notePh} />
              <div className="flex gap-2 justify-end">
                <Btn type="button" variant="ghost" onClick={() => setShowCreate(false)}>
                  {t.common.cancel}
                </Btn>
                <Btn type="submit" disabled={busy}>
                  {busy ? t.common.saving : t.replenishment.submitRequest}
                </Btn>
              </div>
            </form>
          </Card>
        )}

        <Card className="overflow-x-auto">
          <table className="w-full border-collapse min-w-[960px]">
            <thead>
              <tr>
                <TH>{t.replenishment.colRef}</TH>
                <TH>{t.replenishment.colProduct}</TH>
                <TH>{t.replenishment.colSupplier}</TH>
                <TH>{t.replenishment.colQty}</TH>
                <TH>{t.replenishment.colStatus}</TH>
                <TH>{t.replenishment.colRequested}</TH>
                {canProcess && <TH>{t.replenishment.colActions}</TH>}
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-t hover:bg-gray-50" style={{ borderColor: T.border }}>
                  <TD>
                    <span className="font-mono text-xs font-semibold" style={{ color: T.primary }}>
                      {r.reference}
                    </span>
                  </TD>
                  <TD>
                    <span className="font-medium block">{r.articleName}</span>
                    <span className="text-xs font-mono" style={{ color: T.text2 }}>
                      {r.articleSku}
                    </span>
                  </TD>
                  <TD>{r.supplierName}</TD>
                  <TD className="font-semibold">{r.quantity}</TD>
                  <TD>
                    <Badge status={r.status} />
                  </TD>
                  <TD>
                    <span className="text-xs block">{r.requestedBy}</span>
                    <span className="text-xs" style={{ color: T.text2 }}>
                      {r.createdAt}
                    </span>
                  </TD>
                  {canProcess && (
                    <TD>
                      <div className="flex flex-col gap-1 min-w-[140px]">
                        {r.status === "Pending" && (
                          <>
                            <Btn size="sm" type="button" onClick={() => void patchStatus(r.id, "In Progress")}>
                              {t.replenishment.approve}
                            </Btn>
                            <Btn size="sm" type="button" variant="ghost" onClick={() => void patchStatus(r.id, "Rejected")}>
                              {t.replenishment.reject}
                            </Btn>
                          </>
                        )}
                        {(r.status === "Pending" || r.status === "In Progress") && (
                          <Btn size="sm" type="button" onClick={() => void receive(r.id)}>
                            {t.replenishment.markReceived}
                          </Btn>
                        )}
                        {r.status === "Received" && (
                          <span className="text-xs" style={{ color: T.success }}>
                            {t.replenishment.resolved}
                          </span>
                        )}
                      </div>
                    </TD>
                  )}
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={canProcess ? 7 : 6} className="py-10 text-center text-sm" style={{ color: T.text2 }}>
                    {t.replenishment.empty}
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
