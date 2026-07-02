package com.stowflow.inventra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class PosDtos {

    public record PosSaleLineRequest(
            @NotNull Long articleId,
            @NotNull @Min(1) Integer quantity
    ) {}

    public record PosSaleRequest(
            @NotEmpty @Valid List<PosSaleLineRequest> lines,
            String note
    ) {}

    public record PosSaleLineResponse(
            long articleId,
            String sku,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {}

    public record PosSaleResponse(
            long id,
            String saleNumber,
            BigDecimal total,
            String createdAt,
            String soldByEmail,
            List<PosSaleLineResponse> lines,
            String note
    ) {}

    public record SaleListItem(
            long id,
            String saleNumber,
            String soldByEmail,
            String createdAt,
            BigDecimal total,
            int lineCount
    ) {}

    public record SellerOption(String email) {}

    public record PagedSales(
            List<SaleListItem> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {}

    private PosDtos() {}
}
