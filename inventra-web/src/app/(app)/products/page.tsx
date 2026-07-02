"use client";

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
import { apiSend } from "@/lib/api";
import { fetchAllArticles } from "@/lib/types/article";
import { loadSession } from "@/lib/auth";
import { useRouter } from "next/navigation";
import { canEditArticleCatalog, canCreatePurchaseOrder, canCreateReplenishment } from "@/lib/rolePrivileges";
import { canProcureArticle, procurementCreatePath } from "@/lib/procurementRouting";
import { useT } from "@/lib/i18n";
import { Modal } from "@/components/inventra/ui/Modal";
import { AlertBanner } from "@/components/inventra/ui/AlertBanner";
import { useConfirm } from "@/components/inventra/ui/useConfirm";
import { formatCurrency } from "@/lib/currency";

const PAGE_SIZE = 8;

export type Article = {
  id: number;
  name: string;
  sku: string;
  category: string;
  qty: number;
  price: number;
  status: string;
  minThreshold?: number;
  maxThreshold?: number;
  description?: string | null;
  newProduct?: boolean;
  purchaseOrderEligible?: boolean;
  replenishmentEligible?: boolean;
  suggestedPurchaseOrderQty?: number;
  suggestedReplenishmentQty?: number;
};

function ProductModal({
  article,
  onClose,
  onSaved,
}: {
  article: Article | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t } = useT();
  const isEdit = article != null;
  const empty = { name: "", sku: "", category: "", price: "", qty: "", reorder: "", max: "", desc: "" };
  const [form, setForm] = useState(empty);
  const set =
    (k: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm((f) => ({ ...f, [k]: e.target.value }));
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (article) {
      setForm({
        name: article.name,
        sku: article.sku,
        category: article.category ?? "",
        price: String(article.price),
        qty: String(article.qty),
        reorder: String(article.minThreshold ?? 0),
        max: String(article.maxThreshold ?? 9999),
        desc: article.description ?? "",
      });
    } else {
      setForm(empty);
    }
  }, [article]);

  const submit = async () => {
    setErr(null);
    setSaving(true);
    try {
      if (isEdit && article) {
        await apiSend(`/api/articles/${article.id}`, "PUT", {
          name: form.name.trim(),
          category: form.category.trim(),
          salePrice: Number(form.price.replace(",", ".") || 0),
          purchasePrice: null,
          minThreshold: Number(form.reorder || 0),
          maxThreshold: Number(form.max || 9999),
          description: form.desc.trim() || null,
        });
      } else {
        await apiSend("/api/articles", "POST", {
          name: form.name,
          sku: form.sku,
          category: form.category,
          salePrice: Number(form.price.replace(",", ".") || 0),
          minThreshold: Number(form.reorder || 0),
          maxThreshold: Number(form.max || 9999),
          description: form.desc || undefined,
        });
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
      title={isEdit ? t.products.modalEditTitle : t.products.modalAddTitle}
      footer={
        <>
          <Btn variant="ghost" onClick={onClose}>
            {t.common.cancel}
          </Btn>
          <Btn onClick={() => void submit()} disabled={saving}>
            {saving ? t.common.saving : t.common.save}
          </Btn>
        </>
      }
    >
      <div className="px-6 py-5 flex flex-col gap-4">
        {err && (
          <AlertBanner variant="error">{err}</AlertBanner>
        )}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldName}
              </label>
              <Input placeholder={t.products.phName} value={form.name} onChange={set("name")} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldSku}
              </label>
              <Input
                placeholder={t.products.phSku}
                value={form.sku}
                onChange={set("sku")}
                readOnly={isEdit}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldCategory}
              </label>
              <Input placeholder={t.products.phCategory} value={form.category} onChange={set("category")} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldPrice}
              </label>
              <Input placeholder={t.products.phPrice} value={form.price} onChange={set("price")} type="text" />
            </div>
          </div>
          {isEdit ? (
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldQty}
              </label>
              <Input value={form.qty} readOnly type="number" onChange={() => {}} />
              <p className="text-[11px]" style={{ color: T.text2 }}>
                {t.products.stockQtyHint}
              </p>
            </div>
          ) : (
            <p className="text-xs rounded-lg px-3 py-2" style={{ background: "#EFF6FF", color: T.text2 }}>
              {t.products.stockQtyHint}
            </p>
          )}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldReorder}
              </label>
              <Input placeholder="10" value={form.reorder} onChange={set("reorder")} type="number" />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium" style={{ color: T.text2 }}>
                {t.products.fieldMax}
              </label>
              <Input placeholder="500" value={form.max} onChange={set("max")} type="number" />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium" style={{ color: T.text2 }}>
              {t.products.fieldDesc}
            </label>
            <textarea
              placeholder={t.products.phDesc}
              value={form.desc}
              onChange={set("desc")}
              rows={3}
              className="w-full px-3 py-2 text-sm rounded-lg border outline-none focus:ring-2 focus:ring-blue-100 resize-none transition-all"
              style={{ background: T.bg, borderColor: T.border, color: T.text }}
            />
          </div>
      </div>
    </Modal>
  );
}

