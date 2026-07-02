package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.OrderDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.service.PurchaseOrderService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.ORDERS_READ)
    public List<OrderDtos.OrderResponse> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        return purchaseOrderService.listForTenant(tenantService.requireBySlug(tenantSlug), status, q);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventraPolicies.ORDERS_READ)
    public OrderDtos.OrderDetailResponse get(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id
    ) {
        return purchaseOrderService.getForTenant(tenantService.requireBySlug(tenantSlug), id);
    }

    @GetMapping("/summary")
    @PreAuthorize(InventraPolicies.ORDERS_READ)
    public OrderDtos.OrderSummary summary(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug
    ) {
        return purchaseOrderService.summary(tenantService.requireBySlug(tenantSlug));
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.ORDERS_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.OrderDetailResponse create(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @Valid @RequestBody OrderDtos.CreateOrderRequest body
    ) {
        return purchaseOrderService.create(tenantService.requireBySlug(tenantSlug), body);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize(InventraPolicies.ORDERS_WRITE)
    public OrderDtos.OrderResponse ship(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return purchaseOrderService.ship(tenantService.requireBySlug(tenantSlug), id, u.getRole());
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize(InventraPolicies.ORDERS_RECEIVE)
    public OrderDtos.OrderDetailResponse receive(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            @RequestBody(required = false) OrderDtos.ReceiveOrderRequest body,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        OrderDtos.ReceiveOrderRequest req = body != null ? body : new OrderDtos.ReceiveOrderRequest(null);
        return purchaseOrderService.receive(
                tenantService.requireBySlug(tenantSlug),
                id,
                req,
                u.getEmail(),
                u.getRole());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(InventraPolicies.ORDERS_CANCEL)
    public OrderDtos.OrderResponse cancel(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return purchaseOrderService.tenantCancel(tenantService.requireBySlug(tenantSlug), id, u.getRole());
    }
}
