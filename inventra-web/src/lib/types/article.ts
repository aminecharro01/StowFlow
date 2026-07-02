import { apiGet } from "@/lib/api";
import { fetchAllPages, type PageResponse } from "./pagination";

export type ArticleDto = {
  id: number;
  name: string;
  sku: string;
  category: string;
  qty: number;
  price: number;
  status: string;
  minThreshold?: number;
  maxThreshold?: number;
  description?: string | null;
  newProduct?: boolean;
  purchaseOrderEligible?: boolean;
  replenishmentEligible?: boolean;
  suggestedPurchaseOrderQty?: number;
  suggestedReplenishmentQty?: number;
};

export async function fetchArticlesPage(
  page: number,
  size: number,
  params?: { q?: string; status?: string }
): Promise<PageResponse<ArticleDto>> {
  const qs = new URLSearchParams();
  qs.set("page", String(page));
  qs.set("size", String(size));
  if (params?.q) qs.set("q", params.q);
  if (params?.status && params.status !== "All") qs.set("status", params.status);
  return apiGet<PageResponse<ArticleDto>>(`/api/articles?${qs}`);
}

export async function fetchAllArticles(params?: { q?: string; status?: string }): Promise<ArticleDto[]> {
  return fetchAllPages((page, size) => fetchArticlesPage(page, size, params));
}
