package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.AuditDtos;
import com.stowflow.inventra.dto.PageDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.service.AuditService;
import com.stowflow.inventra.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.ADMIN_USERS)
    public PageDtos.PageResponse<AuditDtos.AuditEventResponse> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditService.list(tenantService.requireBySlug(tenantSlug), page, size);
    }
}
