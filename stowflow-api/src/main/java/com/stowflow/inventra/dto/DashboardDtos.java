package com.stowflow.inventra.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {

    public record Kpi(
            String title,
            String value,
            String change,
            String label,
            boolean positive,
            String accent,
            /** Nom d’icône Lucide (ex. Package, AlertTriangle) — sérialisé en JSON sous "icon". */
            String icon
    ) {}

    public record QuickStat(String label, String value, String color) {}

    public record BarPoint(String month, int v) {}

    public record DonutSlice(String name, int pct, String color) {}

    public record DashboardPayload(
            List<Kpi> kpis,
            List<QuickStat> quickStats,
            List<BarPoint> movementBars,
            List<DonutSlice> categoryDonut,
            int totalUnits,
            List<AlertDtos.AlertRow> topAlerts
    ) {}

    private DashboardDtos() {}
}
