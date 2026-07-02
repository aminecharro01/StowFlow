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
import { LucideByName } from "@/components/inventra/LucideByName";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet, apiSend } from "@/lib/api";
import { fetchAllArticles } from "@/lib/types/article";
import {
  canCancelPurchaseOrder,
  canCreatePurchaseOrder,
  canReceivePurchaseOrder,
} from "@/lib/rolePrivileges";
import { loadSession } from "@/lib/auth";
import { assertWithinMaxOrderable, maxOrderableQuantity } from "@/lib/stockLimits";
import { Modal } from "@/components/inventra/ui/Modal";
import { AlertBanner, useConfirm } from "@/components/inventra/ui/useConfirm";
import type { Article } from "@/app/(app)/products/page";

type Order = {
  orderId: number;
  id: string;
  supplier: string;
  supplierId: number;
  date: string;
  items: number;
  total: string;
  status: string;
  tenantReceived: boolean;
};

type Summary = {
  totalOrders: number;
  pending: number;
  shipped: number;
  received: number;
  cancelled: number;
};

type Supplier = { id: number; name: string };

type DraftLine = { articleId: string; quantity: string; unitPrice: string };

export default function OrdersPage() {
  const { t, statusLabel } = useT();
  const router = useRouter();
  const searchParams = useSearchParams();
  const deepLinkHandled = useRef(false);
  const [statusFilter, setStatusFilter] = useState("All Orders");
  const [search, setSearch] = useState("");
  const [rows, setRows] = useState<Order[]>([]);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [articles, setArticles] = useState<Article[]>([]);
  const [supplierId, setSupplierId] = useState("");
  const [lines, setLines] = useState<DraftLine[]>([{ articleId: "", quantity: "1", unitPrice: "0" }]);
  const [createErr, setCreateErr] = useState<string | null>(null);
  const [createSaving, setCreateSaving] = useState(false);

  const canCreate = useMemo(() => canCreatePurchaseOrder(loadSession()?.role), []);

  const orderArticles = useMemo(
    () => articles.filter((a) => a.purchaseOrderEligible !== false),
    [articles],
  );
  const canReceive = useMemo(() => canReceivePurchaseOrder(loadSession()?.role), []);
  const canCancel = useMemo(() => canCancelPurchaseOrder(loadSession()?.role), []);
  const { confirm, dialog } = useConfirm();

  const pills = ["All Orders", "Pending", "Shipped", "Received", "Cancelled"];

  const refreshSummary = useCallback(() => {
    apiGet<Summary>("/api/orders/summary").then(setSummary).catch(() => setSummary(null));
  }, []);

  const load = useCallback(() => {
    const p = new URLSearchParams();
    if (statusFilter !== "All Orders") p.set("status", statusFilter);
    if (search) p.set("q", search);
    const qs = p.toString();
    apiGet<Order[]>(`/api/orders${qs ? `?${qs}` : ""}`)
      .then(setRows)
      .catch(() => setRows([]));
  }, [statusFilter, search]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    refreshSummary();
  }, [refreshSummary]);

  useEffect(() => {
    if (!createOpen) return;
    apiGet<Supplier[]>("/api/suppliers").then(setSuppliers).catch(() => setSuppliers([]));
    fetchAllArticles().then(setArticles).catch(() => setArticles([]));
  }, [createOpen]);

  useEffect(() => {
    if (deepLinkHandled.current) return;
    if (searchParams.get("create") !== "1") return;
    const aid = searchParams.get("articleId");
    if (!aid) return;
    deepLinkHandled.current = true;
    router.replace("/orders");
    if (!canCreate) return;
    setLines([{ articleId: aid, quantity: "1", unitPrice: "0" }]);
    setCreateOpen(true);
  }, [searchParams, canCreate, router]);

  async function shipOrder(o: Order) {
    const ok = await confirm({
      title: t.confirm.shipOrder.title,
      message: t.confirm.shipOrder.message(o.id),
      confirmLabel: t.confirm.shipOrder.confirm,
      variant: "warning",
    });
    if (!ok) return;
    setErr(null);
    setMsg(null);
    try {
      await apiSend(`/api/orders/${o.orderId}/ship`, "POST");
      load();
      refreshSummary();
    } catch (e) {
      setErr((e as Error).message);
    }
  }

  async function receiveOrder(o: Order) {
    const ok = await confirm({
      title: t.confirm.receiveOrder.title,
      message: t.confirm.receiveOrder.message(o.id, t.orders.receiveHint),
      confirmLabel: t.confirm.receiveOrder.confirm,
      variant: "success",
    });
    if (!ok) return;
    setErr(null);
    setMsg(null);
    try {
      await apiSend(`/api/orders/${o.orderId}/receive`, "POST", {});
      setMsg(t.orders.receiveSuccess);
      load();
      refreshSummary();
    } catch (e) {
      setErr((e as Error).message);
    }
  }

  async function submitCreate() {
    setCreateErr(null);
    setCreateSaving(true);
    try {
      const parsed = lines
        .filter((l) => l.articleId)
        .map((l) => {
          const article = orderArticles.find((a) => String(a.id) === l.articleId);
          const quantity = Math.floor(Number(l.quantity.replace(",", ".")));
          if (!Number.isFinite(quantity) || quantity < 1) {
            throw new Error(t.orders.invalidQty);
          }
          if (article) {
            const max = maxOrderableQuantity(article);
            assertWithinMaxOrderable(
              article,
              quantity,
              t.orders.qtyExceedsMax(article.sku, max, article.maxThreshold ?? 0)
            );
          }
          return {
            articleId: Number(l.articleId),
            quantity,
            unitPrice: Number(l.unitPrice.replace(",", ".") || 0),
          };
        });
      if (!supplierId || parsed.length === 0) {
        throw new Error(t.orders.formInvalid);
      }
      await apiSend("/api/orders", "POST", {
        supplierId: Number(supplierId),
        orderDate: new Date().toISOString().slice(0, 10),
        lines: parsed,
      });
      setCreateOpen(false);
      setSupplierId("");
      setLines([{ articleId: "", quantity: "1", unitPrice: "0" }]);
      load();
      refreshSummary();
    } catch (e) {
      setCreateErr((e as Error).message);
    } finally {
      setCreateSaving(false);
    }
  }

  async function cancelOrder(o: Order) {
    const ok = await confirm({
      title: t.confirm.cancelOrder.title,
      message: t.confirm.cancelOrder.message(o.id),
      confirmLabel: t.confirm.cancelOrder.confirm,
      variant: "danger",
    });
    if (!ok) return;
    try {
      await apiSend(`/api/orders/${o.orderId}/cancel`, "POST");
      load();
      refreshSummary();
    } catch {
      /* ignore */
    }
  }

  const kpis = summary
    ? [
        { label: t.orders.kpiTotal, value: String(summary.totalOrders), lucide: "Package", color: T.primary },
        { label: t.orders.kpiPending, value: String(summary.pending), lucide: "Clock", color: T.warning },
        { label: t.orders.kpiShipped, value: String(summary.shipped), lucide: "Truck", color: T.accent },
        { label: t.orders.kpiReceived, value: String(summary.received), lucide: "CircleCheck", color: T.success },
      ]
    : [];

  return (
    <>
      {dialog}
      <Topbar title={t.orders.title} subtitle={t.orders.subtitle} />
      <Page>
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

        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-4">
          <div>
            <h2 className="text-xl font-bold" style={{ color: T.text }}>
              {t.orders.title}
            </h2>
            <p className="text-sm mt-1" style={{ color: T.text2 }}>
              {t.orders.subtitle}
            </p>
          </div>
          {canCreate && (
            <Btn type="button" onClick={() => setCreateOpen(true)}>
              {Icons.plus} {t.orders.newOrderLabel}
            </Btn>
          )}
        </div>

        {kpis.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-4">
            {kpis.map((k) => (
              <Card key={k.label} className="px-5 py-4 flex items-center gap-4 overflow-hidden relative">
                <div className="absolute bottom-0 left-0 right-0 h-0.5" style={{ background: k.color }} />
                <LucideByName name={k.lucide} size={28} strokeWidth={2} style={{ color: k.color }} />
                <div>
                  <p className="text-xl font-bold" style={{ color: T.text }}>
                    {k.value}
                  </p>
                  <p className="text-xs" style={{ color: T.text2 }}>
                    {k.label}
                  </p>
                </div>
              </Card>
            ))}
          </div>
        )}

        <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3 mb-4">
          <Card className="px-3 py-2 flex flex-wrap items-center gap-1.5 flex-1">
            {pills.map((p) => (
              <button
                key={p}
                type="button"
                onClick={() => setStatusFilter(p)}
                className="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all whitespace-nowrap"
                style={
                  statusFilter === p
                    ? { background: T.primary, color: "#fff" }
                    : { background: T.bg, color: T.text2 }
                }
              >
                {p === "All Orders" ? "Toutes" : statusLabel(p)}
              </button>
            ))}
          </Card>
          <Input
            placeholder={t.orders.searchPh}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            icon={Icons.search}
            className="w-full lg:w-64"
          />
        </div>

        <Card className="overflow-hidden overflow-x-auto">
          <table className="w-full border-collapse min-w-[960px]">
            <thead>
              <tr>
                <TH>{t.orders.colOrder}</TH>
                <TH>{t.orders.colSupplier}</TH>
                <TH>{t.orders.colDate}</TH>
                <TH>{t.orders.colItems}</TH>
                <TH>{t.orders.colTotal}</TH>
                <TH>{t.orders.colStatus}</TH>
                <TH>{t.orders.colActions}</TH>
              </tr>
            </thead>
            <tbody>
              {rows.map((o) => (
                <tr key={o.orderId} className="border-t" style={{ borderColor: T.border }}>
                  <TD>
                    <span className="font-mono font-semibold text-xs" style={{ color: T.primary }}>
                      {o.id}
                    </span>
                  </TD>
                  <TD>{o.supplier}</TD>
                  <TD style={{ color: T.text2 }}>{o.date}</TD>
                  <TD>{o.items}</TD>
                  <TD>
                    <span className="font-semibold">{o.total}</span>
                  </TD>
                  <TD>
                    <div className="flex flex-col gap-1">
                      <Badge status={o.status} />
                      {o.tenantReceived && (
                        <span className="text-[10px]" style={{ color: T.primary }}>
                          ✓ {t.orders.tenantReceived}
                        </span>
                      )}
                    </div>
                  </TD>
                  <TD>
                    <div className="flex flex-wrap gap-1">
                      {canCreate && o.status === "Pending" && (
                        <Btn size="sm" type="button" onClick={() => void shipOrder(o)}>
                          {t.orders.shipLabel}
                        </Btn>
                      )}
                      {canReceive && o.status === "Shipped" && (
                        <Btn size="sm" type="button" onClick={() => void receiveOrder(o)}>
                          {t.orders.receiveLabel}
                        </Btn>
                      )}
                      {canCancel && o.status !== "Received" && o.status !== "Cancelled" && (
                        <Btn size="sm" type="button" onClick={() => cancelOrder(o)}>
                          Annuler
                        </Btn>
                      )}
                    </div>
                  </TD>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-sm" style={{ color: T.text2 }}>
                    {t.orders.noMatch}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Card>
      </Page>

      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title={t.orders.createTitle}
        subtitle={t.orders.createContextHint}
        width="640px"
        footer={
          <>
            <Btn type="button" variant="ghost" onClick={() => setCreateOpen(false)}>
              {t.common.cancel}
            </Btn>
            <Btn type="button" onClick={() => void submitCreate()} disabled={createSaving}>
              {createSaving ? t.common.saving : t.common.save}
            </Btn>
          </>
        }
      >
        <div className="px-6 py-5">
          {createErr && (
            <AlertBanner variant="error" className="mb-4">
              {createErr}
            </AlertBanner>
          )}
            <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
              {t.orders.fieldSupplier}
            </label>
            <select
              className="w-full rounded-lg border text-sm px-3 py-2 mb-4"
              style={{ borderColor: T.border }}
              value={supplierId}
              onChange={(e) => setSupplierId(e.target.value)}
            >
              <option value="">—</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
            {lines.map((line, idx) => {
              const picked = orderArticles.find((a) => String(a.id) === line.articleId);
              return (
                <div key={idx} className="grid grid-cols-2 gap-2 mb-2 items-end">
                  <div className="col-span-2">
                    <label className="text-[10px] font-medium block mb-0.5" style={{ color: T.text2 }}>
                      {t.orders.fieldArticle}
                    </label>
                    <select
                      className="w-full rounded-lg border text-xs px-2 py-1.5"
                      style={{ borderColor: T.border }}
                      value={line.articleId}
                      onChange={(e) => {
                        const next = [...lines];
                        next[idx] = { ...line, articleId: e.target.value };
                        setLines(next);
                      }}
                    >
                      <option value="">{t.orders.fieldArticle}</option>
                      {orderArticles.map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.sku} — {a.name}
                          {a.newProduct ? " (nouveau)" : ""}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="text-[10px] font-medium block mb-0.5" style={{ color: T.text2 }}>
                      {t.orders.fieldQty}
                    </label>
                    <Input
                      placeholder="1"
                      value={line.quantity}
                      onChange={(e) => {
                        const next = [...lines];
                        next[idx] = { ...line, quantity: e.target.value };
                        setLines(next);
                      }}
                      type="number"
                      min={1}
                    />
                    {picked && picked.maxThreshold != null && (
                      <p className="text-[10px] mt-0.5" style={{ color: T.text2 }}>
                        {t.orders.maxOrderableHint(
                          picked.qty ?? 0,
                          maxOrderableQuantity(picked),
                          picked.maxThreshold
                        )}
                      </p>
                    )}
                    {picked?.suggestedPurchaseOrderQty != null && (
                      <p className="text-[10px] mt-0.5" style={{ color: T.text2 }}>
                        {t.orders.suggestedQtyHint(picked.suggestedPurchaseOrderQty)}
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="text-[10px] font-medium block mb-0.5" style={{ color: T.text2 }}>
                      {t.orders.fieldUnitPrice}
                    </label>
                    <Input
                      placeholder={t.orders.fieldUnitPrice}
                      value={line.unitPrice}
                      onChange={(e) => {
                        const next = [...lines];
                        next[idx] = { ...line, unitPrice: e.target.value };
                        setLines(next);
                      }}
                      type="text"
                    />
                  </div>
                </div>
              );
            })}
            <Btn
              size="sm"
              type="button"
              onClick={() => setLines((l) => [...l, { articleId: "", quantity: "1", unitPrice: "0" }])}
            >
              {t.orders.addLine}
            </Btn>
        </div>
      </Modal>
    </>
  );
}
