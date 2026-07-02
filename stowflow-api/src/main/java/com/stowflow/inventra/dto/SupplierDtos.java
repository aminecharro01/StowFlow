package com.stowflow.inventra.dto;

import com.stowflow.inventra.domain.SupplierStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class SupplierDtos {

    public record SupplierResponse(
            Long id,
            String name,
            String contact,
            String country,
            int products,
            String lead,
            String status
    ) {}

    public record SupplierDetailResponse(
            Long id,
            String name,
            String contact,
            String country,
            int products,
            String lead,
            String status,
            long totalOrders,
            long pendingOrders,
            long receivedOrders,
            long cancelledOrders,
            List<com.stowflow.inventra.dto.OrderDtos.OrderResponse> orders
    ) {}

    public record CreateSupplierRequest(
            @NotBlank String name,
            String contactEmail,
            String country,
            @NotNull @Min(0) Integer leadTimeDays,
            SupplierStatus status
    ) {}

    public record UpdateSupplierRequest(
            @NotBlank String name,
            String contactEmail,
            String country,
            @NotNull @Min(0) Integer leadTimeDays,
            @NotNull SupplierStatus status
    ) {}

    private SupplierDtos() {}
}
