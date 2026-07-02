import { apiGet } from "@/lib/api";
import type { PageResponse } from "./pagination";

export type MovementDto = {
  id: number;
  articleId: number;
  sku: string;
  articleName: string;
  type: string;
  quantity: number;
  note: string | null;
  createdAt: string;
  createdBy: string;
  saleId: number | null;
  saleNumber: string | null;
};

export async function fetchMovementsPage(page: number, size: number): Promise<PageResponse<MovementDto>> {
  return apiGet<PageResponse<MovementDto>>(`/api/movements?page=${page}&size=${size}`);
}
