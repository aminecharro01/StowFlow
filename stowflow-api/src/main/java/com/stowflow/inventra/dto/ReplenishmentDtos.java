package com.stowflow.inventra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReplenishmentDtos {

    public record ReplenishmentRow(
            Long id,
            String reference,
            Long articleId,
            String articleName,
            String articleSku,
            Long supplierId,
            String supplierName,
            int quantity,
            String status,
            String note,
            String requestedBy,
            String createdAt,
            String processedBy,
            String receivedBy
    ) {}

    public record ReplenishmentSummary(long pending, long inProgress, long received) {}

    public record CreateReplenishmentRequest(
            @NotNull Long articleId,
            @NotNull Long supplierId,
            /** Ignorée si absente — calcul automatique selon seuils stock. */
            @Min(1) Integer quantity,
            @Size(max = 512) String note
    ) {}

    public record UpdateReplenishmentStatusRequest(@NotBlank String status) {}

    private ReplenishmentDtos() {}
}
