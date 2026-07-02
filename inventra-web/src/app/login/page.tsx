"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ArrowLeft, ArrowRight, Check, Lock, Mail, Sparkles } from "lucide-react";
import { Btn } from "@/components/inventra/ui/Btn";
import { Input } from "@/components/inventra/ui/Input";
import { Logo } from "@/components/inventra/Logo";
import { LanguageSwitcher } from "@/components/inventra/LanguageSwitcher";
import { MeshBackground } from "@/components/marketing/MeshBackground";
import { remoteLogin, saveSession, loadSession } from "@/lib/auth";
import { defaultLandingPath } from "@/lib/rolePrivileges";
import { useT } from "@/lib/i18n";
import { T } from "@/lib/theme";

const DEMO_ACCOUNTS = [
  { email: "stock@default.demo", role: "Stock" },
  { email: "admin@default.demo", role: "Admin" },
  { email: "sales@default.demo", role: "Sales" },
];

export default function LoginPage() {
  const { t } = useT();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [shake, setShake] = useState(false);

  useEffect(() => {
    const s = loadSession();
    if (s?.accessToken) {
      router.replace(defaultLandingPath(s.role));
    }
  }, [router]);

  function fillDemo(demoEmail: string) {
    setEmail(demoEmail);
    setPassword("password");
    setError(null);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      const session = await remoteLogin(email, password);
      saveSession(session);
      router.replace(defaultLandingPath(session.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : t.login.errorGeneric);
      setShake(true);
      window.setTimeout(() => setShake(false), 500);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2">
      {/* Brand panel */}
      <div className="relative hidden lg:flex flex-col justify-between p-10 xl:p-14 overflow-hidden">
        <MeshBackground variant="login" />
        <div className="relative z-10">
          <Logo href="/" size={40} variant="light" />
        </div>
        <div className="relative z-10 max-w-md animate-hero-in">
          <span className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full mb-6 badge-shimmer border border-white/10 text-teal-200">
            <Sparkles size={12} />
            StowFlow
          </span>
          <h1 className="text-3xl xl:text-4xl font-bold text-white leading-tight tracking-tight">
            {t.login.panelTitle}
          </h1>
          <p className="mt-4 text-base leading-relaxed text-blue-100/80">{t.login.panelSubtitle}</p>
          <ul className="mt-8 flex flex-col gap-4">
            {t.login.panelBullets.map((bullet, i) => (
              <li
                key={bullet}
                className="flex items-center gap-3 text-sm text-white/90 animate-hero-in"
                style={{ animationDelay: `${0.2 + i * 0.1}s` }}
              >
                <span className="w-6 h-6 rounded-lg bg-white/10 flex items-center justify-center shrink-0 border border-white/10">
                  <Check size={14} className="text-teal-300" />
                </span>
                {bullet}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative z-10 text-xs text-white/40">© StowFlow · Amine Charro</p>
      </div>

      {/* Form panel */}
      <div className="flex flex-col min-h-screen bg-[#F8FAFC] lg:bg-white">
        <div className="flex items-center justify-between p-4 sm:p-6">
          <Link
            href="/"
            className="inline-flex items-center gap-1.5 text-sm font-medium rounded-lg px-3 py-2 hover:bg-gray-100 transition-colors"
            style={{ color: T.text2 }}
          >
            <ArrowLeft size={16} />
            {t.login.backHome}
          </Link>
          <LanguageSwitcher />
        </div>

        <div className="flex-1 flex items-center justify-center px-4 sm:px-8 pb-10">
          <div className={`w-full max-w-[420px] animate-login-in ${shake ? "animate-shake" : ""}`}>
            <div className="lg:hidden mb-8 text-center">
              <Logo href="/" size={40} variant="dark" className="justify-center" />
            </div>

            <div
              className="rounded-3xl border p-8 sm:p-9 shadow-xl shadow-slate-200/60"
              style={{ borderColor: T.border, background: T.surface }}
            >
              <h2 className="text-2xl font-bold tracking-tight" style={{ color: T.text }}>
                {t.login.title}
              </h2>
              <p className="text-sm mt-2 mb-7 leading-relaxed" style={{ color: T.text2 }}>
                {t.login.subtitle}
              </p>

              <form onSubmit={onSubmit} className="flex flex-col gap-4">
                <div>
                  <label className="text-xs font-semibold mb-1.5 block" style={{ color: T.text2 }}>
                    {t.login.emailPh}
                  </label>
                  <Input
                    type="email"
                    autoComplete="username"
                    placeholder="you@company.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    icon={<Mail size={16} />}
                    required
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold mb-1.5 block" style={{ color: T.text2 }}>
                    {t.login.passwordPh}
                  </label>
                  <Input
                    type="password"
                    autoComplete="current-password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    icon={<Lock size={16} />}
                    required
                  />
                </div>

                {error && (
                  <div
                    className="text-sm rounded-xl px-4 py-3 border flex items-start gap-2"
                    style={{ background: "#FEF2F2", color: T.danger, borderColor: "#FECACA" }}
                  >
                    <span className="font-semibold shrink-0">!</span>
                    {error}
                  </div>
                )}

                <Btn
                  type="submit"
                  size="lg"
                  className="w-full justify-center mt-1 shadow-lg shadow-blue-500/20 group"
                  disabled={pending}
                >
                  {pending ? t.login.submitting : t.login.submit}
                  {!pending && <ArrowRight size={16} className="group-hover:translate-x-0.5 transition-transform" />}
                </Btn>
              </form>

              <div className="mt-8 pt-6 border-t" style={{ borderColor: T.border }}>
                <p className="text-xs font-bold uppercase tracking-wide mb-1" style={{ color: T.text }}>
                  {t.login.demoTitle}
                </p>
                <p className="text-xs mb-3" style={{ color: T.text2 }}>
                  {t.login.demoHint} · {t.login.demoPassword}
                </p>
                <div className="flex flex-wrap gap-2">
                  {DEMO_ACCOUNTS.map((acc) => (
                    <button
                      key={acc.email}
                      type="button"
                      onClick={() => fillDemo(acc.email)}
                      className="text-xs font-semibold px-3 py-2 rounded-xl border transition-all hover:scale-[1.02] hover:shadow-md active:scale-[0.98]"
                      style={{
                        borderColor: T.border,
                        color: T.primary,
                        background: "#EFF6FF",
                      }}
                    >
                      {acc.role}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
