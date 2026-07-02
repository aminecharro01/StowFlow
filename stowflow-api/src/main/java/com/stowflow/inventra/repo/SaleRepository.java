package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.Sale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"lines", "lines.article", "tenant"})
    Optional<Sale> findByIdAndTenant_Id(Long id, Long tenantId);

    Page<Sale> findByTenant_IdAndSoldByEmailIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId, String soldByEmail, Pageable pageable);

    @Query(
            "select s from Sale s where s.tenant.id = :tid and "
                    + "(coalesce(:soldBy, '') = '' or lower(s.soldByEmail) like lower(concat('%', :soldBy, '%'))) "
                    + "order by s.createdAt desc")
    Page<Sale> findByTenantForManager(@Param("tid") Long tid, @Param("soldBy") String soldBy, Pageable pageable);

    long countByTenant_Id(Long tenantId);
}
