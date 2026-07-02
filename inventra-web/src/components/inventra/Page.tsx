import { T } from "@/lib/theme";

export function Page({ children }: { children: React.ReactNode }) {
  return (
    <main className="ml-60 pt-16 min-h-screen" style={{ background: T.bg }}>
      <div className="p-6 flex flex-col gap-5">{children}</div>
    </main>
  );
}
