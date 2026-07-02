/**
 * Matrice RBAC UI — alignée sur InventraPolicies (moindre privilège).
 */
export type AppRole =
  | "SUPER_ADMIN"
  | "TENANT_ADMIN"
  | "STOCK_MANAGER"
  | "SALES"
  | "MANAGER";

const ALL_TENANT: AppRole[] = [
  "SUPER_ADMIN",
  "TENANT_ADMIN",
  "STOCK_MANAGER",
  "SALES",
  "MANAGER",
];

/** Pilotage : lecture tableaux de bord, stocks, commandes, fournisseurs. */
const PILOTAGE: AppRole[] = ["SUPER_ADMIN", "TENANT_ADMIN", "STOCK_MANAGER", "MANAGER"];

const ADMIN: AppRole[] = ["SUPER_ADMIN", "TENANT_ADMIN"];

const STOCK_OPS: AppRole[] = ["STOCK_MANAGER"];

const NAV_ACCESS: Record<string, AppRole[]> = {
  dashboard: PILOTAGE,
  products: ALL_TENANT,
  pos: ["SALES"],
  posHistory: ["SALES", "MANAGER", "TENANT_ADMIN", "SUPER_ADMIN"],
  inventory: PILOTAGE,
  movements: ["SUPER_ADMIN", "TENANT_ADMIN", "STOCK_MANAGER", "MANAGER"],
  replenishment: PILOTAGE,
  suppliers: PILOTAGE,
  orders: PILOTAGE,
  reports: ["SUPER_ADMIN", "TENANT_ADMIN", "MANAGER"],
  platform: ["SUPER_ADMIN"],
};

export function parseAppRole(role: string | null | undefined): AppRole | null {
  if (!role) return null;
  const r = role.trim() as AppRole;
  return ALL_TENANT.includes(r) ? r : null;
}

export function canAccessNavKey(role: string | null | undefined, navKey: string): boolean {
  const p = parseAppRole(role);
  if (!p) return false;
  const allowed = NAV_ACCESS[navKey];
  if (!allowed) return true;
  return allowed.includes(p);
}

/** Mouvements manuels de stock (sorties, ajustements). */
export function canRecordStockMovements(role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  return p === "STOCK_MANAGER";
}

export function canEditArticleCatalog(role: string | null | undefined): boolean {
  return canRecordStockMovements(role);
}

export function canEditSuppliers(role: string | null | undefined): boolean {
  return canRecordStockMovements(role);
}

export function canAccessAdminSettings(role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  return p === "SUPER_ADMIN" || p === "TENANT_ADMIN";
}

export function canFilterPosSalesBySeller(role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  return p === "SUPER_ADMIN" || p === "TENANT_ADMIN" || p === "MANAGER";
}

export function canManagePurchaseOrders(role: string | null | undefined): boolean {
  return canRecordStockMovements(role);
}

/** Création commande fournisseur — gestionnaire stock uniquement. */
export function canCreatePurchaseOrder(role: string | null | undefined): boolean {
  return canManagePurchaseOrders(role);
}

/** @deprecated Alias de canManagePurchaseOrders */
export function canReceivePurchaseOrder(role: string | null | undefined): boolean {
  return canManagePurchaseOrders(role);
}

export function canCancelPurchaseOrder(role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  return p === "SUPER_ADMIN" || p === "TENANT_ADMIN";
}

/** Demande réappro — manager ou gestionnaire stock. */
export function canCreateReplenishment(role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  return p === "STOCK_MANAGER" || p === "MANAGER";
}

/** Traitement / réception réappro — gestionnaire stock uniquement. */
export function canProcessReplenishment(role: string | null | undefined): boolean {
  return canRecordStockMovements(role);
}

export function defaultLandingPath(role: string | null | undefined): string {
  const p = parseAppRole(role);
  if (p === "SALES") return "/pos";
  if (p === "SUPER_ADMIN") return "/platform/tenants";
  return "/dashboard";
}

export function isRouteAllowed(pathname: string, role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  if (!p) return false;
  if (pathname.startsWith("/help")) return true;
  if (pathname.startsWith("/settings")) return canAccessAdminSettings(role);
  if (pathname.startsWith("/dashboard")) return canAccessNavKey(role, "dashboard");
  if (pathname.startsWith("/products")) return canAccessNavKey(role, "products");
  if (pathname.startsWith("/pos/history")) return canAccessNavKey(role, "posHistory");
  if (pathname.startsWith("/pos")) return canAccessNavKey(role, "pos");
  if (pathname.startsWith("/inventory")) return canAccessNavKey(role, "inventory");
  if (pathname.startsWith("/movements")) return canAccessNavKey(role, "movements");
  if (pathname.startsWith("/replenishment")) return canAccessNavKey(role, "replenishment");
  if (pathname.startsWith("/suppliers")) return canAccessNavKey(role, "suppliers");
  if (pathname.startsWith("/orders")) return canAccessNavKey(role, "orders");
  if (pathname.startsWith("/reports")) return canAccessNavKey(role, "reports");
  if (pathname.startsWith("/platform")) return canAccessNavKey(role, "platform");
  return true;
}
