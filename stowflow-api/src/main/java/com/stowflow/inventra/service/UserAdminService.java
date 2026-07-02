package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.UserAdminDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.AppUserRepository;
import com.stowflow.inventra.security.InventraUserDetails;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Set<AppRole> TENANT_ADMIN_CREATABLE =
            EnumSet.of(AppRole.STOCK_MANAGER, AppRole.SALES, AppRole.MANAGER);

    private static final Set<AppRole> SUPER_TENANT_CREATABLE =
            EnumSet.of(AppRole.TENANT_ADMIN, AppRole.STOCK_MANAGER, AppRole.SALES, AppRole.MANAGER);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserAdminDtos.UserRow> list(InventraUserDetails actor, Tenant tenant) {
        requireAccessToTenant(actor, tenant);
        return appUserRepository.findAllByTenant_IdOrderByEmailAsc(tenant.getId()).stream()
                .map(u -> new UserAdminDtos.UserRow(u.getId(), u.getEmail(), u.getRole().name(), u.isEnabled()))
                .toList();
    }

    @Transactional
    public UserAdminDtos.UserRow create(InventraUserDetails actor, Tenant tenant, UserAdminDtos.CreateUserRequest req) {
        requireAccessToTenant(actor, tenant);
        AppRole role = req.role();
        assertAssignableRole(actor, role);
        if (appUserRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("Un compte existe déjà avec cet e-mail.");
        }
        AppUser u = AppUser.builder()
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .tenant(tenant)
                .enabled(true)
                .build();
        u = appUserRepository.save(u);
        return new UserAdminDtos.UserRow(u.getId(), u.getEmail(), u.getRole().name(), u.isEnabled());
    }

    @Transactional
    public UserAdminDtos.UserRow update(InventraUserDetails actor, Tenant tenant, Long userId, UserAdminDtos.UpdateUserRequest req) {
        requireAccessToTenant(actor, tenant);
        if (req.role() == null && req.enabled() == null) {
            throw new BusinessException("Indiquez au moins un champ à modifier (rôle ou activé).");
        }
        AppUser u = appUserRepository
                .findByIdAndTenant_Id(userId, tenant.getId())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (req.role() != null) {
            assertAssignableRole(actor, req.role());
            u.setRole(req.role());
        }
        if (req.enabled() != null) {
            u.setEnabled(req.enabled());
        }
        u = appUserRepository.save(u);
        return new UserAdminDtos.UserRow(u.getId(), u.getEmail(), u.getRole().name(), u.isEnabled());
    }

    private void requireAccessToTenant(InventraUserDetails actor, Tenant tenant) {
        if (actor.getRole() == AppRole.SUPER_ADMIN) {
            return;
        }
        if (actor.getTenantSlug() == null || !actor.getTenantSlug().equalsIgnoreCase(tenant.getSlug())) {
            throw new BusinessException("Accès refusé à ce tenant.");
        }
        if (actor.getRole() != AppRole.TENANT_ADMIN) {
            throw new BusinessException("Action réservée à l’administrateur.");
        }
    }

    private void assertAssignableRole(InventraUserDetails actor, AppRole role) {
        if (role == AppRole.SUPER_ADMIN) {
            throw new BusinessException("Le rôle Super Admin ne peut pas être attribué via cette API.");
        }
        if (actor.getRole() == AppRole.TENANT_ADMIN) {
            if (!TENANT_ADMIN_CREATABLE.contains(role)) {
                throw new BusinessException("Rôle non autorisé pour un administrateur tenant.");
            }
        } else if (actor.getRole() == AppRole.SUPER_ADMIN) {
            if (!SUPER_TENANT_CREATABLE.contains(role)) {
                throw new BusinessException("Rôle non autorisé.");
            }
        }
    }
}
