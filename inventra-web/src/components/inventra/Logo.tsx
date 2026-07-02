"use client";

import Image from "next/image";
import Link from "next/link";

type LogoProps = {
  /** Afficher le texte « StowFlow » à côté de l’icône */
  showText?: boolean;
  /** Taille de l’icône en pixels */
  size?: number;
  /** Lien cliquable vers le tableau de bord */
  href?: string;
  className?: string;
  /** Variante pour fond sombre (sidebar, login) ou clair */
  variant?: "light" | "dark";
};

export function Logo({
  showText = true,
  size = 32,
  href,
  className = "",
  variant = "light",
}: LogoProps) {
  const textColor = variant === "light" ? "#fff" : "#111827";

  const content = (
    <div className={`flex items-center gap-3 ${className}`}>
      <div
        className="relative flex-shrink-0 rounded-xl overflow-hidden bg-white"
        style={{ width: size, height: size }}
      >
        <Image
          src="/stowflow-logo.png"
          alt="StowFlow"
          fill
          className="object-contain p-0.5"
          sizes={`${size}px`}
          priority
        />
      </div>
      {showText && (
        <span
          className="font-bold tracking-tight"
          style={{ color: textColor, fontSize: size >= 40 ? "1.25rem" : "1.125rem" }}
        >
          StowFlow
        </span>
      )}
    </div>
  );

  if (href) {
    return (
      <Link href={href} className="inline-flex no-underline hover:opacity-90 transition-opacity">
        {content}
      </Link>
    );
  }

  return content;
}
