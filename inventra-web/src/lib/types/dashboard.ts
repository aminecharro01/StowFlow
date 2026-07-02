export type DashboardKpi = {
  title: string;
  value: string;
  change: string;
  label: string;
  positive: boolean;
  accent: string;
  icon?: string;
};

export type DashboardPayload = {
  kpis: DashboardKpi[];
  quickStats: { label: string; value: string; color: string }[];
  movementBars: { month: string; v: number }[];
  categoryDonut: { name: string; pct: number; color: string }[];
  totalUnits: number;
  topAlerts: {
    articleId: number;
    name: string;
    sku: string;
    stock: number;
    reorder: number;
    status: string;
  }[];
};
