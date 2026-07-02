package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"tenant", "supplier"})
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "tenant")
    List<AppUser> findAllByTenant_IdOrderByEmailAsc(Long tenantId);

    @EntityGraph(attributePaths = "tenant")
    Optional<AppUser> findByIdAndTenant_Id(Long id, Long tenantId);

    List<AppUser> findByTenant_IdAndRoleOrderByEmailAsc(Long tenantId, AppRole role);

    long countByTenant_Id(Long tenantId);

    boolean existsBySupplier_IdAndRole(Long supplierId, AppRole role);
}
