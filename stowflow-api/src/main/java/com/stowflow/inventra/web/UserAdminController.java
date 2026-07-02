package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.UserAdminDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.service.TenantService;
import com.stowflow.inventra.service.UserAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.ADMIN_USERS)
    public List<UserAdminDtos.UserRow> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return userAdminService.list(u, tenantService.requireBySlug(tenantSlug));
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.ADMIN_USERS)
    public UserAdminDtos.UserRow create(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            Authentication authentication,
            @Valid @RequestBody UserAdminDtos.CreateUserRequest body
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return userAdminService.create(u, tenantService.requireBySlug(tenantSlug), body);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(InventraPolicies.ADMIN_USERS)
    public UserAdminDtos.UserRow update(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody UserAdminDtos.UpdateUserRequest body
    ) {
        InventraUserDetails u = (InventraUserDetails) authentication.getPrincipal();
        return userAdminService.update(u, tenantService.requireBySlug(tenantSlug), id, body);
    }
}
