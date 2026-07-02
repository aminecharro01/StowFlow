"use client";

export function MeshBackground({ variant = "hero" }: { variant?: "hero" | "login" }) {
  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden>
      <div
        className="absolute inset-0"
        style={{
          background:
            variant === "hero"
              ? "linear-gradient(165deg, #020617 0%, #0F172A 30%, #1E3A8A 55%, #1D4ED8 100%)"
              : "linear-gradient(145deg, #020617 0%, #0F172A 40%, #1E3A8A 70%, #172554 100%)",
        }}
      />
      <div className="absolute w-[520px] h-[520px] rounded-full blur-3xl opacity-40 animate-orb-1 -top-32 -left-32" style={{ background: "#2563EB" }} />
      <div className="absolute w-[420px] h-[420px] rounded-full blur-3xl opacity-35 animate-orb-2 top-1/4 -right-24" style={{ background: "#14B8A6" }} />
      <div className="absolute w-[360px] h-[360px] rounded-full blur-3xl opacity-25 animate-orb-3 bottom-0 left-1/3" style={{ background: "#6366F1" }} />
      <div
        className="absolute inset-0 opacity-[0.035]"
        style={{
          backgroundImage:
            "linear-gradient(rgba(255,255,255,0.8) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.8) 1px, transparent 1px)",
          backgroundSize: "64px 64px",
        }}
      />
    </div>
  );
}
