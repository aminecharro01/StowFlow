package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.SupplierDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.service.SupplierService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.SUPPLIERS_READ)
    public List<SupplierDtos.SupplierResponse> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(required = false) String q
    ) {
        return supplierService.list(tenantService.requireBySlug(tenantSlug), q);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventraPolicies.SUPPLIERS_READ)
    public SupplierDtos.SupplierDetailResponse get(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id
    ) {
        return supplierService.getDetail(tenantService.requireBySlug(tenantSlug), id);
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.SUPPLIERS_WRITE)
    public SupplierDtos.SupplierResponse create(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @Valid @RequestBody SupplierDtos.CreateSupplierRequest body
    ) {
        return supplierService.create(tenantService.requireBySlug(tenantSlug), body);
    }

    @PutMapping("/{id}")
    @PreAuthorize(InventraPolicies.SUPPLIERS_WRITE)
    public SupplierDtos.SupplierResponse update(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            @Valid @RequestBody SupplierDtos.UpdateSupplierRequest body
    ) {
        return supplierService.update(tenantService.requireBySlug(tenantSlug), id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(InventraPolicies.SUPPLIERS_WRITE)
    public void archive(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id
    ) {
        supplierService.archive(tenantService.requireBySlug(tenantSlug), id);
    }
}
