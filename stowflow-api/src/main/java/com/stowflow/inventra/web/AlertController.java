package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.AlertDtos;
import com.stowflow.inventra.service.AlertService;
import com.stowflow.inventra.service.TenantService;
import java.util.List;
import com.stowflow.inventra.security.InventraPolicies;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.ALERTS_READ)
    public List<AlertDtos.AlertRow> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(required = false) String status
    ) {
        return alertService.list(tenantService.requireBySlug(tenantSlug), status);
    }

    @GetMapping("/summary")
    @PreAuthorize(InventraPolicies.ALERTS_READ)
    public AlertDtos.AlertSummary summary(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug
    ) {
        return alertService.summary(tenantService.requireBySlug(tenantSlug));
    }
}
