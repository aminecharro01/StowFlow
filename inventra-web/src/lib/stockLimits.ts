/** Plafond stock : stock actuel + quantité commandée ≤ max. */

export type StockCapArticle = {
  sku?: string;
  qty?: number;
  maxThreshold?: number;
};

export function maxOrderableQuantity(article: StockCapArticle): number {
  const onHand = article.qty ?? 0;
  const cap = article.maxThreshold;
  if (cap == null) return Number.MAX_SAFE_INTEGER;
  return Math.max(0, cap - onHand);
}

export function assertWithinMaxOrderable(article: StockCapArticle, quantity: number, message: string): void {
  const max = maxOrderableQuantity(article);
  if (quantity > max) {
    throw new Error(message);
  }
}
