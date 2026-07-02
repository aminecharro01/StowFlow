"use client";

import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Icons } from "@/components/inventra/Icons";
import { LucideByName } from "@/components/inventra/LucideByName";
import { T } from "@/lib/theme";
import { useDashboard } from "@/hooks/useDashboard";
import { AlertBanner } from "@/components/inventra/ui/AlertBanner";
import { useT } from "@/lib/i18n";
import { CalendarDays } from "lucide-react";

const REVENUE_DATA = [
  { month: 0, v26: 42, v25: 38 },
  { month: 1, v26: 55, v25: 44 },
  { month: 2, v26: 48, v25: 51 },
  { month: 3, v26: 71, v25: 58 },
  { month: 4, v26: 63, v25: 49 },
  { month: 5, v26: 78, v25: 66 },
  { month: 6, v26: 0, v25: 71 },
  { month: 7, v26: 0, v25: 68 },
  { month: 8, v26: 0, v25: 74 },
  { month: 9, v26: 0, v25: 80 },
  { month: 10, v26: 0, v25: 75 },
  { month: 11, v26: 0, v25: 88 },
];

const TOP_PRODUCTS = [
  { nameKey: 0, units: 1248, rev: "186 k DH", pct: 28 },
  { nameKey: 1, units: 844, rev: "67 k DH", pct: 19 },
  { nameKey: 2, units: 701, rev: "28 k DH", pct: 16 },
  { nameKey: 3, units: 612, rev: "73 k DH", pct: 14 },
  { nameKey: 4, units: 489, rev: "24 k DH", pct: 11 },
];

const SALES_BY_CATEGORY = [
  { key: "electronics" as const, pct: 42, color: T.primary },
  { key: "accessories" as const, pct: 28, color: T.accent },
  { key: "office" as const, pct: 18, color: T.warning },
  { key: "other" as const, pct: 12, color: T.text2 },
];

const MOCK_KPI_VALUES = [
  { value: "284 500 DH", change: "+14,2%", positive: true },
  { value: "8,412", change: "+9.7%", positive: true },
  { value: "2.1%", change: "-0.4%", positive: true },
  { value: "38.4%", change: "+2.1%", positive: true },
];

