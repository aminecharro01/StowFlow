import type { IconKey } from "./Icons";

export type NavSection = "main" | "purchasing";

export type NavItem = {
  key: string;
  label: string;
  href: string;
  icon: IconKey;
  section?: NavSection;
};

export const NAV: NavItem[] = [
  { key: "dashboard", label: "Tableau de bord", href: "/dashboard", icon: "grid", section: "main" },
  { key: "products", label: "Articles", href: "/products", icon: "box", section: "main" },
  { key: "pos", label: "Point de vente", href: "/pos", icon: "cart", section: "main" },
  { key: "posHistory", label: "Ventes", href: "/pos/history", icon: "clipboard", section: "main" },
  { key: "inventory", label: "Stocks & alertes", href: "/inventory", icon: "warehouse", section: "main" },
  { key: "movements", label: "Mouvements", href: "/movements", icon: "clipboard", section: "main" },
  { key: "suppliers", label: "Fournisseurs", href: "/suppliers", icon: "truck", section: "main" },
  { key: "orders", label: "Commandes fournisseurs", href: "/orders", icon: "fileText", section: "purchasing" },
  { key: "replenishment", label: "Demandes réappro.", href: "/replenishment", icon: "warning", section: "purchasing" },
  { key: "platform", label: "Tenants SaaS", href: "/platform/tenants", icon: "layers", section: "main" },
  { key: "reports", label: "Rapports", href: "/reports", icon: "bar", section: "main" },
];
