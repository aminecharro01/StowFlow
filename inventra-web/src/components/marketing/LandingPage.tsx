"use client";

import "@/styles/clearwave-landing.css";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowRight,
  Check,
  ChevronRight,
  Layers,
  Shield,
  Users,
  X,
  AlertTriangle,
  Sparkles,
} from "lucide-react";
import { Logo } from "@/components/inventra/Logo";
import { LanguageSwitcher } from "@/components/inventra/LanguageSwitcher";
import { LucideByName } from "@/components/inventra/LucideByName";
import { Reveal } from "@/components/marketing/Reveal";
import { loadSession } from "@/lib/auth";
import { useT } from "@/lib/i18n";
import { defaultLandingPath } from "@/lib/rolePrivileges";

const CHART_BARS = [35, 52, 48, 71, 62, 88];
const CHART_ACTIVE = new Set([3, 5, 7]);

const TRUST_ICONS = [Shield, Layers, Users] as const;

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
}

export function LandingPage() {
  const { t } = useT();
  const router = useRouter();
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [openFaq, setOpenFaq] = useState<Set<number>>(new Set());

  useEffect(() => {
    const s = loadSession();
    if (s?.accessToken) {
      router.replace(defaultLandingPath(s.role));
    }
  }, [router]);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    document.body.style.overflow = menuOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [menuOpen]);

  const navItems = [
    { id: "features", label: t.landing.navFeatures },
    { id: "how", label: t.landing.navHowItWorks },
    { id: "faq", label: t.landing.navFaq },
  ];

  const toggleFaq = (i: number) => {
    setOpenFaq((prev) => {
      const next = new Set(prev);
      if (next.has(i)) next.delete(i);
      else next.add(i);
      return next;
    });
  };

  const allFaqOpen = openFaq.size === t.landing.faqItems.length;

  const toggleAllFaq = () => {
    if (allFaqOpen) setOpenFaq(new Set());
    else setOpenFaq(new Set(t.landing.faqItems.map((_, i) => i)));
  };

  const tickerDoubled = [...t.landing.tickerItems, ...t.landing.tickerItems];

  return (
    <div className="landing-cw">
      <div className={`cw-mobile-menu ${menuOpen ? "open" : ""}`} aria-hidden={!menuOpen}>
        {navItems.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => {
              setMenuOpen(false);
              scrollTo(item.id);
            }}
          >
            {item.label}
          </button>
        ))}
        <Link href="/login" className="cw-mobile-cta cw-btn-primary" onClick={() => setMenuOpen(false)}>
          {t.landing.navGetStarted}
        </Link>
      </div>

      <nav className={`cw-nav ${scrolled ? "scrolled" : ""}`}>
        <div className="cw-nav-inner">
          <Logo href="/" size={34} variant="dark" />
          <div className="cw-nav-links">
            {navItems.map((item) => (
              <button key={item.id} type="button" onClick={() => scrollTo(item.id)}>
                {item.label}
              </button>
            ))}
          </div>
          <div className="cw-nav-cta">
            <LanguageSwitcher compact />
            <Link href="/login" className="cw-btn-ghost">
              {t.landing.navLogin}
            </Link>
            <Link href="/login" className="cw-btn-primary">
              {t.landing.navGetStarted}
              <ArrowRight size={14} />
            </Link>
          </div>
          <button
            type="button"
            className={`cw-hamburger ${menuOpen ? "open" : ""}`}
            aria-label="Menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((o) => !o)}
          >
            <span />
            <span />
            <span />
          </button>
        </div>
      </nav>

      <section className="cw-hero">
        <div className="cw-container">
          <div className="cw-hero-inner">
            <div>
              <Reveal>
                <div className="cw-hero-badge">
                  <div className="cw-hero-badge-dot">✦</div>
                  <span>
                    <strong>{t.landing.heroBadgeStrong}</strong> — {t.landing.heroBadge}
                  </span>
                </div>
              </Reveal>
              <Reveal delay={80}>
                <h1 className="cw-hero-title">
                  {t.landing.heroTitle}
                  <br />
                  <em>{t.landing.heroTitleEm}</em>
                </h1>
              </Reveal>
              <Reveal delay={160}>
                <p className="cw-hero-sub">{t.landing.heroSubtitle}</p>
              </Reveal>
              <Reveal delay={240}>
                <div className="cw-hero-actions">
                  <Link href="/login" className="cw-btn-primary-lg">
                    {t.landing.heroCtaPrimary}
                    <span className="btn-arrow">→</span>
                  </Link>
                  <button type="button" className="cw-btn-outline-lg" onClick={() => scrollTo("features")}>
                    <ChevronRight size={16} />
                    {t.landing.heroCtaSecondary}
                  </button>
                </div>
                <p className="cw-hero-note">{t.landing.heroNote}</p>
              </Reveal>
              <Reveal delay={320}>
                <div className="cw-hero-trust">
                  {t.landing.trustItems.map((label, i) => {
                    const Icon = TRUST_ICONS[i] ?? Shield;
                    return (
                      <span key={label} style={{ display: "contents" }}>
                        {i > 0 && <div className="cw-trust-divider" />}
                        <div className="cw-trust-item">
                          <Icon size={16} strokeWidth={2.25} />
                          {label}
                        </div>
                      </span>
                    );
                  })}
                </div>
              </Reveal>
            </div>

            <Reveal delay={120} className="cw-hero-visual">
              <div className="cw-hero-dashboard">
                <div className="cw-dashboard-bar">
                  <div className="cw-db-dot" />
                  <div className="cw-db-dot" />
                  <div className="cw-db-dot" />
                </div>
                <div className="cw-dashboard-body">
                  <div className="cw-db-header">
                    <div className="cw-db-title">{t.landing.previewDashboardTitle}</div>
                    <div className="cw-db-tag">{t.landing.previewLive}</div>
                  </div>
                  <div className="cw-db-chart">
                    {CHART_BARS.map((h, i) => (
                      <div
                        key={i}
                        className={`cw-db-bar ${CHART_ACTIVE.has(i) ? "active" : ""}`}
                        style={{ height: `${h}%` }}
                      />
                    ))}
                  </div>
                  <div className="cw-db-stats">
                    <div className="cw-db-stat">
                      <div className="cw-db-stat-val">
                        24<span style={{ fontSize: "0.9rem", color: "var(--cw-accent)" }}> </span>
                      </div>
                      <div className="cw-db-stat-label">{t.landing.previewStatArticles}</div>
                      <div className="cw-db-stat-change">+2</div>
                    </div>
                    <div className="cw-db-stat">
                      <div className="cw-db-stat-val">
                        7<span style={{ fontSize: "0.9rem", color: "var(--cw-accent)" }}> </span>
                      </div>
                      <div className="cw-db-stat-label">{t.landing.previewStatAlerts}</div>
                      <div className="cw-db-stat-change">{t.landing.previewChartTrend}</div>
                    </div>
                    <div className="cw-db-stat">
                      <div className="cw-db-stat-val">
                        12<span style={{ fontSize: "0.9rem", color: "var(--cw-accent)" }}> </span>
                      </div>
                      <div className="cw-db-stat-label">{t.landing.previewStatSales}</div>
                      <div className="cw-db-stat-change">POS</div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="cw-float-badge">
                <div className="cw-float-icon">
                  <AlertTriangle size={18} />
                </div>
                <div className="cw-float-text">
                  <strong>{t.landing.floatBadgeTitle}</strong>
                  <span>{t.landing.floatBadgeSub}</span>
                </div>
              </div>
              <div className="cw-float-badge-2">
                <div className="cw-float-2-val">{t.landing.floatBadge2Val}</div>
                <div className="cw-float-2-label">{t.landing.floatBadge2Label}</div>
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="cw-ticker" aria-hidden>
        <p className="cw-ticker-label">{t.landing.tickerLabel}</p>
        <div style={{ overflow: "hidden" }}>
          <div className="cw-ticker-track">
            {tickerDoubled.map((name, i) => (
              <span key={`${name}-${i}`} style={{ display: "contents" }}>
                <div className="cw-ticker-item">
                  <Sparkles size={14} style={{ color: "var(--cw-accent-light)" }} />
                  {name}
                </div>
                <div className="cw-ticker-dot" />
              </span>
            ))}
          </div>
        </div>
      </section>

      <section className="cw-compare">
        <div className="cw-container">
          <div className="text-center">
            <Reveal>
              <div className="cw-section-label" style={{ margin: "0 auto 20px" }}>
                {t.landing.problemLabel}
              </div>
              <h2 className="cw-section-title">{t.landing.problemTitle}</h2>
              <p className="cw-section-sub" style={{ margin: "16px auto 0", textAlign: "center" }}>
                {t.landing.problemSubtitle}
              </p>
            </Reveal>
          </div>
          <div className="cw-compare-grid">
            <Reveal delay={80}>
              <div className="cw-compare-card bad">
                <div
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: 14,
                    background: "#fef2f2",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "#ef4444",
                    marginBottom: 16,
                  }}
                >
                  <X size={22} />
                </div>
                <h3 style={{ fontWeight: 700, fontSize: "1.125rem" }}>{t.landing.problemCardTitle}</h3>
                <ul className="cw-compare-list">
                  {t.landing.problemItems.map((item) => (
                    <li key={item}>
                      <X size={14} style={{ color: "#ef4444", flexShrink: 0, marginTop: 4 }} />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            </Reveal>
            <Reveal delay={160}>
              <div className="cw-compare-card good">
                <div className="cw-section-label">{t.landing.solutionLabel}</div>
                <h3 style={{ fontWeight: 700, fontSize: "1.125rem", marginBottom: 4 }}>{t.landing.solutionTitle}</h3>
                <ul className="cw-compare-list">
                  {t.landing.solutionItems.map((item) => (
                    <li key={item} style={{ color: "var(--cw-text-1)" }}>
                      <Check size={14} style={{ color: "#14b8a6", flexShrink: 0, marginTop: 4 }} />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section id="features" className="cw-features scroll-mt-24">
        <div className="cw-container">
          <Reveal className="cw-features-header">
            <div className="cw-section-label">{t.landing.featuresLabel}</div>
            <h2 className="cw-section-title">{t.landing.featuresTitle}</h2>
          </Reveal>
          <div className="cw-features-grid">
            {t.landing.features.map((f, i) => (
              <Reveal key={f.title} delay={i * 60}>
                <div className="cw-feature-card">
                  <div className="cw-feature-icon">
                    <LucideByName name={f.lucide} size={22} strokeWidth={2} />
                  </div>
                  <h3>{f.title}</h3>
                  <p>{f.desc}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="cw-stats">
        <div className="cw-container">
          <div className="cw-stats-grid">
            {t.landing.stats.map((s, i) => (
              <Reveal key={s.label} delay={i * 80}>
                <div className="cw-stat-card">
                  <div className="cw-stat-rule" />
                  <div className="cw-stat-value">
                    {s.value}
                    {s.suffix && <span className="cw-stat-suffix">{s.suffix}</span>}
                  </div>
                  <div className="cw-stat-label">{s.label}</div>
                  <div className="cw-stat-sub">{s.sub}</div>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section id="how" className="cw-how scroll-mt-24">
        <div className="cw-container">
          <div className="text-center">
            <Reveal>
              <div className="cw-section-label" style={{ margin: "0 auto 20px" }}>
                {t.landing.howLabel}
              </div>
              <h2 className="cw-section-title">{t.landing.howTitle}</h2>
            </Reveal>
          </div>
          <div className="cw-how-grid">
            {t.landing.howSteps.map((s, i) => (
              <Reveal key={s.step} delay={i * 100}>
                <div className="cw-how-card">
                  <div className="cw-how-step">{s.step}</div>
                  <h3 style={{ fontWeight: 700, marginBottom: 8 }}>{s.title}</h3>
                  <p style={{ fontSize: "0.875rem", color: "var(--cw-text-2)", lineHeight: 1.65 }}>{s.desc}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section id="faq" className="cw-faq scroll-mt-24">
        <div className="cw-container">
          <div className="cw-faq-inner">
            <Reveal>
              <div className="cw-section-label">{t.landing.navFaq}</div>
              <h2 className="cw-section-title">
                {t.landing.faqTitle}
                <br />
                <em>{t.landing.faqTitleEm}</em>
              </h2>
              <p className="cw-section-sub">{t.landing.faqSubtitle}</p>
              <button
                type="button"
                onClick={toggleAllFaq}
                style={{ marginTop: 24, color: "var(--cw-accent)", fontWeight: 600, fontSize: "0.875rem" }}
              >
                {allFaqOpen ? t.landing.faqCollapseAll : t.landing.faqToggleAll}
              </button>
            </Reveal>
            <Reveal delay={100}>
              <div>
                {t.landing.faqItems.map((item, i) => (
                  <div key={item.q} className={`cw-faq-item ${openFaq.has(i) ? "open" : ""}`}>
                    <button type="button" className="cw-faq-q" onClick={() => toggleFaq(i)} aria-expanded={openFaq.has(i)}>
                      {item.q}
                      <span className="cw-faq-icon">+</span>
                    </button>
                    <div className="cw-faq-a">
                      <div className="cw-faq-a-inner">{item.a}</div>
                    </div>
                  </div>
                ))}
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="cw-cta-wrap">
        <div className="cw-container">
          <Reveal>
            <div className="cw-cta-inner">
              <div>
                <div className="cw-cta-label">{t.landing.ctaLabel}</div>
                <h2 className="cw-cta-title">
                  {t.landing.ctaTitle}
                  <br />
                  <em>{t.landing.ctaTitleEm}</em>
                </h2>
                <p className="cw-cta-sub">{t.landing.ctaSubtitle}</p>
              </div>
              <Link href="/login" className="cw-btn-cta">
                {t.landing.ctaButton}
                <ArrowRight size={16} />
              </Link>
            </div>
          </Reveal>
        </div>
      </section>

      <footer className="cw-footer">
        <div className="cw-container cw-footer-inner">
          <Logo size={28} variant="light" />
          <p className="cw-footer-copy">
            {t.landing.footerTagline} · {t.landing.footerCredit}
          </p>
          <Link href="/login" style={{ color: "var(--cw-accent-light)", fontWeight: 600, fontSize: "0.875rem" }}>
            {t.landing.navLogin}
          </Link>
        </div>
      </footer>
    </div>
  );
}
