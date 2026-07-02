"use client";

import { Card } from "@/components/inventra/ui/Card";
import { ExternalActionLink } from "@/components/inventra/ExternalActionLink";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { Clock, Headphones, Mail, Phone, MessageCircle, CheckCircle2 } from "lucide-react";

function ContactCard({
  kind,
  icon: Icon,
  label,
  value,
  href,
  accent,
}: {
  kind: "phone" | "email";
  icon: typeof Phone;
  label: string;
  value: string;
  href: string;
  accent: string;
}) {
  return (
    <ExternalActionLink
      kind={kind}
      value={value}
      href={href}
      className="flex items-start gap-4 p-5 rounded-xl border transition-all duration-150 hover:shadow-md hover:-translate-y-0.5 text-left w-full cursor-pointer"
      style={{ borderColor: T.border, background: T.surface }}
    >
      <div
        className="w-11 h-11 rounded-xl flex items-center justify-center shrink-0"
        style={{ background: `${accent}18`, color: accent }}
      >
        <Icon size={22} strokeWidth={2} />
      </div>
      <div className="min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wide mb-1" style={{ color: T.text2 }}>
          {label}
        </p>
        <p className="text-base font-semibold break-all" style={{ color: T.text }}>
          {value}
        </p>
      </div>
    </ExternalActionLink>
  );
}

export function HelpSupportSection() {
  const { t } = useT();
  return (
    <div className="max-w-3xl flex flex-col gap-5">
      <Card className="p-6">
        <div className="flex items-start gap-4">
          <div
            className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
            style={{ background: "#EFF6FF", color: T.primary }}
          >
            <Headphones size={24} strokeWidth={2} />
          </div>
          <div>
            <h2 className="text-lg font-bold mb-2" style={{ color: T.text }}>
              {t.help.introTitle}
            </h2>
            <p className="text-sm leading-relaxed" style={{ color: T.text2 }}>
              {t.help.introText}
            </p>
          </div>
        </div>
      </Card>

      <Card
        className="p-5 flex items-center gap-4"
        style={{ background: "linear-gradient(135deg, #EFF6FF 0%, #ECFDF5 100%)" }}
      >
        <div
          className="w-11 h-11 rounded-xl flex items-center justify-center shrink-0"
          style={{ background: T.primary, color: "#fff" }}
        >
          <Clock size={22} strokeWidth={2} />
        </div>
        <div>
          <p className="text-sm font-bold" style={{ color: T.text }}>
            {t.help.availabilityTitle}
          </p>
          <p className="text-sm mt-0.5" style={{ color: T.text2 }}>
            {t.help.availabilityText}
          </p>
        </div>
      </Card>

      <div>
        <h3 className="text-sm font-bold mb-1 px-1" style={{ color: T.text }}>
          {t.help.contactTitle}
        </h3>
        <p className="text-sm mb-3 px-1" style={{ color: T.text2 }}>
          {t.help.contactName}
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <ContactCard
            kind="phone"
            icon={Phone}
            label={t.help.phoneLabel}
            value={t.help.phoneValue}
            href={t.help.phoneHref}
            accent={T.primary}
          />
          <ContactCard
            kind="email"
            icon={Mail}
            label={t.help.mailLabel}
            value={t.help.mailValue}
            href={t.help.mailHref}
            accent={T.accent}
          />
        </div>
        <p className="flex items-center gap-2 text-xs mt-3 px-1" style={{ color: T.text2 }}>
          <MessageCircle size={14} strokeWidth={2} style={{ color: T.success }} />
          {t.help.responseHint}
        </p>
      </div>

      <Card className="p-6">
        <h3 className="text-sm font-bold mb-4" style={{ color: T.text }}>
          {t.help.tipsTitle}
        </h3>
        <ul className="flex flex-col gap-3">
          {t.help.tips.map((tip) => (
            <li key={tip} className="flex items-start gap-3 text-sm leading-relaxed" style={{ color: T.text2 }}>
              <CheckCircle2
                size={18}
                strokeWidth={2}
                className="shrink-0 mt-0.5"
                style={{ color: T.success }}
              />
              {tip}
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
