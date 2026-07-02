"use client";

import { useCallback, useEffect, useState } from "react";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { TH, TD } from "@/components/inventra/ui/Table";
import { AlertBanner } from "@/components/inventra/ui/AlertBanner";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiSend } from "@/lib/api";
import { loadSession } from "@/lib/auth";
import { canRecordStockMovements } from "@/lib/rolePrivileges";
import { fetchAllArticles } from "@/lib/types/article";
import { fetchMovementsPage, type MovementDto } from "@/lib/types/movement";

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" });
  } catch {
    return iso;
  }
}

export default function MovementsPage() {
  const { t } = useT();
  const role = loadSession()?.role ?? "";
  const canWrite = canRecordStockMovements(role);
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState<MovementDto[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [articles, setArticles] = useState<{ id: number; label: string }[]>([]);
  const [articleId, setArticleId] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setErr(null);
    fetchMovementsPage(page, 20)
      .then((p) => {
        setRows(p.content);
        setTotalPages(Math.max(1, p.totalPages));
      })
      .catch(() => {
        setRows([]);
        setErr(t.movements.loadError);
      });
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    fetchAllArticles()
      .then((list) =>
        setArticles(list.map((a) => ({ id: a.id, label: `${a.sku} — ${a.name} (${a.qty})` })))
      )
      .catch(() => setArticles([]));
  }, []);

  const submitOut = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canWrite) return;
    setBusy(true);
    setMsg(null);
    setErr(null);
    try {
      await apiSend("/api/movements", "POST", {
        articleId: Number(articleId),
        type: "OUT",
        quantity: Number(quantity),
        note: note.trim() || null,
      });
      setMsg(t.movements.success);
      setNote("");
      setQuantity("1");
      load();
    } catch (ex) {
      setErr(ex instanceof Error ? ex.message : t.movements.loadError);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Topbar title={t.movements.title} subtitle={t.movements.subtitle} />
      <Page>
        {err && (
          <AlertBanner variant="error" className="mb-4">
            {err}
          </AlertBanner>
        )}
        {msg && (
          <AlertBanner variant="success" className="mb-4" onDismiss={() => setMsg(null)}>
            {msg}
          </AlertBanner>
        )}

        {canWrite && (
          <Card className="p-5 mb-6">
            <h3 className="text-sm font-semibold mb-1" style={{ color: T.text }}>
              {t.movements.newOut}
            </h3>
            <p className="text-xs mb-4" style={{ color: T.text2 }}>
              {t.movements.inBlocked}
            </p>
            <form onSubmit={(e) => void submitOut(e)} className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="text-sm flex flex-col gap-1">
                <span style={{ color: T.text2 }}>{t.movements.article}</span>
                <select
                  className="border rounded-lg px-3 py-2 text-sm"
                  style={{ borderColor: T.border, color: T.text }}
                  value={articleId}
                  onChange={(e) => setArticleId(e.target.value)}
                  required
                >
                  <option value="">—</option>
                  {articles.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.label}
                    </option>
                  ))}
                </select>
              </label>
              <div>
                <span className="text-sm mb-1 block" style={{ color: T.text2 }}>
                  {t.movements.quantity}
                </span>
                <Input
                  type="number"
                  min={1}
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  required
                />
              </div>
              <div className="md:col-span-2">
                <span className="text-sm mb-1 block" style={{ color: T.text2 }}>
                  {t.movements.note}
                </span>
                <Input value={note} onChange={(e) => setNote(e.target.value)} />
              </div>
              <div className="md:col-span-2">
                <Btn type="submit" disabled={busy || !articleId}>
                  {busy ? t.movements.submitting : t.movements.submit}
                </Btn>
              </div>
            </form>
          </Card>
        )}

        <Card className="overflow-hidden">
          <div className="px-5 py-4 border-b" style={{ borderColor: T.border }}>
            <h3 className="text-sm font-semibold" style={{ color: T.text }}>
              {t.movements.listTitle}
            </h3>
          </div>
          {rows.length === 0 ? (
            <p className="p-5 text-sm" style={{ color: T.text2 }}>
              {t.movements.empty}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr>
                    <TH>{t.movements.colDate}</TH>
                    <TH>{t.movements.colArticle}</TH>
                    <TH>{t.movements.colType}</TH>
                    <TH>{t.movements.colQty}</TH>
                    <TH>{t.movements.colUser}</TH>
                    <TH>{t.movements.colNote}</TH>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((m) => (
                    <tr key={m.id} className="border-t" style={{ borderColor: T.border }}>
                      <TD>{formatDate(m.createdAt)}</TD>
                      <TD>
                        <span className="font-medium">{m.sku}</span>
                        <span className="block text-xs" style={{ color: T.text2 }}>
                          {m.articleName}
                        </span>
                      </TD>
                      <TD>{m.type === "OUT" ? t.movements.typeOut : m.type}</TD>
                      <TD>{m.quantity}</TD>
                      <TD>{m.createdBy}</TD>
                      <TD>{m.note ?? "—"}</TD>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {totalPages > 1 && (
            <div className="flex items-center justify-end gap-2 px-5 py-3 border-t" style={{ borderColor: T.border }}>
              <Btn type="button" size="sm" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
                ←
              </Btn>
              <span className="text-xs" style={{ color: T.text2 }}>
                {page + 1} / {totalPages}
              </span>
              <Btn
                type="button"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                →
              </Btn>
            </div>
          )}
        </Card>
      </Page>
    </>
  );
}
