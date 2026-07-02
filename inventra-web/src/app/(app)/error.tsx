"use client";

import { Btn } from "@/components/inventra/ui/Btn";
import { Card } from "@/components/inventra/ui/Card";
import { Page } from "@/components/inventra/Page";
import { useT } from "@/lib/i18n";
import { T } from "@/lib/theme";

export default function AppError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const { t } = useT();
  return (
    <Page>
      <Card className="p-6 max-w-lg mx-auto mt-12">
        <h2 className="text-lg font-bold mb-2" style={{ color: T.danger }}>
          {t.common.errorTitle}
        </h2>
        <p className="text-sm mb-4" style={{ color: T.text2 }}>
          {error.message || t.common.errorGeneric}
        </p>
        <Btn type="button" onClick={reset}>
          {t.common.retry}
        </Btn>
      </Card>
    </Page>
  );
}
