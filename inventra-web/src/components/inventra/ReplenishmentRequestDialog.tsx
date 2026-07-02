"use client";

import { useEffect, useState } from "react";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { Modal } from "@/components/inventra/ui/Modal";
import { AlertBanner } from "@/components/inventra/ui/AlertBanner";
import { T } from "@/lib/theme";
import { apiGet, apiSend } from "@/lib/api";
import { useT } from "@/lib/i18n";
import type { Article } from "@/app/(app)/products/page";

export type ReplenishmentLine = {
  articleId: number;
  name: string;
  sku: string;
  stock: number;
  reorder: number;
};

type SupplierOption = { id: number; name: string };

type Props = {
  line: ReplenishmentLine;
  onClose: () => void;
  onSuccess: () => void;
};

export function ReplenishmentRequestDialog({ line, onClose, onSuccess }: Props) {
  const { t } = useT();
  const [qty, setQty] = useState("1");
  const [note, setNote] = useState("");
  const [supplierId, setSupplierId] = useState("");
  const [suppliers, setSuppliers] = useState<SupplierOption[]>([]);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    setNote("");
    setErr(null);
    apiGet<SupplierOption[]>("/api/suppliers")
      .then((rows) => {
        setSuppliers(rows.map((s) => ({ id: s.id, name: s.name })));
        if (rows.length > 0) {
          setSupplierId(String(rows[0].id));
        }
      })
      .catch(() => setSuppliers([]));

    apiGet<Article>(`/api/articles/${line.articleId}`)
      .then((a) => {
        const suggested =
          a.suggestedReplenishmentQty ?? Math.max(1, line.reorder - line.stock);
        setQty(String(suggested));
        if (a.replenishmentEligible === false) {
          setErr(
            a.newProduct
              ? "Produit nouveau : utilisez une commande fournisseur."
              : "Stock suffisant : pas de réapprovisionnement nécessaire.",
          );
        }
      })
      .catch(() => setQty(String(Math.max(1, line.reorder - line.stock))));
  }, [line.articleId, line.reorder, line.stock]);

  const submit = async () => {
    const sid = Number(supplierId);
    if (!Number.isFinite(sid) || sid < 1) {
      setErr(t.replenishment.supplierRequired);
      return;
    }
    setErr(null);
    setSaving(true);
    try {
      await apiSend("/api/replenishment-requests", "POST", {
        articleId: line.articleId,
        supplierId: sid,
        note: note.trim() || null,
      });
      onSuccess();
    } catch (e) {
      setErr((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      title={t.replenishment.dialogTitle}
      subtitle={t.replenishment.dialogHint}
      width="480px"
      footer={
        <>
          <Btn type="button" variant="ghost" onClick={onClose}>
            {t.common.cancel}
          </Btn>
          <Btn
            type="button"
            onClick={() => void submit()}
            disabled={saving || suppliers.length === 0 || !!err}
          >
            {saving ? t.common.saving : t.replenishment.submitRequest}
          </Btn>
        </>
      }
    >
      <div className="px-6 py-5">
        <p className="text-sm font-medium mb-4" style={{ color: T.text }}>
          {line.name}{" "}
          <span className="text-xs font-normal" style={{ color: T.text2 }}>
            ({line.sku})
          </span>
        </p>
        {err && (
          <AlertBanner variant="error" className="mb-4">
            {err}
          </AlertBanner>
        )}
        <div className="flex flex-col gap-3">
          <div>
            <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
              {t.replenishment.fieldSupplier}
            </label>
            <select
              value={supplierId}
              onChange={(e) => setSupplierId(e.target.value)}
              className="w-full rounded-lg border text-sm px-3 py-2"
              style={{ borderColor: T.border, background: T.bg, color: T.text }}
            >
              {suppliers.length === 0 && <option value="">{t.replenishment.noSuppliers}</option>}
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-xs font-medium block mb-1" style={{ color: T.text2 }}>
              {t.replenishment.fieldQty}
            </label>
            <Input value={qty} readOnly type="number" min={1} onChange={() => {}} />
            <p className="text-[10px] mt-1" style={{ color: T.text2 }}>
              {t.replenishment.autoQtyHint}
            </p>
          </div>
          <Input value={note} onChange={(e) => setNote(e.target.value)} placeholder={t.replenishment.notePh} />
        </div>
      </div>
    </Modal>
  );
}
