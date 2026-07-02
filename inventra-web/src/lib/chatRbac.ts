/**
 * RBAC assistant chat — aligné sur rolePrivileges.ts / InventraPolicies.
 */
import type { AppRole } from "@/lib/rolePrivileges";
import {
  canAccessNavKey,
  canCreatePurchaseOrder,
  canCreateReplenishment,
  parseAppRole,
} from "@/lib/rolePrivileges";
import type { ChatAction, ChatApiResponse, ConfirmAction } from "@/components/inventra/chat/chatTypes";

export function getQuickChipsForRole(role: string | null | undefined): string[] {
  const p = parseAppRole(role);
  if (!p) return ["Différence commande et réappro"];

  const chips: string[] = [];
  if (canAccessNavKey(role, "inventory") || p === "SALES") {
    chips.push("Quels articles sont en alerte ?");
  }
  if (canAccessNavKey(role, "dashboard")) {
    chips.push("Résumé du dashboard");
  }
  if (p === "SUPER_ADMIN") {
    chips.push("Ouvrir la plateforme");
  }
  if (canAccessNavKey(role, "orders")) {
    chips.push("Commandes en attente");
  }
  if (canAccessNavKey(role, "replenishment") && !canCreatePurchaseOrder(role)) {
    chips.push("Demandes réappro en attente");
  }
  if (canCreateReplenishment(role)) {
    chips.push("Créer réappro pour SKU-0088");
  }
  if (canCreatePurchaseOrder(role) || p === "SALES") {
    chips.push("Stock du SKU-0021");
  }
  if (p === "SALES") {
    chips.push("Ouvrir le point de vente");
  }
  chips.push("Différence commande et réappro");
  return [...new Set(chips)];
}

export function filterChatAction(role: string | null | undefined, action: ChatAction): boolean {
  const p = parseAppRole(role);
  if (!p || !action.href) return false;
  const path = action.href.split("?")[0];
  const full = action.href;

  if (path.startsWith("/platform")) return p === "SUPER_ADMIN";
  if (path.startsWith("/settings")) return p === "SUPER_ADMIN" || p === "TENANT_ADMIN";
  if (path.startsWith("/pos/history")) return canAccessNavKey(role, "posHistory");
  if (path.startsWith("/pos")) return p === "SALES";
  if (path.startsWith("/dashboard")) return canAccessNavKey(role, "dashboard");
  if (path.startsWith("/inventory")) return canAccessNavKey(role, "inventory");
  if (path.startsWith("/movements")) return canAccessNavKey(role, "movements");
  if (path.startsWith("/orders")) {
    if (full.includes("create=1")) return canCreatePurchaseOrder(role);
    return canAccessNavKey(role, "orders");
  }
  if (path.startsWith("/replenishment")) {
    if (full.includes("create=1")) return canCreateReplenishment(role);
    return canAccessNavKey(role, "replenishment");
  }
  if (path.startsWith("/suppliers")) return canAccessNavKey(role, "suppliers");
  if (path.startsWith("/reports")) return canAccessNavKey(role, "reports");
  if (path.startsWith("/products")) return canAccessNavKey(role, "products");
  if (path.startsWith("/help")) return true;
  return false;
}

export function filterConfirmAction(
  role: string | null | undefined,
  action: ConfirmAction | null | undefined,
): ConfirmAction | null {
  if (!action) return null;
  if (action.type === "create_replenishment" && canCreateReplenishment(role)) {
    return action;
  }
  return null;
}

export function filterChatResponse(
  role: string | null | undefined,
  res: ChatApiResponse,
): ChatApiResponse {
  return {
    ...res,
    suggestions: (res.suggestions ?? []).filter((s) => suggestionAllowedForRole(role, s)),
    actions: (res.actions ?? []).filter((a) => filterChatAction(role, a)),
    confirmAction: filterConfirmAction(role, res.confirmAction),
  };
}

function suggestionAllowedForRole(role: string | null | undefined, suggestion: string): boolean {
  const p = parseAppRole(role);
  if (!p) return false;
  const s = suggestion.toLowerCase();

  if (s.includes("plateforme") || s.includes("tenant")) return p === "SUPER_ADMIN";
  if (s.includes("point de vente") || s.includes("pos")) return p === "SALES";
  if (s.includes("dashboard") || s.includes("tableau")) return canAccessNavKey(role, "dashboard");
  if (s.includes("commande")) {
    if (s.includes("créer") || s.includes("creer") || s.includes("commander")) {
      return canCreatePurchaseOrder(role);
    }
    return canAccessNavKey(role, "orders");
  }
  if (s.includes("réappro") || s.includes("reappro")) {
    if (s.includes("créer") || s.includes("creer")) return canCreateReplenishment(role);
    return canAccessNavKey(role, "replenishment");
  }
  if (s.includes("alerte") || s.includes("rupture") || s.includes("inventaire")) {
    return canAccessNavKey(role, "inventory") || p === "SALES";
  }
  if (s.includes("stock")) return canAccessNavKey(role, "products");
  if (s.includes("fournisseur")) return canAccessNavKey(role, "suppliers");
  if (s.includes("aide")) return true;
  if (s.includes("différence") || s.includes("difference") || s.includes("plafond")) return true;
  return false;
}

export function welcomeForRole(role: AppRole | null): string {
  switch (role) {
    case "SUPER_ADMIN":
      return "Bonjour ! Assistant StowFlow (Gemini). Posez votre question en français naturel.";
    case "TENANT_ADMIN":
      return "Bonjour ! Assistant StowFlow (Gemini). Pilotage, commandes et alertes.";
    case "STOCK_MANAGER":
      return "Bonjour ! Assistant StowFlow (Gemini). Stock, alertes, commandes et réappro.";
    case "MANAGER":
      return "Bonjour ! Assistant StowFlow (Gemini). Pilotage, alertes et réapprovisionnements.";
    case "SALES":
      return "Bonjour ! Assistant StowFlow (Gemini). Disponibilité articles et point de vente.";
    default:
      return "Bonjour ! Posez une question en français naturel.";
  }
}
