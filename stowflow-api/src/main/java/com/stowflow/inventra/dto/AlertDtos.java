package com.stowflow.inventra.dto;

public final class AlertDtos {

    public record AlertRow(
            Long articleId,
            String name,
            String sku,
            String category,
            int stock,
            int reorder,
            String status,
            boolean purchaseOrderEligible,
            boolean replenishmentEligible
    ) {}

    public record AlertSummary(
            long outOfStock,
            long critical,
            long low,
            long healthy
    ) {}

    private AlertDtos() {}
}
