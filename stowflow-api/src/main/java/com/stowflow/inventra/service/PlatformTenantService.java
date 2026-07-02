package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.TenantDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.AppUserRepository;
import com.stowflow.inventra.repo.TenantRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformTenantService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TenantDtos.TenantRow> listAll() {
        return tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getSlug))
                .map(this::toRow)
                .toList();
    }

    @Transactional
    public TenantDtos.TenantRow create(TenantDtos.CreateTenantRequest req) {
        String slug = req.slug().trim().toLowerCase();
        if (tenantRepository.existsBySlugIgnoreCase(slug)) {
            throw new BusinessException("Ce slug de tenant est déjà utilisé.");
        }
        String adminEmail = req.adminEmail().trim().toLowerCase();
        if (appUserRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new BusinessException("Un compte existe déjà avec cet e-mail administrateur.");
        }

        Tenant tenant = tenantRepository.save(
                Tenant.builder().slug(slug).name(req.name().trim()).build());

        appUserRepository.save(AppUser.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(req.adminPassword()))
                .role(AppRole.TENANT_ADMIN)
                .tenant(tenant)
                .enabled(true)
                .build());

        return toRow(tenant);
    }

    private TenantDtos.TenantRow toRow(Tenant tenant) {
        int users = (int) appUserRepository.countByTenant_Id(tenant.getId());
        return new TenantDtos.TenantRow(tenant.getId(), tenant.getSlug(), tenant.getName(), users, users > 0);
    }
}
