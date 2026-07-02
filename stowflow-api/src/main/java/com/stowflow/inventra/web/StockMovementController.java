package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.MovementDtos;
import com.stowflow.inventra.dto.PageDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.service.StockMovementService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.MOVEMENTS_READ)
    public PageDtos.PageResponse<MovementDtos.MovementResponse> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return stockMovementService.list(tenantService.requireBySlug(tenantSlug), page, size);
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.MOVEMENTS_WRITE)
    public MovementDtos.MovementResponse create(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication,
            @Valid @RequestBody MovementDtos.CreateMovementRequest body
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        return stockMovementService.record(tenantService.requireBySlug(tenantSlug), body, actor);
    }
}
