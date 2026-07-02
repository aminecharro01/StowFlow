package com.stowflow.inventra.dto;

import com.stowflow.inventra.domain.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class MovementDtos {

    public record CreateMovementRequest(
            @NotNull Long articleId,
            @NotNull MovementType type,
            @NotNull @Min(1) Integer quantity,
            String note
    ) {}

    public record MovementResponse(
            Long id,
            Long articleId,
            String sku,
            String articleName,
            MovementType type,
            int quantity,
            String note,
            String createdAt,
            String createdBy,
            Long saleId,
            String saleNumber
    ) {}

    private MovementDtos() {}
}
