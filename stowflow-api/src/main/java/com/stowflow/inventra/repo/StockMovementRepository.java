package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.MovementType;
import com.stowflow.inventra.domain.StockMovement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query(
            "SELECT DISTINCT m FROM StockMovement m JOIN FETCH m.article a "
                    + "LEFT JOIN FETCH m.saleLine sl LEFT JOIN FETCH sl.sale "
                    + "WHERE a.tenant.id = :tenantId ORDER BY m.createdAt DESC")
    List<StockMovement> findByTenantOrderByCreatedAtDesc(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT m FROM StockMovement m JOIN m.article a "
                    + "WHERE a.tenant.id = :tenantId ORDER BY m.createdAt DESC")
    Page<StockMovement> findPageByTenantOrderByCreatedAtDesc(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM StockMovement m JOIN m.article a WHERE a.tenant.id = :tenantId AND m.createdAt >= :since")
    long countSince(@Param("tenantId") Long tenantId, @Param("since") Instant since);

    @Query(
            "SELECT COUNT(m) FROM StockMovement m JOIN m.article a "
                    + "WHERE a.tenant.id = :tenantId AND m.createdAt >= :from AND m.createdAt < :to")
    long countBetween(
            @Param("tenantId") Long tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    boolean existsByArticle_IdAndType(Long articleId, MovementType type);

    @Query(
            "SELECT DISTINCT m.article.id FROM StockMovement m "
                    + "WHERE m.article.id IN :articleIds AND m.type = :type")
    Set<Long> findArticleIdsWithInMovement(
            @Param("articleIds") Collection<Long> articleIds,
            @Param("type") MovementType type
    );

    boolean existsByReplenishmentRequest_Id(Long replenishmentRequestId);

    @Query(
            "SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m "
                    + "WHERE m.purchaseOrderLine.id = :lineId AND m.type = com.stowflow.inventra.domain.MovementType.IN")
    int sumInQuantityByPurchaseOrderLineId(@Param("lineId") Long lineId);

    @Query(
            value = """
                    SELECT date_trunc('month', m.created_at AT TIME ZONE 'UTC') AS month_start,
                           CAST(SUM(m.quantity) AS integer) AS total
                    FROM stock_movements m
                    INNER JOIN articles a ON a.id = m.article_id
                    WHERE a.tenant_id = :tenantId
                      AND m.created_at >= :since
                    GROUP BY 1
                    ORDER BY 1
                    """,
            nativeQuery = true)
    List<Object[]> sumQuantityByMonth(@Param("tenantId") Long tenantId, @Param("since") Instant since);
}
