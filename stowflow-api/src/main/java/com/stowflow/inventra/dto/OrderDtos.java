package com.stowflow.inventra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {

    public record OrderLineResponse(
            Long lineId,
            Long articleId,
            String sku,
            String articleName,
            int quantityOrdered,
            int quantityReceived,
            BigDecimal unitPrice
    ) {}

    public record OrderResponse(
            Long orderId,
            String id,
            String supplier,
            Long supplierId,
            String date,
            int items,
            String total,
            String status,
            boolean tenantReceived
    ) {}

    public record OrderDetailResponse(
            Long orderId,
            String id,
            String supplier,
            Long supplierId,
            String date,
            String total,
            String status,
            boolean tenantReceived,
            String receivedBy,
            List<OrderLineResponse> lines
    ) {}

    public record CreateOrderLineRequest(
            @NotNull Long articleId,
            @NotNull @Min(1) Integer quantity,
            @NotNull @Min(0) BigDecimal unitPrice
    ) {}

    public record CreateOrderRequest(
            @NotNull Long supplierId,
            @NotNull LocalDate orderDate,
            @NotEmpty @Valid List<CreateOrderLineRequest> lines
    ) {}

    public record ReceiveOrderLineRequest(@NotNull Long lineId, @NotNull @Min(0) Integer quantityReceived) {}

    public record ReceiveOrderRequest(List<ReceiveOrderLineRequest> lines) {}

    public record OrderSummary(
            long totalOrders,
            long pending,
            long shipped,
            long received,
            long cancelled
    ) {}

    private OrderDtos() {}
}
