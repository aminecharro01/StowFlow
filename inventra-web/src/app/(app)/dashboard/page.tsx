"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Icons } from "@/components/inventra/Icons";
import { LucideByName } from "@/components/inventra/LucideByName";
import { T } from "@/lib/theme";
import { useDashboard } from "@/hooks/useDashboard";
import { loadSession } from "@/lib/auth";
import { canCreateReplenishment } from "@/lib/rolePrivileges";
import { replenishmentCreatePath } from "@/lib/procurementRouting";
import { useT } from "@/lib/i18n";
import { getDisplayName } from "@/lib/userDisplay";
import { CalendarDays, Hand } from "lucide-react";

type Kpi = {
  title: string;
  value: string;
  change: string;
  label: string;
  positive: boolean;
  accent: string;
  icon?: string;
  emoji?: string;
};
type QuickStat = { label: string; value: string; color: string };
type BarPoint = { month: string; v: number };
type DonutSlice = { name: string; pct: number; color: string };
type AlertRow = {
  articleId: number;
  name: string;
  sku: string;
  stock: number;
  reorder: number;
  status: string;
  purchaseOrderEligible?: boolean;
  replenishmentEligible?: boolean;
};

type DashboardPayload = {
  kpis: Kpi[];
  quickStats: QuickStat[];
  movementBars: BarPoint[];
  categoryDonut: DonutSlice[];
  totalUnits: number;
  topAlerts: AlertRow[];
};

function kpiLucideName(k: Kpi) {
  return k.icon ?? "Package";
}

function KPICard({ k }: { k: Kpi }) {
  return (
    <Card className="p-5 flex flex-col gap-3">
      <div className="flex items-start justify-between">
        <span className="text-sm font-medium" style={{ color: T.text2 }}>
          {k.title}
        </span>
        <div
          className="w-9 h-9 rounded-xl flex items-center justify-center"
          style={{ background: `${k.accent}18`, color: k.accent }}
        >
          <LucideByName name={kpiLucideName(k)} size={20} strokeWidth={2} />
        </div>
      </div>
      <p className="text-2xl font-bold tracking-tight" style={{ color: T.text }}>
        {k.value}
      </p>
      <div className="flex items-center gap-2">
        {k.change ? (
          <span
            className="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-md"
            style={{
              background: k.positive ? "rgba(34,197,94,0.1)" : "rgba(239,68,68,0.1)",
              color: k.positive ? T.success : T.danger,
            }}
          >
            {k.positive ? Icons.trendUp : Icons.trendDown}
            {k.change}
          </span>
        ) : null}
        <span className="text-xs" style={{ color: T.text2 }}>
          {k.label}
        </span>
      </div>
    </Card>
  );
}

