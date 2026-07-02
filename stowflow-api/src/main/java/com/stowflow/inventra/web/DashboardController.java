package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.DashboardDtos;
import com.stowflow.inventra.service.DashboardService;
import com.stowflow.inventra.service.TenantService;
import com.stowflow.inventra.security.InventraPolicies;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.DASHBOARD_READ)
    public DashboardDtos.DashboardPayload get(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug
    ) {
        return dashboardService.build(tenantService.requireBySlug(tenantSlug));
    }
}
