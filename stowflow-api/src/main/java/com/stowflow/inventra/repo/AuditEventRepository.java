package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("SELECT e FROM AuditEvent e WHERE e.tenant.id = :tenantId ORDER BY e.createdAt DESC")
    Page<AuditEvent> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") Long tenantId, Pageable pageable);
}