function BarChartCard({ data }: { data: BarPoint[] }) {
  const { t } = useT();
  const max = Math.max(...data.map((d) => d.v), 1);
  return (
    <Card className="p-5 flex flex-col gap-4">
      <div>
        <h3 className="text-sm font-semibold" style={{ color: T.text }}>
          {t.dashboard.chartMovementTitle}
        </h3>
        <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
          {t.dashboard.chartMovementSubtitle}
        </p>
      </div>
      <div className="flex gap-3 items-end" style={{ height: 160 }}>
        <div className="flex flex-col justify-between h-full text-right pr-1">
          {[100, 75, 50, 25, 0].map((n) => (
            <span key={n} className="text-xs leading-none" style={{ color: T.text2 }}>
              {n}
            </span>
          ))}
        </div>
        <div className="flex-1 flex items-end gap-2 h-full relative">
          <div className="absolute inset-0 flex flex-col justify-between pointer-events-none">
            {[0, 1, 2, 3, 4].map((i) => (
              <div key={i} className="border-t w-full" style={{ borderColor: "#F3F4F6" }} />
            ))}
          </div>
          {data.map((d, i) => {
            const isLast = i === data.length - 1;
            const h = `${(d.v / max) * 100}%`;
            return (
              <div key={`${d.month}-${i}`} className="flex-1 flex flex-col items-center gap-1.5 group">
                <div className="relative w-full flex items-end justify-center" style={{ height: 136 }}>
                  <div
                    className="w-full rounded-t-lg transition-all duration-200 group-hover:opacity-75 relative"
                    style={{
                      height: h,
                      background: isLast ? T.primary : T.border,
                      minHeight: 4,
                    }}
                  >
                    <span className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs px-2 py-0.5 rounded whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                      {d.v} {t.dashboard.barTooltipUnits}
                    </span>
                  </div>
                </div>
                <span className="text-xs" style={{ color: T.text2 }}>
                  {d.month}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </Card>
  );
}

function DonutChartCard({ slices, totalUnits }: { slices: DonutSlice[]; totalUnits: number }) {
  const { t } = useT();
  const R = 52,
    CX = 64,
    STROKE = 22,
    circum = 2 * Math.PI * R;
  let offset = 0;
  return (
    <Card className="p-5 flex flex-col gap-4">
      <div>
        <h3 className="text-sm font-semibold" style={{ color: T.text }}>
          {t.dashboard.chartDonutTitle}
        </h3>
        <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
          {t.dashboard.chartDonutSubtitle}
        </p>
      </div>
      <div className="flex items-center gap-5">
        <div className="relative flex-shrink-0" style={{ width: 128, height: 128 }}>
          <svg width={128} height={128} viewBox="0 0 128 128" style={{ transform: "rotate(-90deg)" }}>
            {slices.map((cat) => {
              const dash = (cat.pct / 100) * circum;
              const gap = circum - dash;
              const off = (-offset * circum) / 100;
              offset += cat.pct;
              return (
                <circle
                  key={cat.name}
                  cx={CX}
                  cy={CX}
                  r={R}
                  fill="none"
                  stroke={cat.color}
                  strokeWidth={STROKE}
                  strokeDasharray={`${dash} ${gap}`}
                  strokeDashoffset={off}
                  className="transition-opacity duration-200 hover:opacity-80 cursor-pointer"
                />
              );
            })}
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-base font-bold" style={{ color: T.text }}>
              {totalUnits.toLocaleString("fr-FR")}
            </span>
            <span className="text-xs" style={{ color: T.text2 }}>
              {t.dashboard.units}
            </span>
          </div>
        </div>
        <div className="flex flex-col gap-2.5 flex-1">
          {slices.length === 0 ? (
            <p className="text-xs" style={{ color: T.text2 }}>
              {t.dashboard.chartDonutEmpty}
            </p>
          ) : (
            slices.map((cat) => (
            <div key={cat.name} className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full" style={{ background: cat.color }} />
                <span className="text-xs" style={{ color: T.text }}>
                  {cat.name}
                </span>
              </div>
              <span className="text-xs font-semibold" style={{ color: T.text }}>
                {cat.pct}%
              </span>
            </div>
          ))
          )}
        </div>
      </div>
    </Card>
  );
}

function LowStockPanel({
  alerts,
  role,
}: {
  alerts: AlertRow[];
  role: string | null | undefined;
}) {
  const { t } = useT();
  const router = useRouter();
  const crit = alerts.filter((a) => a.status === "Critical" || a.status === "Out of Stock").length;
  return (
    <Card>
      <div
        className="flex items-center justify-between px-5 py-3.5 border-b"
        style={{ borderColor: T.border }}
      >
        <div className="flex items-center gap-2" style={{ color: T.warning }}>
          {Icons.warning}
          <span className="text-sm font-semibold">{t.dashboard.lowStockTitle}</span>
        </div>
        <div className="flex items-center gap-2">
          <span
            className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold text-white"
            style={{ background: T.danger }}
          >
            {crit || alerts.length}
          </span>
          <Link
            href="/inventory"
            className="text-xs font-medium inline-flex items-center gap-0.5 hover:underline"
            style={{ color: T.primary }}
          >
            {t.dashboard.viewAll}
            {Icons.chevronRight}
          </Link>
        </div>
      </div>
      {alerts.slice(0, 3).map((a, i) => (
        <div
          key={a.sku}
          className="flex items-center justify-between px-5 py-3.5 hover:bg-gray-50 transition-colors"
          style={{ borderBottom: i < 2 ? `1px solid ${T.border}` : "none" }}
        >
          <div>
            <p className="text-sm font-medium" style={{ color: T.text }}>
              {a.name}
            </p>
            <p className="text-xs mt-0.5" style={{ color: T.text2 }}>
              {a.sku}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <div className="w-20 h-1.5 rounded-full overflow-hidden" style={{ background: "#FEE2E2" }}>
              <div
                className="h-full rounded-full"
                style={{
                  background: T.danger,
                  width: `${Math.min((a.stock / Math.max(a.reorder, 1)) * 100, 100)}%`,
                }}
              />
            </div>
            <span
              className="text-xs font-semibold px-2.5 py-1 rounded-lg"
              style={{ background: "rgba(239,68,68,0.1)", color: T.danger }}
            >
              {a.stock} {t.dashboard.stockLeft}
            </span>
            {canCreateReplenishment(role) && a.replenishmentEligible !== false && (
              <Btn
                size="sm"
                type="button"
                onClick={() => router.push(replenishmentCreatePath(a))}
              >
                {t.dashboard.requestReplenishment}
              </Btn>
            )}
          </div>
        </div>
      ))}
    </Card>
  );
}

export default function DashboardPage() {
  const { t } = useT();
  const { data, error, isLoading } = useDashboard();
  const displayName = getDisplayName(loadSession()?.email);
  const err = error instanceof Error ? error.message : error ? String(error) : null;
  const role = loadSession()?.role;
  const bars = data?.movementBars.map((b) => ({ month: b.month, v: b.v })) ?? [];
  const alerts: AlertRow[] =
    data?.topAlerts.map((a) => ({
      articleId: a.articleId,
      name: a.name,
      sku: a.sku,
      stock: a.stock,
      reorder: a.reorder,
      status: a.status,
    })) ?? [];

  return (
    <>
      <Topbar title={t.dashboard.title} subtitle={t.dashboard.subtitle(displayName)} />
      <Page>
        {err && (
          <Card className="p-4 border border-red-200 text-sm text-red-700">
            {t.dashboard.apiError} : {err}. {t.dashboard.apiErrorHint}
          </Card>
        )}
        {!data && !err && isLoading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            {[0, 1, 2, 3].map((i) => (
              <Card key={i} className="p-5 flex flex-col gap-3">
                <div className="h-4 w-24 rounded bg-gray-200 animate-pulse" />
                <div className="h-8 w-32 rounded bg-gray-200 animate-pulse" />
                <div className="h-4 w-20 rounded bg-gray-200 animate-pulse" />
              </Card>
            ))}
          </div>
        )}
        {data && (
          <>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 className="text-xl font-bold flex items-center gap-2 flex-wrap" style={{ color: T.text }}>
                  {t.dashboard.greeting(displayName)}
                  <Hand className="shrink-0" size={22} strokeWidth={2} style={{ color: T.primary }} aria-hidden />
                </h2>
                <p className="text-sm mt-1" style={{ color: T.text2 }}>
                  {t.dashboard.snapshot}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className="text-xs px-3 py-1.5 rounded-lg font-medium inline-flex items-center gap-1.5"
                  style={{ background: "#EFF6FF", color: T.primary }}
                >
                  <CalendarDays size={14} strokeWidth={2} className="shrink-0" aria-hidden />
                  {t.dashboard.period}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
              {data.kpis.map((k) => (
                <KPICard key={k.title} k={k} />
              ))}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
              {data.quickStats.map((s) => (
                <Card key={s.label} className="px-5 py-3 flex items-center justify-between">
                  <span className="text-xs font-medium" style={{ color: T.text2 }}>
                    {s.label}
                  </span>
                  <span className="text-lg font-bold" style={{ color: s.color }}>
                    {s.value}
                  </span>
                </Card>
              ))}
            </div>

            <div className="grid gap-4 xl:grid-cols-[1fr_320px]">
              <BarChartCard data={bars} />
              <DonutChartCard slices={data.categoryDonut} totalUnits={data.totalUnits} />
            </div>

            <LowStockPanel alerts={alerts} role={loadSession()?.role} />
          </>
        )}
      </Page>
    </>
  );
}
