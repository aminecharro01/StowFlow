package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.TenantDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.service.PlatformTenantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {

    private final PlatformTenantService platformTenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.PLATFORM_TENANTS)
    public List<TenantDtos.TenantRow> list() {
        return platformTenantService.listAll();
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.PLATFORM_TENANTS)
    public TenantDtos.TenantRow create(@Valid @RequestBody TenantDtos.CreateTenantRequest body) {
        return platformTenantService.create(body);
    }
}
