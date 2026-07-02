"use client";

import { useConfirm } from "@/components/inventra/ui/useConfirm";
import { useT } from "@/lib/i18n";

type ExternalActionLinkProps = {
  kind: "phone" | "email";
  value: string;
  href: string;
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
};

export function ExternalActionLink({
  kind,
  value,
  href,
  children,
  className = "",
  style,
}: ExternalActionLinkProps) {
  const { t } = useT();
  const { confirm, dialog } = useConfirm();

  async function handleClick(e: React.MouseEvent) {
    e.preventDefault();
    const cfg =
      kind === "phone"
        ? {
            title: t.confirm.openPhone.title,
            message: t.confirm.openPhone.message,
            confirmLabel: t.confirm.openPhone.confirm,
            variant: "phone" as const,
            detail: value,
          }
        : {
            title: t.confirm.openEmail.title,
            message: t.confirm.openEmail.message,
            confirmLabel: t.confirm.openEmail.confirm,
            variant: "email" as const,
            detail: value,
          };

    const ok = await confirm(cfg);
    if (ok) {
      window.location.href = href;
    }
  }

  return (
    <>
      {dialog}
      <button type="button" onClick={(e) => void handleClick(e)} className={className} style={style}>
        {children}
      </button>
    </>
  );
}