export default function ReportsPage() {
  const { t } = useT();
  const { data: dashboard } = useDashboard();
  const liveKpis = dashboard?.kpis ?? [];
  const maxBar = 100;

  const mockKpiLabels = [
    t.reports.mockKpis.revenue,
    t.reports.mockKpis.unitsSold,
    t.reports.mockKpis.returnRate,
    t.reports.mockKpis.grossMargin,
  ];

  const mockKpis = MOCK_KPI_VALUES.map((v, i) => ({
    label: mockKpiLabels[i],
    ...v,
  }));

  async function exportPdf() {
    const { jsPDF } = await import("jspdf");
    const doc = new jsPDF({ unit: "mm", format: "a4" });
    const margin = 14;
    const pageBottom = 287;
    let y = 18;

    const ensureSpace = (neededMm: number) => {
      if (y + neededMm > pageBottom) {
        doc.addPage();
        y = 18;
      }
    };

    const heading = (text: string, size = 11) => {
      ensureSpace(8);
      doc.setFont("helvetica", "bold");
      doc.setFontSize(size);
      doc.setTextColor(30);
      doc.text(text, margin, y);
      y += size * 0.45 + 2;
    };

    const bodyLines = (lines: string[], size = 9.5) => {
      doc.setFont("helvetica", "normal");
      doc.setFontSize(size);
      doc.setTextColor(45);
      for (const line of lines) {
        const wrapped = doc.splitTextToSize(line, 180);
        for (const w of wrapped) {
          ensureSpace(5);
          doc.text(w, margin, y);
          y += 5;
        }
      }
      y += 2;
    };

    doc.setFont("helvetica", "bold");
    doc.setFontSize(16);
    doc.setTextColor(20);
    doc.text(t.reports.pdfHeading, margin, y);
    y += 10;

    doc.setFont("helvetica", "normal");
    doc.setFontSize(10);
    doc.setTextColor(90);
    doc.text(t.reports.period, margin, y);
    y += 12;
    doc.setTextColor(20);

    heading(t.reports.pdfKpisHeading, 11);
    bodyLines(mockKpis.map((k) => `${k.label}: ${k.value} (${k.change})`));

    heading(t.reports.pdfSalesByCategoryHeading, 11);
    bodyLines(
      SALES_BY_CATEGORY.map(
        (c) => `${t.reports.mockCategories[c.key]}: ${c.pct}%`,
      ),
    );

    doc.save(t.reports.pdfFilename);
  }

  return (
    <>
      <Topbar title={t.reports.title} subtitle={t.reports.subtitle} />
      <Page>
        <AlertBanner variant="warning" className="mb-4">
          {t.reports.demoBanner}
        </AlertBanner>
        <div className="flex items-start justify-between flex-col sm:flex-row gap-3">
          <div>
            <h2 className="text-xl font-bold" style={{ color: T.text }}>
              {t.reports.analyticsOverview}
            </h2>
            <p className="text-sm mt-1" style={{ color: T.text2 }}>
              {t.reports.period}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span
              className="text-xs px-3 py-2 rounded-lg border font-medium inline-flex items-center gap-1.5"
              style={{ color: T.text, borderColor: T.border }}
            >
              <CalendarDays size={14} strokeWidth={2} className="shrink-0" style={{ color: T.primary }} aria-hidden />
              {t.reports.periodShort}
            </span>
            <button
              type="button"
              onClick={() => void exportPdf()}
              className="text-xs px-3 py-2 rounded-lg border font-semibold flex items-center gap-1.5 hover:bg-gray-50 transition-colors active:scale-[0.98]"
              style={{ color: T.primary, borderColor: T.border }}
            >
              {Icons.download} {t.reports.exportPdf}
            </button>
          </div>
        </div>

        <p className="text-xs font-semibold uppercase tracking-wide" style={{ color: T.accent }}>
          {t.reports.liveSectionTitle}
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          {(liveKpis.length > 0
            ? liveKpis.map((k) => ({ label: k.title, value: k.value, change: k.change, positive: k.positive }))
            : mockKpis
          ).map((k) => (
            <Card key={k.label} className="p-5 flex flex-col gap-2 hover:shadow-md transition-shadow">
              <p className="text-xs font-medium" style={{ color: T.text2 }}>
                {k.label}
              </p>
              <p className="text-2xl font-bold" style={{ color: T.text }}>
                {k.value}
              </p>
              {k.change ? (
                <span
                  className="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-md w-fit"
                  style={{
                    background: k.positive ? "rgba(34,197,94,0.1)" : "rgba(239,68,68,0.1)",
                    color: k.positive ? T.success : T.danger,
                  }}
                >
                  {k.positive ? Icons.trendUp : Icons.trendDown}
                  {k.change}
                </span>
              ) : null}
            </Card>
          ))}
        </div>

        <p className="text-xs font-semibold uppercase tracking-wide pt-2" style={{ color: T.text2 }}>
          {t.reports.mockSectionTitle}
        </p>

        <div className="grid gap-4 xl:grid-cols-[1fr_300px]">
          <Card className="p-5">
            <div className="flex items-start justify-between mb-4 flex-col sm:flex-row gap-2">
              <div>
                <h3 className="text-sm font-semibold" style={{ color: T.text }}>
                  {t.reports.revenueTrend}
                </h3>
                <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
                  {t.reports.revenueTrendSubtitle}
                </p>
              </div>
              <div className="flex items-center gap-3">
                {[
                  { c: T.primary, l: t.reports.mockYear2026 },
                  { c: T.border, l: t.reports.mockYear2025 },
                ].map((l) => (
                  <div key={l.l} className="flex items-center gap-1.5">
                    <div className="w-3 h-3 rounded-sm" style={{ background: l.c }} />
                    <span className="text-xs" style={{ color: T.text2 }}>
                      {l.l}
                    </span>
                  </div>
                ))}
              </div>
            </div>
            <div className="flex gap-3 items-end" style={{ height: 140 }}>
              <div className="flex flex-col justify-between h-full text-right pr-1">
                {[100, 75, 50, 25, 0].map((n) => (
                  <span key={n} className="text-xs leading-none" style={{ color: T.text2 }}>
                    {n}
                  </span>
                ))}
              </div>
              <div className="flex-1 flex items-end gap-1.5 h-full relative">
                <div className="absolute inset-0 flex flex-col justify-between pointer-events-none">
                  {[0, 1, 2, 3, 4].map((i) => (
                    <div key={i} className="border-t w-full" style={{ borderColor: "#F3F4F6" }} />
                  ))}
                </div>
                {REVENUE_DATA.map((d) => (
                  <div key={d.month} className="flex-1 flex flex-col items-center gap-1 group">
                    <div className="relative w-full flex items-end justify-center gap-0.5" style={{ height: 120 }}>
                      {d.v25 > 0 && (
                        <div
                          className="flex-1 rounded-t-sm"
                          style={{
                            height: `${(d.v25 / maxBar) * 100}%`,
                            background: T.border,
                            minHeight: 4,
                          }}
                        />
                      )}
                      {d.v26 > 0 && (
                        <div
                          className="flex-1 rounded-t-sm"
                          style={{
                            height: `${(d.v26 / maxBar) * 100}%`,
                            background: T.primary,
                            minHeight: 4,
                          }}
                        />
                      )}
                    </div>
                    <span className="text-[9px]" style={{ color: T.text2 }}>
                      {t.reports.mockMonths[d.month]}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </Card>

          <Card className="p-5 flex flex-col gap-4">
            <div>
              <h3 className="text-sm font-semibold" style={{ color: T.text }}>
                {t.reports.salesByCategory}
              </h3>
              <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
                {t.reports.salesByCategorySubtitle}
              </p>
            </div>
            <div className="flex flex-col gap-4">
              {SALES_BY_CATEGORY.map((c) => (
                <div key={c.key} className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium" style={{ color: T.text }}>
                      {t.reports.mockCategories[c.key]}
                    </span>
                    <span className="text-xs font-bold" style={{ color: c.color }}>
                      {c.pct}%
                    </span>
                  </div>
                  <div className="h-2 rounded-full overflow-hidden" style={{ background: T.bg }}>
                    <div
                      className="h-full rounded-full transition-all duration-500"
                      style={{ width: `${c.pct}%`, background: c.color }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>

        <div className="grid gap-4 xl:grid-cols-[1fr_300px]">
          <Card className="overflow-hidden">
            <div className="px-5 py-4 border-b" style={{ borderColor: T.border }}>
              <h3 className="text-sm font-semibold" style={{ color: T.text }}>
                {t.reports.topProducts}
              </h3>
            </div>
            <div className="divide-y divide-gray-100">
              {TOP_PRODUCTS.map((p, i) => (
                <div key={i} className="flex items-center gap-4 px-5 py-3.5 hover:bg-gray-50 transition-colors">
                  <span className="text-sm font-bold w-5 text-center flex-shrink-0" style={{ color: T.text2 }}>
                    {i + 1}
                  </span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate" style={{ color: T.text }}>
                      {t.reports.mockTopProducts[p.nameKey]?.name ?? ""}
                    </p>
                    <div className="mt-1 h-1.5 rounded-full overflow-hidden" style={{ background: T.bg }}>
                      <div className="h-full rounded-full" style={{ width: `${p.pct}%`, background: T.primary }} />
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="text-sm font-semibold" style={{ color: T.text }}>
                      {p.rev}
                    </p>
                    <p className="text-xs" style={{ color: T.text2 }}>
                      {p.units} {t.reports.unitsLabel}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          <Card className="overflow-hidden">
            <div className="px-5 py-4 border-b" style={{ borderColor: T.border }}>
              <h3 className="text-sm font-semibold" style={{ color: T.text }}>
                {t.reports.recentActivity}
              </h3>
            </div>
            <div className="px-5 py-3 flex flex-col gap-0">
              {t.reports.mockActivities.map((a, i) => (
                <div key={i} className="flex items-start gap-3 py-3 relative">
                  {i < t.reports.mockActivities.length - 1 && (
                    <div
                      className="absolute left-[18px] top-8 bottom-0 w-px"
                      style={{ background: T.border }}
                    />
                  )}
                  <div
                    className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 relative z-10 bg-white"
                    style={{ border: `2px solid ${T.primary}`, color: T.primary }}
                  >
                    <LucideByName name="Package" size={14} strokeWidth={2} />
                  </div>
                  <div className="flex-1 min-w-0 pt-1">
                    <p className="text-xs font-medium" style={{ color: T.text }}>
                      {a.label}
                    </p>
                    <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
                      {a.time}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </Page>
    </>
  );
}
