package com.stowflow.inventra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ArticleDtos {

    public record ArticleResponse(
            Long id,
            String name,
            String sku,
            String category,
            int qty,
            BigDecimal price,
            String status,
            int minThreshold,
            int maxThreshold,
            String description,
            boolean newProduct,
            boolean purchaseOrderEligible,
            boolean replenishmentEligible,
            int suggestedPurchaseOrderQty,
            int suggestedReplenishmentQty
    ) {}

    public record CreateArticleRequest(
            @NotBlank String name,
            @NotBlank String sku,
            String category,
            @NotNull @Min(0) BigDecimal salePrice,
            @NotNull @Min(0) Integer minThreshold,
            @NotNull Integer maxThreshold,
            BigDecimal purchasePrice,
            String unitOfMeasure,
            String description
    ) {}

    public record UpdateArticleRequest(
            String name,
            String category,
            BigDecimal salePrice,
            BigDecimal purchasePrice,
            Integer minThreshold,
            Integer maxThreshold,
            String description
    ) {}

    private ArticleDtos() {}
}
