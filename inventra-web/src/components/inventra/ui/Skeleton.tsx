import { T } from "@/lib/theme";

export function Skeleton({ className = "" }: { className?: string }) {
  return (
    <div
      className={`animate-pulse rounded-lg ${className}`}
      style={{ background: "#E5E7EB" }}
      aria-hidden
    />
  );
}

export function PageSkeleton() {
  return (
    <div className="flex flex-col gap-5 p-6 ml-60 pt-20">
      <Skeleton className="h-8 w-64" />
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="rounded-xl border p-5 flex flex-col gap-3" style={{ borderColor: T.border, background: T.surface }}>
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-4 w-20" />
          </div>
        ))}
      </div>
      <Skeleton className="h-48 w-full rounded-xl" />
    </div>
  );
}
