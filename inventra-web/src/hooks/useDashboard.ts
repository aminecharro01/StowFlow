"use client";

import { apiGet } from "@/lib/api";
import type { DashboardPayload } from "@/lib/types/dashboard";
import { useQuery } from "@tanstack/react-query";

export function useDashboard() {
  return useQuery({
    queryKey: ["dashboard"],
    queryFn: () => apiGet<DashboardPayload>("/api/dashboard"),
  });
}
