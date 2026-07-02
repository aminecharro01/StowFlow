package com.stowflow.inventra.repo;

import com.stowflow.inventra.domain.OrderStatus;
import com.stowflow.inventra.domain.PurchaseOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query(
            "SELECT DISTINCT o FROM PurchaseOrder o "
                    + "JOIN FETCH o.supplier s "
                    + "LEFT JOIN FETCH o.lines "
                    + "WHERE o.tenant.id = :tenantId "
                    + "ORDER BY o.orderDate DESC, o.id DESC")
    List<PurchaseOrder> findByTenantWithSupplierAndLines(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT DISTINCT o FROM PurchaseOrder o "
                    + "JOIN FETCH o.supplier s "
                    + "LEFT JOIN FETCH o.lines l "
                    + "LEFT JOIN FETCH l.article "
                    + "WHERE o.id = :id AND o.tenant.id = :tenantId")
    Optional<PurchaseOrder> findDetailByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT DISTINCT o FROM PurchaseOrder o "
                    + "JOIN FETCH o.supplier s "
                    + "LEFT JOIN FETCH o.lines l "
                    + "LEFT JOIN FETCH l.article "
                    + "WHERE o.supplier.id = :supplierId "
                    + "ORDER BY o.orderDate DESC, o.id DESC")
    List<PurchaseOrder> findBySupplierIdWithLines(@Param("supplierId") Long supplierId);

    @Query(
            "SELECT DISTINCT o FROM PurchaseOrder o "
                    + "JOIN FETCH o.supplier s "
                    + "LEFT JOIN FETCH o.lines l "
                    + "LEFT JOIN FETCH l.article "
                    + "WHERE o.id = :id AND o.supplier.id = :supplierId")
    Optional<PurchaseOrder> findDetailByIdAndSupplierId(@Param("id") Long id, @Param("supplierId") Long supplierId);

    @Query("SELECT o FROM PurchaseOrder o JOIN FETCH o.supplier s WHERE o.id = :id AND o.tenant.id = :tenantId")
    Optional<PurchaseOrder> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    long countByTenantId(Long tenantId);

    @Query(
            "SELECT COUNT(o) FROM PurchaseOrder o WHERE o.tenant.id = :tenantId "
                    + "AND o.orderDate >= :from AND o.orderDate < :to")
    long countByTenantIdAndOrderDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT COUNT(o) FROM PurchaseOrder o WHERE o.tenant.id = :tenantId AND o.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") OrderStatus status);

    @Query(
            "SELECT DISTINCT o FROM PurchaseOrder o "
                    + "JOIN FETCH o.lines l "
                    + "JOIN FETCH l.article "
                    + "WHERE o.status = 'RECEIVED'")
    List<PurchaseOrder> findAllReceivedWithLines();
}
