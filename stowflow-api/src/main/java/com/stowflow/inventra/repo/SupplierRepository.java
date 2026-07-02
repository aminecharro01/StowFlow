package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.Supplier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId AND s.archived = false ORDER BY s.name")
    List<Supplier> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId AND LOWER(s.name) = LOWER(:name) AND s.archived = false")
    Optional<Supplier> findByTenantIdAndNameIgnoreCase(@Param("tenantId") Long tenantId, @Param("name") String name);
}
