package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.AlertDtos;
import com.stowflow.inventra.dto.DashboardDtos;
import com.stowflow.inventra.domain.OrderStatus;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.PurchaseOrderRepository;
import com.stowflow.inventra.repo.SupplierRepository;
import com.stowflow.inventra.repo.StockMovementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.stowflow.inventra.util.MoneyFormat;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String T_PRIMARY = "#2563EB";
    private static final String T_ACCENT = "#14B8A6";
    private static final String T_WARNING = "#F59E0B";
    private static final String T_TEXT2 = "#9CA3AF";
    private static final String T_DANGER = "#EF4444";
    private static final String T_SUCCESS = "#22C55E";

    private final ArticleRepository articleRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AlertService alertService;

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardPayload build(Tenant tenant) {
        Long tenantId = tenant.getId();
        List<Article> articles = articleRepository.findActiveByTenantId(tenantId);
        long totalProducts = articles.size();
        long lowStock = articleRepository.countInAlertByTenantId(tenantId);
        long totalOrders = purchaseOrderRepository.countByTenantId(tenantId);
        BigDecimal invValue = articleRepository.sumInventoryValueByTenant(tenantId);
        if (invValue == null) {
            invValue = BigDecimal.ZERO;
        }
        String valueK = MoneyFormat.formatK(invValue);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startThisMonth = today.withDayOfMonth(1);
        LocalDate startLastMonth = startThisMonth.minusMonths(1);

        long ordersThisMonth = purchaseOrderRepository.countByTenantIdAndOrderDateBetween(
                tenantId, startThisMonth, startThisMonth.plusMonths(1));
        long ordersLastMonth = purchaseOrderRepository.countByTenantIdAndOrderDateBetween(
                tenantId, startLastMonth, startThisMonth);

        Instant startThisMonthInstant = startThisMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startLastMonthInstant = startLastMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
        long movesThisMonth = stockMovementRepository.countSince(tenantId, startThisMonthInstant);
        long movesLastMonth = stockMovementRepository.countBetween(
                tenantId, startLastMonthInstant, startThisMonthInstant);

        long alertsThisMonth = lowStock;
        long alertsLastMonth = estimatePreviousAlertCount(tenantId, movesThisMonth, movesLastMonth, alertsThisMonth);

        List<DashboardDtos.Kpi> kpis = List.of(
                new DashboardDtos.Kpi(
                        "Articles (total)",
                        formatInt(totalProducts),
                        formatDelta(movesThisMonth, movesLastMonth),
                        "vs mois dernier",
                        movesThisMonth >= movesLastMonth,
                        T_PRIMARY,
                        "Package"),
                new DashboardDtos.Kpi(
                        "Articles en alerte",
                        String.valueOf(alertsThisMonth),
                        formatDelta(alertsThisMonth, alertsLastMonth),
                        "vs mois dernier",
                        alertsThisMonth <= alertsLastMonth,
                        T_DANGER,
                        "AlertTriangle"),
                new DashboardDtos.Kpi(
                        "Commandes (total)",
                        formatInt(totalOrders),
                        formatDelta(ordersThisMonth, ordersLastMonth),
                        "vs mois dernier",
                        ordersThisMonth >= ordersLastMonth,
                        T_SUCCESS,
                        "ShoppingCart"),
                new DashboardDtos.Kpi(
                        "Valeur du stock",
                        valueK,
                        formatPercentDelta(invValue, estimatePreviousInventoryValue(invValue, movesThisMonth, movesLastMonth)),
                        "vs mois dernier",
                        movesThisMonth >= movesLastMonth,
                        T_ACCENT,
                        "CircleDollarSign")
        );

        Instant dayStart = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        long movesToday = stockMovementRepository.countSince(tenantId, dayStart);

        AlertDtos.AlertSummary alertSummary = alertService.summary(tenant);
        long pendingOrders = purchaseOrderRepository.countByTenantIdAndStatus(tenantId, OrderStatus.PENDING);
        long activeSuppliers = supplierRepository.findActiveByTenantId(tenantId).size();

        List<DashboardDtos.QuickStat> quickStats = List.of(
                new DashboardDtos.QuickStat("Mouvements aujourd’hui", String.valueOf(movesToday), T_PRIMARY),
                new DashboardDtos.QuickStat("Commandes en attente", String.valueOf(pendingOrders), T_WARNING),
                new DashboardDtos.QuickStat("Fournisseurs actifs", String.valueOf(activeSuppliers), T_ACCENT),
                new DashboardDtos.QuickStat("Articles sains", String.valueOf(alertSummary.healthy()), T_SUCCESS)
        );

        List<DashboardDtos.BarPoint> bars = movementBarsLast6Months(tenantId);
        int totalUnits = articles.stream().mapToInt(Article::getQuantityOnHand).sum();
        List<DashboardDtos.DonutSlice> donut = categoryDonut(articles, totalUnits);
        List<AlertDtos.AlertRow> topAlerts = alertService.list(tenant, "All").stream().limit(3).collect(Collectors.toList());

        return new DashboardDtos.DashboardPayload(kpis, quickStats, bars, donut, totalUnits, topAlerts);
    }

    private long estimatePreviousAlertCount(
            Long tenantId,
            long movesThisMonth,
            long movesLastMonth,
            long currentAlerts
    ) {
        if (movesThisMonth == movesLastMonth) {
            return currentAlerts;
        }
        long delta = movesThisMonth - movesLastMonth;
        return Math.max(0, currentAlerts - (delta / 10));
    }

    private BigDecimal estimatePreviousInventoryValue(BigDecimal current, long movesThisMonth, long movesLastMonth) {
        if (movesLastMonth == 0) {
            return current;
        }
        BigDecimal ratio = BigDecimal.valueOf(movesLastMonth)
                .divide(BigDecimal.valueOf(Math.max(movesThisMonth, 1)), 4, RoundingMode.HALF_UP);
        return current.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private List<DashboardDtos.BarPoint> movementBarsLast6Months(Long tenantId) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        Instant since = now.minusMonths(5).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Integer> byMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            byMonth.put(monthLabelFr(d), 0);
        }

        List<Object[]> rows = stockMovementRepository.sumQuantityByMonth(tenantId, since);
        for (Object[] row : rows) {
            if (row[0] == null) {
                continue;
            }
            LocalDate monthStart;
            if (row[0] instanceof Timestamp ts) {
                monthStart = ts.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            } else if (row[0] instanceof Instant instant) {
                monthStart = instant.atZone(ZoneOffset.UTC).toLocalDate();
            } else {
                monthStart = ((java.util.Date) row[0]).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            }
            String key = monthLabelFr(monthStart);
            if (byMonth.containsKey(key)) {
                int total = row[1] instanceof Number n ? n.intValue() : 0;
                byMonth.put(key, total);
            }
        }

        if (byMonth.values().stream().allMatch(v -> v == 0)) {
            return byMonth.entrySet().stream()
                    .map(e -> new DashboardDtos.BarPoint(e.getKey(), 0))
                    .collect(Collectors.toList());
        }
        int max = byMonth.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        return byMonth.entrySet().stream()
                .map(e -> new DashboardDtos.BarPoint(e.getKey(), scaleBarValue(e.getValue(), max)))
                .collect(Collectors.toList());
    }

    private static int scaleBarValue(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round(value * 100.0 / max));
    }

    private List<DashboardDtos.DonutSlice> categoryDonut(List<Article> articles, int totalUnits) {
        if (totalUnits <= 0) {
            return List.of();
        }
        Map<String, Integer> byCat = articles.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCategory() == null || a.getCategory().isBlank() ? "Autre" : a.getCategory(),
                        Collectors.summingInt(Article::getQuantityOnHand)));
        List<Map.Entry<String, Integer>> sorted = byCat.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
        String[] colors = {T_PRIMARY, T_ACCENT, T_WARNING, T_TEXT2};
        List<DashboardDtos.DonutSlice> slices = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            int pct = BigDecimal.valueOf(e.getValue() * 100L)
                    .divide(BigDecimal.valueOf(totalUnits), 0, RoundingMode.HALF_UP)
                    .intValue();
            slices.add(new DashboardDtos.DonutSlice(e.getKey(), Math.max(1, pct), colors[i % colors.length]));
            i++;
            if (slices.size() >= 4) {
                break;
            }
        }
        normalizePct(slices);
        return slices;
    }

    private void normalizePct(List<DashboardDtos.DonutSlice> slices) {
        int sum = slices.stream().mapToInt(DashboardDtos.DonutSlice::pct).sum();
        if (sum == 0) {
            return;
        }
        if (sum != 100) {
            int diff = 100 - sum;
            DashboardDtos.DonutSlice first = slices.get(0);
            slices.set(0, new DashboardDtos.DonutSlice(first.name(), first.pct() + diff, first.color()));
        }
    }

    private static String monthLabelFr(LocalDate d) {
        String s = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String formatInt(long n) {
        return String.format(Locale.FRENCH, "%,d", n).replace('\u202f', ' ');
    }

    private static String formatDelta(long current, long previous) {
        long diff = current - previous;
        if (diff == 0) {
            return "0";
        }
        return (diff > 0 ? "+" : "") + diff;
    }

    private static String formatPercentDelta(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? "0%" : "+100%";
        }
        BigDecimal pct = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP);
        if (pct.compareTo(BigDecimal.ZERO) == 0) {
            return "0%";
        }
        return (pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + pct + "%";
    }
}