export default function ProductsPage() {
  const { t } = useT();
  const router = useRouter();
  const role = loadSession()?.role;
  const [filter, setFilter] = useState("All");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Article | null>(null);
  const [rows, setRows] = useState<Article[]>([]);
  const canEdit = canEditArticleCatalog(role);
  const canProcureAny = canCreatePurchaseOrder(role) || canCreateReplenishment(role);
  const showActionsCol = canEdit || canProcureAny;
  const { confirm, dialog } = useConfirm();

  const filterDefs = useMemo(
    () => [
      { api: "All", label: t.filters.all },
      { api: "In Stock", label: t.filters.inStock },
      { api: "Low Stock", label: t.filters.lowStock },
      { api: "Out of Stock", label: t.filters.outOfStock },
    ],
    [t],
  );

  const load = useCallback(() => {
    fetchAllArticles({
      q: search || undefined,
      status: filter !== "All" ? filter : undefined,
    })
      .then((list) =>
        setRows(
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
            description: r.description,
            newProduct: r.newProduct,
            purchaseOrderEligible: r.purchaseOrderEligible,
            replenishmentEligible: r.replenishmentEligible,
            suggestedPurchaseOrderQty: r.suggestedPurchaseOrderQty,
            suggestedReplenishmentQty: r.suggestedReplenishmentQty,
          }))
        )
      )
      .catch(() => setRows([]));
  }, [search, filter]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(1);
  }, [search, filter]);

  const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const visible = useMemo(() => {
    const p = Math.min(page, totalPages);
    const start = (p - 1) * PAGE_SIZE;
    return rows.slice(start, start + PAGE_SIZE);
  }, [rows, page, totalPages]);

  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  const remove = async (id: number) => {
    const ok = await confirm({
      title: t.confirm.archiveProduct.title,
      message: t.confirm.archiveProduct.message,
      confirmLabel: t.confirm.archiveProduct.confirm,
      variant: "danger",
    });
    if (!ok) return;
    try {
      await apiSend(`/api/articles/${id}`, "DELETE");
      load();
    } catch {
      /* ignore */
    }
  };

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const openEdit = (p: Article) => {
    setEditing(p);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
  };

  return (
    <>
      {dialog}
      {modalOpen && canEdit && (
        <ProductModal
          key={editing?.id ?? "new"}
          article={editing}
          onClose={closeModal}
          onSaved={() => {
            load();
          }}
        />
      )}
      <Topbar title={t.products.title} subtitle={t.products.subtitle} />
      <Page>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-xl font-bold" style={{ color: T.text }}>
              {t.products.catalogTitle}
            </h2>
            <p className="text-sm mt-1" style={{ color: T.text2 }}>
              {t.products.total(rows.length)}
            </p>
            {!canEdit && (
              <p className="text-xs mt-2 rounded-lg px-3 py-2 inline-block" style={{ background: "#EFF6FF", color: T.text2 }}>
                {t.products.readOnlyHint}
              </p>
            )}
          </div>
          {canEdit && (
            <Btn onClick={openCreate}>
              {Icons.plus} {t.products.add}
            </Btn>
          )}
        </div>

        <Card className="px-4 py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            {filterDefs.map((f) => (
              <button
                key={f.api}
                type="button"
                onClick={() => setFilter(f.api)}
                className="px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all"
                style={
                  filter === f.api
                    ? { background: T.primary, color: "#fff" }
                    : { background: T.bg, color: T.text2 }
                }
              >
                {f.label}
              </button>
            ))}
          </div>
          <Input
            placeholder={t.products.searchPlaceholder}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            icon={Icons.search}
            className="w-full sm:w-64"
          />
        </Card>

        <Card className="overflow-hidden overflow-x-auto">
          <table className="w-full border-collapse min-w-[720px]">
            <thead>
              <tr>
                <TH>{t.products.tableName}</TH>
                <TH>{t.products.tableSku}</TH>
                <TH>{t.products.tableCategory}</TH>
                <TH>{t.products.tableQty}</TH>
                <TH>{t.products.tablePrice}</TH>
                <TH>{t.products.tableStatus}</TH>
                {showActionsCol && <TH className="text-center">{t.products.tableActions}</TH>}
              </tr>
            </thead>
            <tbody>
              {visible.map((p) => (
                <tr key={p.id} className="border-t hover:bg-gray-50 transition-colors" style={{ borderColor: T.border }}>
                  <TD>
                    <span className="font-medium">{p.name}</span>
                  </TD>
                  <TD>
                    <span className="font-mono text-xs px-2 py-0.5 rounded-md" style={{ background: T.bg, color: T.text2 }}>
                      {p.sku}
                    </span>
                  </TD>
                  <TD>{p.category}</TD>
                  <TD>
                    <span
                      className="font-semibold"
                      style={{
                        color: p.qty === 0 ? T.danger : p.qty < 10 ? T.warning : T.text,
                      }}
                    >
                      {p.qty}
                    </span>
                  </TD>
                  <TD className="font-medium">{formatCurrency(p.price)}</TD>
                  <TD>
                    <Badge status={p.status} />
                  </TD>
                  {showActionsCol && (
                    <TD>
                      <div className="flex items-center justify-center gap-1.5 flex-wrap">
                        {canProcureArticle(role, p) && (
                          <Btn
                            size="sm"
                            type="button"
                            onClick={() => {
                              const path = procurementCreatePath(p);
                              if (path) router.push(path);
                            }}
                          >
                            {t.products.procureBtn}
                          </Btn>
                        )}
                        {canEdit && (
                          <>
                            <button
                              type="button"
                              onClick={() => openEdit(p)}
                              className="w-7 h-7 rounded-lg flex items-center justify-center transition-colors hover:text-white"
                              style={{ color: T.primary, background: "rgba(37,99,235,0.08)" }}
                              title={t.products.edit}
                            >
                              {Icons.edit}
                            </button>
                            <button
                              type="button"
                              onClick={() => remove(p.id)}
                              className="w-7 h-7 rounded-lg flex items-center justify-center transition-colors hover:text-white"
                              style={{ color: T.danger, background: "rgba(239,68,68,0.08)" }}
                              title={t.products.archive}
                            >
                              {Icons.trash}
                            </button>
                          </>
                        )}
                      </div>
                    </TD>
                  )}
                </tr>
              ))}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={showActionsCol ? 7 : 6} className="py-12 text-center text-sm" style={{ color: T.text2 }}>
                    {t.products.empty}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          <div
            className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between px-4 py-3 border-t"
            style={{ borderColor: T.border }}
          >
            <span className="text-xs" style={{ color: T.text2 }}>
              {t.products.rowsRange(
                rows.length === 0 ? 0 : (Math.min(page, totalPages) - 1) * PAGE_SIZE + 1,
                rows.length === 0 ? 0 : (Math.min(page, totalPages) - 1) * PAGE_SIZE + visible.length,
                rows.length
              )}{" "}
              — {t.products.pageOf(Math.min(page, totalPages), totalPages)}
            </span>
            <div className="flex items-center gap-2">
              <Btn
                variant="ghost"
                size="sm"
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page <= 1}
              >
                {t.products.prev}
              </Btn>
              <Btn
                variant="ghost"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page >= totalPages}
              >
                {t.products.next}
              </Btn>
            </div>
          </div>
        </Card>
      </Page>
    </>
  );
}
