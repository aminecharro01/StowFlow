const amountFormatter = new Intl.NumberFormat("fr-FR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Formats an amount in Moroccan dirhams (DH). */
export function formatCurrency(value: number): string {
  const n = Number.isFinite(value) ? value : Number(value);
  if (!Number.isFinite(n)) return "— DH";
  return `${amountFormatter.format(n)} DH`;
}
