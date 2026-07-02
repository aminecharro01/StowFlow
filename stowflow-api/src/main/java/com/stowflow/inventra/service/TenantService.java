package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Tenant requireBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            slug = "default";
        }
        final String s = slug.trim();
        return tenantRepository.findBySlug(s).orElseThrow(() -> new BusinessException("Tenant inconnu: " + s));
    }
}
