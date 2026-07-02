"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { TH, TD } from "@/components/inventra/ui/Table";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet, apiGetBlob, openPdfBlob } from "@/lib/api";
import { loadSession } from "@/lib/auth";
import { canFilterPosSalesBySeller } from "@/lib/rolePrivileges";
import { formatCurrency } from "@/lib/currency";

type SaleRow = {
  id: number;
  saleNumber: string;
  soldByEmail: string;
  createdAt: string;
  total: number;
  lineCount: number;
};

type SpringPage = {
  content: SaleRow[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

type SellerOption = { email: string };

function formatApiDate(iso: string) {
  try {
    const d = new Date(iso);
    return d.toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" });
  } catch {
    return iso;
  }
}

export default function PosHistoryPage() {
  const { t } = useT();
  const role = loadSession()?.role ?? "";
  const canFilter = canFilterPosSalesBySeller(role);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<SpringPage | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [soldBy, setSoldBy] = useState("");
  const [soldByDebounced, setSoldByDebounced] = useState("");
  const [commercials, setCommercials] = useState<SellerOption[]>([]);
  const [pdfBusyId, setPdfBusyId] = useState<number | null>(null);

  useEffect(() => {
    const t = setTimeout(() => setSoldByDebounced(soldBy.trim()), 400);
    return () => clearTimeout(t);
  }, [soldBy]);

  const load = useCallback(() => {
    const qs = new URLSearchParams();
    qs.set("page", String(page));
    qs.set("size", "15");
    if (canFilter && soldByDebounced) qs.set("soldBy", soldByDebounced);
    setErr(null);
    apiGet<SpringPage>(`/api/pos/sales?${qs.toString()}`)
      .then(setData)
      .catch(() => {
        setData(null);
        setErr(t.posHistory.loadError);
      });
  }, [page, soldByDebounced, canFilter]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!canFilter) return;
    apiGet<SellerOption[]>("/api/pos/sales/commercials")
      .then(setCommercials)
      .catch(() => setCommercials([]));
  }, [canFilter]);

  const [detailId, setDetailId] = useState<number | null>(null);
  const [detail, setDetail] = useState<{
    saleNumber: string;
    soldByEmail: string;
    createdAt: string;
    total: number;
    note: string | null;
    lines: { sku: string; name: string; quantity: number; unitPrice: number; lineTotal: number }[];
  } | null>(null);
  const [detailBusy, setDetailBusy] = useState(false);

  useEffect(() => {
    if (detailId == null) {
      setDetail(null);
      return;
    }
    setDetailBusy(true);
    apiGet<{
      saleNumber: string;
      soldByEmail: string;
      createdAt: string;
      total: number;
      note: string | null;
      lines: { sku: string; name: string; quantity: number; unitPrice: number; lineTotal: number }[];
    }>(`/api/pos/sales/${detailId}`)
      .then(setDetail)
      .catch(() => setDetail(null))
      .finally(() => setDetailBusy(false));
  }, [detailId]);

  async function openInvoice(id: number) {
    setPdfBusyId(id);
    try {
      const blob = await apiGetBlob(`/api/pos/sales/${id}/invoice.pdf`);
      openPdfBlob(blob);
    } catch {
      setErr(t.posHistory.loadError);
    } finally {
      setPdfBusyId(null);
    }
  }

  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <>
      <Topbar title={t.posHistory.title} subtitle={t.posHistory.subtitle} />
      <Page>
        <div className="mb-4 flex flex-wrap items-center gap-4">
          <Link href="/pos" className="text-sm font-medium underline-offset-2 hover:underline" style={{ color: T.primary }}>
            ← Point de vente
          </Link>
          {canFilter && (
            <div className="flex flex-col sm:flex-row gap-2 sm:items-center flex-1 min-w-[200px]">
              <label className="text-xs font-medium whitespace-nowrap" style={{ color: T.text2 }}>
                {t.posHistory.filterSeller}
              </label>
              <Input
                placeholder={t.posHistory.filterPlaceholder}
                value={soldBy}
                onChange={(e) => {
                  setSoldBy(e.target.value);
                  setPage(0);
                }}
                className="max-w-md"
                list="pos-commercial-emails"
              />
              <datalist id="pos-commercial-emails">
                {commercials.map((c) => (
                  <option key={c.email} value={c.email} />
                ))}
              </datalist>
            </div>
          )}
        </div>

        <Card className="p-0 overflow-x-auto">
          {err && (
            <p className="p-4 text-sm" style={{ color: T.danger }}>
              {err}
            </p>
          )}
          <table className="w-full border-collapse min-w-[720px]">
            <thead>
              <tr>
                <TH>{t.posHistory.colNumber}</TH>
                <TH>{t.posHistory.colSeller}</TH>
                <TH>{t.posHistory.colDate}</TH>
                <TH>{t.posHistory.colLines}</TH>
                <TH>{t.posHistory.colTotal}</TH>
                <TH>{t.posHistory.detail}</TH>
                <TH>{t.posHistory.pdf}</TH>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-t" style={{ borderColor: T.border }}>
                  <TD>
                    <span className="font-mono text-sm font-semibold">{r.saleNumber}</span>
                  </TD>
                  <TD>
                    <span className="text-sm">{r.soldByEmail}</span>
                  </TD>
                  <TD>
                    <span className="text-sm" style={{ color: T.text2 }}>
                      {formatApiDate(r.createdAt)}
                    </span>
                  </TD>
                  <TD>{r.lineCount}</TD>
                  <TD className="font-semibold">{formatCurrency(typeof r.total === "number" ? r.total : Number(r.total))}</TD>
                  <TD>
                    <Btn className="text-xs py-1.5 px-2" onClick={() => setDetailId(r.id)}>
                      {t.posHistory.detail}
                    </Btn>
                  </TD>
                  <TD>
                    <Btn
                      className="text-xs py-1.5 px-2"
                      onClick={() => openInvoice(r.id)}
                      disabled={pdfBusyId === r.id}
                    >
                      {pdfBusyId === r.id ? "…" : "PDF"}
                    </Btn>
                  </TD>
                </tr>
              ))}
              {rows.length === 0 && !err && (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-sm" style={{ color: T.text2 }}>
                    {t.posHistory.empty}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t" style={{ borderColor: T.border }}>
              <Btn
                className="text-xs"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Précédent
              </Btn>
              <span className="text-xs" style={{ color: T.text2 }}>
                Page {page + 1} / {totalPages}
              </span>
              <Btn
                className="text-xs"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Suivant
              </Btn>
            </div>
          )}
        </Card>

        {detailId != null && (
          <Card className="mt-4 p-5">
            <div className="flex items-start justify-between gap-4 mb-4">
              <h3 className="text-sm font-semibold" style={{ color: T.text }}>
                {t.posHistory.detailTitle}
              </h3>
              <Btn type="button" size="sm" onClick={() => setDetailId(null)}>
                {t.common.cancel}
              </Btn>
            </div>
            {detailBusy && <p className="text-sm" style={{ color: T.text2 }}>{t.common.loading}</p>}
            {detail && !detailBusy && (
              <div className="text-sm space-y-3">
                <p>
                  <strong>{detail.saleNumber}</strong> — {formatApiDate(detail.createdAt)} — {detail.soldByEmail}
                </p>
                <ul className="space-y-1">
                  {detail.lines.map((l, i) => (
                    <li key={i} style={{ color: T.text2 }}>
                      {l.sku} {l.name} × {l.quantity} = {formatCurrency(l.lineTotal)}
                    </li>
                  ))}
                </ul>
                <p className="font-semibold" style={{ color: T.text }}>
                  Total : {formatCurrency(typeof detail.total === "number" ? detail.total : Number(detail.total))}
                </p>
              </div>
            )}
          </Card>
        )}
      </Page>
    </>
  );
}
