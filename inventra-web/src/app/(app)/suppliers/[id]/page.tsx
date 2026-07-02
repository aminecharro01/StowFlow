"use client";

import Link from "next/link";
import { Fragment, useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { Card } from "@/components/inventra/ui/Card";
import { Btn } from "@/components/inventra/ui/Btn";
import { Badge } from "@/components/inventra/ui/Badge";
import { TH, TD } from "@/components/inventra/ui/Table";
import { Icons } from "@/components/inventra/Icons";
import { T } from "@/lib/theme";
import { apiGet } from "@/lib/api";
import { useT } from "@/lib/i18n";
import { formatCurrency } from "@/lib/currency";
import { ExternalActionLink } from "@/components/inventra/ExternalActionLink";

type OrderLine = {
  lineId: number;
  sku: string;
  articleName: string;
  quantityOrdered: number;
  quantityReceived: number;
  unitPrice: number;
};

type OrderRow = {
  orderId: number;
  id: string;
  date: string;
  items: number;
  total: string;
  status: string;
  tenantReceived: boolean;
};

type SupplierDetail = {
  id: number;
  name: string;
  contact: string;
  country: string;
  products: number;
  lead: string;
  status: string;
  totalOrders: number;
  pendingOrders: number;
  receivedOrders: number;
  cancelledOrders: number;
  orders: OrderRow[];
};

type OrderDetail = {
  orderId: number;
  id: string;
  date: string;
  total: string;
  status: string;
  tenantReceived: boolean;
  receivedBy: string;
  lines: OrderLine[];
};

export default function SupplierDetailPage() {
  const { t, statusLabel } = useT();
  const params = useParams();
  const id = params?.id as string;
  const [detail, setDetail] = useState<SupplierDetail | null>(null);
  const [expandedOrder, setExpandedOrder] = useState<number | null>(null);
  const [orderDetails, setOrderDetails] = useState<Record<number, OrderDetail>>({});
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!id) return;
    apiGet<SupplierDetail>(`/api/suppliers/${id}`)
      .then(setDetail)
      .catch((e) => {
        setErr((e as Error).message);
        setDetail(null);
      });
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  async function toggleOrder(orderId: number) {
    if (expandedOrder === orderId) {
      setExpandedOrder(null);
      return;
    }
    setExpandedOrder(orderId);
    if (!orderDetails[orderId]) {
      try {
        const d = await apiGet<OrderDetail>(`/api/orders/${orderId}`);
        setOrderDetails((prev) => ({ ...prev, [orderId]: d }));
      } catch {
        /* ignore */
      }
    }
  }

  if (err) {
    return (
      <>
        <Topbar title={t.suppliers.title} subtitle={t.suppliers.subtitle} />
        <Page>
          <p className="text-sm text-red-600 mb-4">{err}</p>
          <Link href="/suppliers">
            <Btn type="button">{t.suppliers.backToList}</Btn>
          </Link>
        </Page>
      </>
    );
  }

  if (!detail) {
    return (
      <>
        <Topbar title={t.suppliers.title} subtitle={t.common.loading} />
        <Page>
          <p className="text-sm" style={{ color: T.text2 }}>
            {t.common.loading}
          </p>
        </Page>
      </>
    );
  }

  const stats = [
    { label: t.suppliers.statOrders, value: String(detail.totalOrders), sub: t.suppliers.statOrdersSub, color: T.primary },
    { label: t.suppliers.statPendingOrders, value: String(detail.pendingOrders), sub: t.suppliers.statPendingOrdersSub, color: T.warning },
    { label: t.suppliers.statReceivedOrders, value: String(detail.receivedOrders), sub: t.suppliers.statReceivedOrdersSub, color: T.success },
  ];

  return (
    <>
      <Topbar title={detail.name} subtitle={t.suppliers.orderHistory} />
      <Page>
        <div className="flex items-center gap-3 mb-4">
          <Link href="/suppliers" className="text-sm hover:underline" style={{ color: T.primary }}>
            ← {t.suppliers.backToList}
          </Link>
        </div>

        <Card className="p-5 mb-4">
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
            <div>
              <h2 className="text-xl font-bold mb-2" style={{ color: T.text }}>
                {detail.name}
              </h2>
              <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm" style={{ color: T.text2 }}>
                {detail.contact && (
                  <span>
                    {t.suppliers.colContact} :{" "}
                    <ExternalActionLink
                      kind="email"
                      value={detail.contact}
                      href={`mailto:${detail.contact}`}
                      className="hover:underline bg-transparent border-0 p-0 cursor-pointer font-inherit inline"
                      style={{ color: T.primary }}
                    >
                      {detail.contact}
                    </ExternalActionLink>
                  </span>
                )}
                {detail.country && <span>{t.suppliers.colCountry} : {detail.country}</span>}
                <span>{t.suppliers.colLead} : {detail.lead}</span>
                <span>{t.suppliers.colProducts} : {detail.products}</span>
              </div>
            </div>
            <Badge status={detail.status} />
          </div>
        </Card>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
          {stats.map((s) => (
            <Card key={s.label} className="p-5 flex flex-col gap-1 overflow-hidden relative">
              <div className="absolute left-0 top-0 bottom-0 w-1 rounded-r-full" style={{ background: s.color }} />
              <p className="text-2xl font-bold pl-3" style={{ color: T.text }}>
                {s.value}
              </p>
              <p className="text-xs font-semibold pl-3" style={{ color: T.text }}>
                {s.label}
              </p>
              <p className="text-xs pl-3" style={{ color: T.text2 }}>
                {s.sub}
              </p>
            </Card>
          ))}
        </div>

        <h3 className="text-base font-semibold mb-3" style={{ color: T.text }}>
          {t.suppliers.orderHistory}
        </h3>

        <Card className="overflow-hidden overflow-x-auto">
          <table className="w-full border-collapse min-w-[720px]">
            <thead>
              <tr>
                <TH>{t.orders.colOrder}</TH>
                <TH>{t.orders.colDate}</TH>
                <TH>{t.orders.colItems}</TH>
                <TH>{t.orders.colTotal}</TH>
                <TH>{t.orders.colStatus}</TH>
                <TH className="text-center">{t.orders.colActions}</TH>
              </tr>
            </thead>
            <tbody>
              {detail.orders.map((o) => (
                <Fragment key={o.orderId}>
                  <tr className="border-t" style={{ borderColor: T.border }}>
                    <TD>
                      <span className="font-mono font-semibold text-xs" style={{ color: T.primary }}>
                        {o.id}
                      </span>
                    </TD>
                    <TD style={{ color: T.text2 }}>{o.date}</TD>
                    <TD>{o.items}</TD>
                    <TD>
                      <span className="font-semibold">{o.total}</span>
                    </TD>
                    <TD>
                      <div className="flex flex-col gap-1">
                        <Badge status={o.status} />
                        {o.tenantReceived && (
                          <span className="text-[10px]" style={{ color: T.primary }}>
                            ✓ {t.orders.tenantReceived}
                          </span>
                        )}
                      </div>
                    </TD>
                    <TD className="text-center">
                      <button
                        type="button"
                        className="text-xs font-semibold px-2 py-1 rounded-lg"
                        style={{ color: T.primary, background: "rgba(37,99,235,0.08)" }}
                        onClick={() => void toggleOrder(o.orderId)}
                      >
                        {expandedOrder === o.orderId ? "Masquer" : "Détails"}
                      </button>
                    </TD>
                  </tr>
                  {expandedOrder === o.orderId && orderDetails[o.orderId] && (
                    <tr className="border-t" style={{ borderColor: T.border, background: "#F9FAFB" }}>
                      <td colSpan={6} className="px-4 py-3">
                        <OrderLinesDetail detail={orderDetails[o.orderId]} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
              {detail.orders.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-sm" style={{ color: T.text2 }}>
                    {t.suppliers.noOrders}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Card>
      </Page>
    </>
  );
}

function OrderLinesDetail({ detail }: { detail: OrderDetail }) {
  const { statusLabel } = useT();
  return (
    <div className="text-sm">
      <p className="font-medium mb-2" style={{ color: T.text }}>
        Lignes de la commande {detail.id} — {statusLabel(detail.status)}
        {detail.receivedBy && (
          <span className="font-normal ml-2 text-xs" style={{ color: T.text2 }}>
            (réception par {detail.receivedBy})
          </span>
        )}
      </p>
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <TH>SKU</TH>
            <TH>Produit</TH>
            <TH>Commandé</TH>
            <TH>Reçu</TH>
            <TH>Prix unit.</TH>
          </tr>
        </thead>
        <tbody>
          {detail.lines.map((l) => (
            <tr key={l.lineId} className="border-t" style={{ borderColor: T.border }}>
              <TD>
                <span className="font-mono text-xs">{l.sku}</span>
              </TD>
              <TD>{l.articleName}</TD>
              <TD>{l.quantityOrdered}</TD>
              <TD>{l.quantityReceived}</TD>
              <TD>{formatCurrency(Number(l.unitPrice))}</TD>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
