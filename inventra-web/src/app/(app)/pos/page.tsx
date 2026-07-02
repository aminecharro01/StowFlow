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
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGetBlob, apiSend, openPdfBlob } from "@/lib/api";
import { fetchAllArticles } from "@/lib/types/article";
import { formatCurrency } from "@/lib/currency";

type Article = {
  id: number;
  name: string;
  sku: string;
  category: string;
  qty: number;
  price: number;
  status: string;
};

type CartLine = { article: Article; qty: number };

type PosSaleResponse = {
  id: number;
  saleNumber: string;
  total: number;
  createdAt: string;
  soldByEmail: string;
  lines: { articleId: number; sku: string; name: string; unitPrice: number; quantity: number; lineTotal: number }[];
  note: string | null;
};

function cartTotal(lines: CartLine[]): number {
  return lines.reduce((s, l) => s + l.article.price * l.qty, 0);
}

export default function PosPage() {
  const { t } = useT();
  const [search, setSearch] = useState("");
  const [rows, setRows] = useState<Article[]>([]);
  const [selected, setSelected] = useState<Article | null>(null);
  const [qty, setQty] = useState("1");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [note, setNote] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    fetchAllArticles({ q: search.trim() || undefined })
      .then((list) => {
        setRows(
          list.map((r) => ({
            id: r.id,
            name: r.name,
            sku: r.sku,
            category: r.category,
            qty: r.qty,
            price: typeof r.price === "number" ? r.price : Number(r.price),
            status: r.status,
          }))
        );
      })
      .catch(() => setRows([]));
  }, [search]);

  useEffect(() => {
    const t = setTimeout(() => load(), 280);
    return () => clearTimeout(t);
  }, [load]);

  const cartByArticleId = useMemo(() => new Map(cart.map((c) => [c.article.id, c])), [cart]);

  function addToCart() {
    setErr(null);
    setMsg(null);
    if (!selected) {
      setErr(t.pos.noSelection);
      return;
    }
    const q = Math.max(1, parseInt(qty, 10) || 1);
    if (q > selected.qty) {
      setErr(t.pos.noStock);
      return;
    }
    const existing = cartByArticleId.get(selected.id);
    if (existing) {
      const nextQty = existing.qty + q;
      if (nextQty > selected.qty) {
        setErr(t.pos.noStock);
        return;
      }
      setCart((prev) =>
        prev.map((l) => (l.article.id === selected.id ? { ...l, qty: nextQty } : l))
      );
    } else {
      setCart((prev) => [...prev, { article: { ...selected }, qty: q }]);
    }
    setMsg(null);
  }

  function setCartLineQty(articleId: number, raw: string) {
    const v = Math.max(1, parseInt(raw, 10) || 1);
    setCart((prev) =>
      prev.map((l) => {
        if (l.article.id !== articleId) return l;
        const max = l.article.qty;
        return { ...l, qty: Math.min(v, max) };
      })
    );
  }

  function removeLine(articleId: number) {
    setCart((prev) => prev.filter((l) => l.article.id !== articleId));
  }

  async function validateSale() {
    setErr(null);
    setMsg(null);
    if (cart.length === 0) {
      setErr(t.pos.cartEmpty);
      return;
    }
    setBusy(true);
    try {
      const body = {
        lines: cart.map((c) => ({ articleId: c.article.id, quantity: c.qty })),
        note: note.trim() || undefined,
      };
      const res = await apiSend<PosSaleResponse>("/api/pos/sale", "POST", body);
      if (!res || typeof res !== "object" || !("id" in res)) {
        throw new Error("Réponse serveur invalide.");
      }
      const pdf = await apiGetBlob(`/api/pos/sales/${res.id}/invoice.pdf`);
      openPdfBlob(pdf);
      setMsg(t.pos.success);
      setCart([]);
      setNote("");
      setQty("1");
      setSelected(null);
      load();
    } catch (ex) {
      setErr((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Topbar title={t.pos.title} subtitle={t.pos.subtitle} />
      <Page>
        <div className="mb-4 flex justify-end">
          <Link
            href="/pos/history"
            className="text-sm font-medium underline-offset-2 hover:underline"
            style={{ color: T.primary }}
          >
            {t.pos.historyLink}
          </Link>
        </div>
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
          <Card className="xl:col-span-2 overflow-hidden overflow-x-auto p-0">
            <div className="px-4 py-3 border-b flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between" style={{ borderColor: T.border }}>
              <Input
                placeholder={t.pos.searchPh}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full sm:max-w-xs"
              />
              <p className="text-xs" style={{ color: T.text2 }}>
                {t.pos.pickHint}
              </p>
            </div>
            <table className="w-full border-collapse min-w-[560px]">
              <thead>
                <tr>
                  <TH>{t.pos.colArticle}</TH>
                  <TH>{t.pos.colSku}</TH>
                  <TH>{t.pos.colPrice}</TH>
                  <TH>{t.pos.colStock}</TH>
                  <TH>{t.products.tableStatus}</TH>
                </tr>
              </thead>
              <tbody>
                {rows.map((a) => {
                  const active = selected?.id === a.id;
                  return (
                    <tr
                      key={a.id}
                      onClick={() => {
                        setSelected(a);
                        setErr(null);
                        setMsg(null);
                      }}
                      className="border-t cursor-pointer transition-colors"
                      style={{
                        borderColor: T.border,
                        background: active ? "rgba(37,99,235,0.08)" : undefined,
                      }}
                    >
                      <TD>
                        <span className="font-medium">{a.name}</span>
                      </TD>
                      <TD>
                        <span className="font-mono text-xs" style={{ color: T.text2 }}>
                          {a.sku}
                        </span>
                      </TD>
                      <TD className="font-semibold">{formatCurrency(a.price)}</TD>
                      <TD>
                        <span className="font-bold" style={{ color: a.qty === 0 ? T.danger : T.text }}>
                          {a.qty}
                        </span>
                      </TD>
                      <TD>
                        <Badge status={a.status} />
                      </TD>
                    </tr>
                  );
                })}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm" style={{ color: T.text2 }}>
                      {t.pos.empty}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </Card>

          <div className="space-y-6">
            <Card className="p-5 h-fit sticky top-24">
              <h3 className="text-sm font-bold mb-3" style={{ color: T.text }}>
                {t.pos.selectionTitle}
              </h3>
              {selected ? (
                <div className="space-y-4">
                  <div>
                    <p className="text-xs font-medium" style={{ color: T.text2 }}>
                      {t.pos.colArticle}
                    </p>
                    <p className="text-sm font-semibold" style={{ color: T.text }}>
                      {selected.name}
                    </p>
                    <p className="text-xs font-mono mt-1" style={{ color: T.text2 }}>
                      {selected.sku}
                    </p>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span style={{ color: T.text2 }}>{t.pos.colPrice}</span>
                    <span className="font-semibold">{formatCurrency(selected.price)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span style={{ color: T.text2 }}>{t.pos.colStock}</span>
                    <span className="font-bold">{selected.qty}</span>
                  </div>
                  <div>
                    <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
                      {t.pos.qtyLabel}
                    </label>
                    <Input
                      value={qty}
                      onChange={(e) => setQty(e.target.value)}
                      type="number"
                      min={1}
                      max={selected.qty > 0 ? selected.qty : undefined}
                    />
                  </div>
                  <Btn className="w-full justify-center" onClick={addToCart} disabled={selected.qty < 1}>
                    {t.pos.addToCart}
                  </Btn>
                </div>
              ) : (
                <p className="text-sm" style={{ color: T.text2 }}>
                  {t.pos.pickHint}
                </p>
              )}
            </Card>

            <Card className="p-5 h-fit sticky top-[28rem]">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-bold" style={{ color: T.text }}>
                  {t.pos.cartTitle}
                </h3>
                {cart.length > 0 && (
                  <button
                    type="button"
                    className="text-xs font-medium"
                    style={{ color: T.danger }}
                    onClick={() => setCart([])}
                  >
                    {t.pos.clearCart}
                  </button>
                )}
              </div>
              {cart.length === 0 ? (
                <p className="text-sm" style={{ color: T.text2 }}>
                  {t.pos.cartEmpty}
                </p>
              ) : (
                <div className="space-y-3">
                  <table className="w-full text-sm border-collapse">
                    <thead>
                      <tr>
                        <TH>{t.pos.colArticle}</TH>
                        <TH>{t.pos.colQty}</TH>
                        <TH>{t.pos.colLineTotal}</TH>
                        <TH> </TH>
                      </tr>
                    </thead>
                    <tbody>
                      {cart.map((l) => (
                        <tr key={l.article.id} className="border-t" style={{ borderColor: T.border }}>
                          <TD>
                            <span className="font-medium line-clamp-2">{l.article.name}</span>
                          </TD>
                          <TD>
                            <Input
                              value={String(l.qty)}
                              onChange={(e) => setCartLineQty(l.article.id, e.target.value)}
                              type="number"
                              min={1}
                              max={l.article.qty}
                              className="!py-1 max-w-[72px]"
                            />
                          </TD>
                          <TD className="font-semibold whitespace-nowrap">
                            {formatCurrency(l.article.price * l.qty)}
                          </TD>
                          <TD>
                            <button
                              type="button"
                              className="text-xs font-medium"
                              style={{ color: T.danger }}
                              onClick={() => removeLine(l.article.id)}
                            >
                              {t.pos.remove}
                            </button>
                          </TD>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <div className="flex justify-between items-center pt-2 border-t" style={{ borderColor: T.border }}>
                    <span className="text-sm" style={{ color: T.text2 }}>
                      {t.pos.cartTotal}
                    </span>
                    <span className="text-lg font-bold" style={{ color: T.text }}>
                      {formatCurrency(cartTotal(cart))}
                    </span>
                  </div>
                  <div>
                    <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
                      {t.pos.notePh}
                    </label>
                    <Input value={note} onChange={(e) => setNote(e.target.value)} />
                  </div>
                  {msg && (
                    <p className="text-sm rounded-lg px-3 py-2" style={{ background: "#ECFDF5", color: T.success }}>
                      {msg}
                    </p>
                  )}
                  {err && (
                    <p className="text-sm rounded-lg px-3 py-2" style={{ background: "#FEF2F2", color: T.danger }}>
                      {err}
                    </p>
                  )}
                  <Btn className="w-full justify-center" onClick={validateSale} disabled={busy}>
                    {busy ? t.pos.selling : t.pos.sell}
                  </Btn>
                </div>
              )}
            </Card>
          </div>
        </div>
      </Page>
    </>
  );
}
