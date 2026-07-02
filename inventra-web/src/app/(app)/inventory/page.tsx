"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Badge } from "@/components/inventra/ui/Badge";
import { TH, TD } from "@/components/inventra/ui/Table";
import { Icons } from "@/components/inventra/Icons";
import { T } from "@/lib/theme";
import { apiGet } from "@/lib/api";
import { loadSession } from "@/lib/auth";
import { canCreateReplenishment } from "@/lib/rolePrivileges";
import { replenishmentCreatePath } from "@/lib/procurementRouting";
import { useT } from "@/lib/i18n";
import { downloadCsv } from "@/lib/exportCsv";

type AlertRow = {
  articleId: number;
  name: string;
  sku: string;
  category: string;
  stock: number;
  reorder: number;
  status: string;
  purchaseOrderEligible: boolean;
  replenishmentEligible: boolean;
};

type Summary = { outOfStock: number; critical: number; low: number; healthy: number };

export default function InventoryPage() {
  const { t } = useT();
  const router = useRouter();
  const role = loadSession()?.role;
  const canRequestReplenishment = canCreateReplenishment(role);
  const [af, setAf] = useState("All");
  const [rows, setRows] = useState<AlertRow[]>([]);
  const [summaries, setSummaries] = useState<Summary | null>(null);

  const filterDefs = useMemo(
    () => [
      { api: "All", label: t.inventory.filterAll },
      { api: "Critical", label: t.inventory.filterCritical },
      { api: "Low", label: t.inventory.filterLow },
      { api: "Out of Stock", label: t.inventory.filterOut },
    ],
    [t],
  );

  const reload = useCallback(() => {
    apiGet<Summary>("/api/alerts/summary").then(setSummaries).catch(() => setSummaries(null));
    const q = af === "All" ? "" : `?status=${encodeURIComponent(af)}`;
    apiGet<AlertRow[]>(`/api/alerts${q}`).then(setRows).catch(() => setRows([]));
  }, [af]);

  useEffect(() => {
    reload();
  }, [reload]);

  const sumCards =
    summaries == null
      ? [
          { label: t.inventory.summaryOut, count: "—", color: T.danger },
          { label: t.inventory.summaryCrit, count: "—", color: T.warning },
          { label: t.inventory.summaryLow, count: "—", color: T.accent },
          { label: t.inventory.summaryHealthy, count: "—", color: T.success },
        ]
      : [
          { label: t.inventory.summaryOut, count: String(summaries.outOfStock), color: T.danger },
          { label: t.inventory.summaryCrit, count: String(summaries.critical), color: T.warning },
          { label: t.inventory.summaryLow, count: String(summaries.low), color: T.accent },
          { label: t.inventory.summaryHealthy, count: String(summaries.healthy), color: T.success },
        ];

  return (
    <>
      <Topbar title={t.inventory.title} subtitle={t.inventory.subtitle} />
      <Page>
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-xl font-bold" style={{ color: T.text }}>
              {t.inventory.heading}
            </h2>
            <p className="text-sm mt-1" style={{ color: T.text2 }}>
              {t.inventory.lead}
            </p>
          </div>
          <Btn
            type="button"
            disabled={rows.length === 0}
            onClick={() => {
              downloadCsv(t.inventory.exportCsvFilename, [
                t.inventory.tableName,
                t.inventory.tableSku,
                t.inventory.tableCategory,
                t.inventory.tableStock,
                t.inventory.tableReorder,
                t.inventory.tableStatus,
              ], rows.map((a) => [a.name, a.sku, a.category, String(a.stock), String(a.reorder), a.status]));
            }}
          >
            {Icons.download} {t.inventory.exportCsv}
          </Btn>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          {sumCards.map((s) => (
            <Card key={s.label} className="overflow-hidden">
              <div className="h-1 w-full" style={{ background: s.color }} />
              <div className="p-5">
                <p className="text-2xl font-bold" style={{ color: T.text }}>
                  {s.count}
                </p>
                <p className="text-sm mt-1" style={{ color: T.text2 }}>
                  {s.label}
                </p>
              </div>
            </Card>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {filterDefs.map((f) => (
            <button
              key={f.api}
              type="button"
              onClick={() => setAf(f.api)}
              className="px-4 py-1.5 rounded-lg text-xs font-semibold transition-all"
              style={
                af === f.api
                  ? { background: T.primary, color: "#fff" }
                  : { background: T.surface, color: T.text2, border: `1px solid ${T.border}` }
              }
            >
              {f.label}
            </button>
          ))}
        </div>

        <Card className="overflow-hidden overflow-x-auto">
          <table className="w-full border-collapse min-w-[800px]">
            <thead>
              <tr>
                <TH>{t.inventory.tableName}</TH>
                <TH>{t.inventory.tableSku}</TH>
                <TH>{t.inventory.tableCategory}</TH>
                <TH>{t.inventory.tableStock}</TH>
                <TH>{t.inventory.tableReorder}</TH>
                <TH>{t.inventory.tableStatus}</TH>
                <TH>{t.inventory.tableAction}</TH>
              </tr>
            </thead>
            <tbody>
              {rows.map((a) => {
                const stockColor = a.stock === 0 ? T.danger : a.stock < 6 ? T.danger : T.warning;
                const canAct = canRequestReplenishment && a.replenishmentEligible !== false;
                return (
                  <tr
                    key={`${a.articleId}-${a.sku}`}
                    className="border-t hover:bg-gray-50 transition-colors"
                    style={{ borderColor: T.border }}
                  >
                    <TD>
                      <span className="font-medium">{a.name}</span>
                    </TD>
                    <TD>
                      <span
                        className="font-mono text-xs px-2 py-0.5 rounded-md"
                        style={{ background: T.bg, color: T.text2 }}
                      >
                        {a.sku}
                      </span>
                    </TD>
                    <TD>{a.category}</TD>
                    <TD>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-base" style={{ color: stockColor }}>
                          {a.stock}
                        </span>
                        <div className="w-16 h-1.5 rounded-full overflow-hidden" style={{ background: "#FEE2E2" }}>
                          <div
                            className="h-full rounded-full"
                            style={{
                              background: stockColor,
                              width: `${Math.min((a.stock / Math.max(a.reorder, 1)) * 100, 100)}%`,
                            }}
                          />
                        </div>
                      </div>
                    </TD>
                    <TD style={{ color: T.text2 }}>{a.reorder}</TD>
                    <TD>
                      <Badge status={a.status} />
                    </TD>
                    <TD>
                      {canAct ? (
                        <Btn
                          size="sm"
                          type="button"
                          onClick={() => router.push(replenishmentCreatePath(a))}
                        >
                          {t.inventory.requestReplenishmentBtn}
                        </Btn>
                      ) : a.purchaseOrderEligible ? (
                        <span className="text-xs" style={{ color: T.text2 }}>
                          {t.inventory.newProductUseOrder}
                        </span>
                      ) : (
                        <span className="text-xs" style={{ color: T.text2 }}>
                          —
                        </span>
                      )}
                    </TD>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Card>
      </Page>
    </>
  );
}
