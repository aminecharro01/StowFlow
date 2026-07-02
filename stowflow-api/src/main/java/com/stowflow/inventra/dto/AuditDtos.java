package com.stowflow.inventra.dto;

import java.time.Instant;

public final class AuditDtos {

    public record AuditEventResponse(
            Long id,
            String action,
            String entityType,
            Long entityId,
            String payloadJson,
            String actorEmail,
            Instant createdAt
    ) {}

    private AuditDtos() {}
}
