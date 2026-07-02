"use client";

import { Topbar } from "@/components/inventra/Topbar";
import { Page } from "@/components/inventra/Page";
import { SettingsUsersPanel } from "@/components/inventra/SettingsUsersPanel";
import { useT } from "@/lib/i18n";

export default function SettingsPage() {
  const { t } = useT();
  return (
    <>
      <Topbar title={t.settings.title} subtitle={t.settings.subtitle} />
      <Page>
        <SettingsUsersPanel />
      </Page>
    </>
  );
}
