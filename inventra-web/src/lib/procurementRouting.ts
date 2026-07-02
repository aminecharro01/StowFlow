import { canCreatePurchaseOrder, canCreateReplenishment } from "@/lib/rolePrivileges";

export type ProcurementTarget = "order" | "replenishment";

export type ProcurementArticle = {
  id?: number;
  articleId?: number;
  purchaseOrderEligible?: boolean;
  replenishmentEligible?: boolean;
};

export function articleIdOf(article: ProcurementArticle): number {
  const id = article.articleId ?? article.id;
  if (id == null) throw new Error("Article id required for procurement routing.");
  return id;
}

export function resolveProcurementTarget(article: ProcurementArticle): ProcurementTarget | null {
  if (article.replenishmentEligible === true) return "replenishment";
  if (article.purchaseOrderEligible !== false) return "order";
  return null;
}

export function replenishmentCreatePath(article: ProcurementArticle): string {
  return `/replenishment?create=1&articleId=${articleIdOf(article)}`;
}

export function procurementCreatePath(article: ProcurementArticle): string | null {
  const target = resolveProcurementTarget(article);
  if (!target) return null;
  const id = articleIdOf(article);
  const base = target === "replenishment" ? "/replenishment" : "/orders";
  return `${base}?create=1&articleId=${id}`;
}

export function canProcureArticle(
  role: string | null | undefined,
  article: ProcurementArticle
): boolean {
  const target = resolveProcurementTarget(article);
  if (target === "replenishment") return canCreateReplenishment(role);
  if (target === "order") return canCreatePurchaseOrder(role);
  return false;
}
