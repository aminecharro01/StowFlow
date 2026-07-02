package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppUser;
import com.stowflow.inventra.domain.AuditEvent;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.AuditDtos;
import com.stowflow.inventra.dto.PageDtos;
import com.stowflow.inventra.repo.AppUserRepository;
import com.stowflow.inventra.repo.AuditEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditEventRepository auditEventRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public void log(
            Tenant tenant,
            String actorEmail,
            String action,
            String entityType,
            Long entityId,
            String payloadJson
    ) {
        AppUser user = actorEmail != null
                ? appUserRepository.findByEmailIgnoreCase(actorEmail).orElse(null)
                : null;
        auditEventRepository.save(AuditEvent.builder()
                .tenant(tenant)
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .payloadJson(payloadJson)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public PageDtos.PageResponse<AuditDtos.AuditEventResponse> list(Tenant tenant, int page, int size) {
        int cappedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<AuditEvent> result =
                auditEventRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId(), PageRequest.of(page, cappedSize));
        return PageDtos.PageResponse.from(result.map(this::toResponse));
    }

    private AuditDtos.AuditEventResponse toResponse(AuditEvent event) {
        String actorEmail = event.getUser() != null ? event.getUser().getEmail() : null;
        return new AuditDtos.AuditEventResponse(
                event.getId(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getPayloadJson(),
                actorEmail,
                event.getCreatedAt());
    }
}
