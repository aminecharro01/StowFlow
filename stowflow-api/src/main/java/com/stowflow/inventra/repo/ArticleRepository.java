package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.Article;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("SELECT a FROM Article a JOIN FETCH a.tenant t WHERE t.id = :tenantId AND a.archived = false")
    List<Article> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query(
            """
            SELECT a FROM Article a
            WHERE a.tenant.id = :tenantId AND a.archived = false
              AND (:q IS NULL OR :q = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.sku) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR :category = '' OR LOWER(a.category) = LOWER(:category))
            """)
    Page<Article> findActiveByTenantIdFiltered(
            @Param("tenantId") Long tenantId,
            @Param("q") String q,
            @Param("category") String category,
            Pageable pageable
    );

    @Query("SELECT a FROM Article a JOIN FETCH a.tenant t WHERE t.id = :tenantId AND a.sku = :sku AND a.archived = false")
    Optional<Article> findActiveByTenantIdAndSku(@Param("tenantId") Long tenantId, @Param("sku") String sku);

    @Query("""
            SELECT a FROM Article a JOIN FETCH a.tenant t
            WHERE t.id = :tenantId AND a.archived = false
              AND LOWER(a.name) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    List<Article> searchActiveByTenantIdAndName(@Param("tenantId") Long tenantId, @Param("term") String term);

    boolean existsByTenantIdAndSkuAndArchivedFalse(Long tenantId, String sku);

    @Query("SELECT COALESCE(SUM(a.quantityOnHand * a.salePrice), 0) FROM Article a WHERE a.tenant.id = :tenantId AND a.archived = false")
    java.math.BigDecimal sumInventoryValueByTenant(@Param("tenantId") Long tenantId);

    long countByTenantIdAndArchivedFalse(Long tenantId);

    @Query(
            "SELECT COUNT(a) FROM Article a WHERE a.tenant.id = :tenantId AND a.archived = false "
                    + "AND (a.quantityOnHand = 0 OR a.quantityOnHand < a.minThreshold)")
    long countInAlertByTenantId(@Param("tenantId") Long tenantId);
}
