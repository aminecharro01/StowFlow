package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.ReplenishmentRequest;
import com.stowflow.inventra.domain.ReplenishmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReplenishmentRequestRepository extends JpaRepository<ReplenishmentRequest, Long> {

    @EntityGraph(attributePaths = {"article", "supplier"})
    @Query(
            "SELECT r FROM ReplenishmentRequest r WHERE r.tenant.id = :tenantId ORDER BY r.createdAt DESC, r.id DESC")
    List<ReplenishmentRequest> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = {"article", "supplier", "tenant"})
    Optional<ReplenishmentRequest> findByIdAndTenant_Id(Long id, Long tenantId);

    long countByTenant_IdAndStatus(Long tenantId, ReplenishmentStatus status);

    long countByTenant_Id(Long tenantId);
}
