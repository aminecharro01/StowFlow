"use client";

import { defaultLandingPath, isRouteAllowed } from "@/lib/rolePrivileges";
import { loadSession } from "@/lib/auth";
import { usePathname, useRouter } from "next/navigation";
import { useLayoutEffect } from "react";

export function RouteRoleGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  useLayoutEffect(() => {
    const role = loadSession()?.role;
    if (!role) return;
    if (!isRouteAllowed(pathname, role)) {
      router.replace(defaultLandingPath(role));
    }
  }, [pathname, router]);

  return <>{children}</>;
}
