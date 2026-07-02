package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.ReplenishmentDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.service.ReplenishmentRequestService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/replenishment-requests")
@RequiredArgsConstructor
public class ReplenishmentRequestController {

    private final ReplenishmentRequestService replenishmentRequestService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.REPLENISHMENT_READ)
    public List<ReplenishmentDtos.ReplenishmentRow> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(required = false) String status
    ) {
        return replenishmentRequestService.list(tenantService.requireBySlug(tenantSlug), status);
    }

    @GetMapping("/summary")
    @PreAuthorize(InventraPolicies.REPLENISHMENT_READ)
    public ReplenishmentDtos.ReplenishmentSummary summary(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug
    ) {
        return replenishmentRequestService.summary(tenantService.requireBySlug(tenantSlug));
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.REPLENISHMENT_CREATE)
    public ReplenishmentDtos.ReplenishmentRow create(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @Valid @RequestBody ReplenishmentDtos.CreateReplenishmentRequest body,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return replenishmentRequestService.create(
                tenantService.requireBySlug(tenantSlug), body, u.getUsername(), u.getRole());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(InventraPolicies.REPLENISHMENT_PROCESS)
    public ReplenishmentDtos.ReplenishmentRow patchStatus(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            @Valid @RequestBody ReplenishmentDtos.UpdateReplenishmentStatusRequest body,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return replenishmentRequestService.updateStatus(
                tenantService.requireBySlug(tenantSlug), id, body.status(), u.getUsername(), u.getRole());
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize(InventraPolicies.REPLENISHMENT_PROCESS)
    public ReplenishmentDtos.ReplenishmentRow receive(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return replenishmentRequestService.receive(
                tenantService.requireBySlug(tenantSlug), id, u.getUsername(), u.getRole());
    }
}
