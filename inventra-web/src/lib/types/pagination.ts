export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

/** Charge toutes les pages (max 100 éléments par page côté API). */
export async function fetchAllPages<T>(
  fetchPage: (page: number, size: number) => Promise<PageResponse<T>>,
  pageSize = 100
): Promise<T[]> {
  const first = await fetchPage(0, pageSize);
  if (first.totalPages <= 1) return first.content;
  const rest = await Promise.all(
    Array.from({ length: first.totalPages - 1 }, (_, i) => fetchPage(i + 1, pageSize))
  );
  return [...first.content, ...rest.flatMap((p) => p.content)];
}
