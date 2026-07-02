"use client";

import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { HelpSupportSection } from "@/components/inventra/HelpSupportSection";
import { useT } from "@/lib/i18n";

export default function HelpPage() {
  const { t } = useT();
  return (
    <>
      <Topbar title={t.help.title} subtitle={t.help.subtitle} />
      <Page>
        <HelpSupportSection />
      </Page>
    </>
  );
}
