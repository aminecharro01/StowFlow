package com.stowflow.inventra.bootstrap;

import com.stowflow.inventra.domain.OrderStatus;
import com.stowflow.inventra.domain.PurchaseOrder;
import com.stowflow.inventra.domain.PurchaseOrderLine;
import com.stowflow.inventra.repo.PurchaseOrderRepository;
import com.stowflow.inventra.repo.StockMovementRepository;
import com.stowflow.inventra.service.StockMovementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantit que le stock affiché provient uniquement des mouvements tracés
 * (réceptions commandes / réapprovisionnements), jamais d’une saisie directe à la création produit.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "inventra", name = "legacy-schema-fixer", havingValue = "true")
public class StockIntegrityFixer implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementService stockMovementService;

    @Override
    @Transactional
    public void run(String... args) {
        ensureTraceabilityColumns();
        resetOrphanQuantities();
        backfillReceivedOrderMovements();
        recalculateAllQuantitiesFromMovements();
    }

    private void ensureTraceabilityColumns() {
        try {
            jdbc.execute("ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS purchase_order_line_id bigint");
            jdbc.execute("ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS replenishment_request_id bigint");
        } catch (Exception ex) {
            log.warn("Colonnes traçabilité stock_movements : {}", ex.getMessage());
        }
    }

    /** Stock sans mouvement d’entrée = incohérent (ex. ancien seed direct). */
    private void resetOrphanQuantities() {
        try {
            int n = jdbc.update(
                    "UPDATE articles SET quantity_on_hand = 0 "
                            + "WHERE quantity_on_hand > 0 "
                            + "AND id NOT IN ("
                            + "  SELECT DISTINCT m.article_id FROM stock_movements m WHERE m.type = 'IN'"
                            + ")");
            if (n > 0) {
                log.info("Stock réinitialisé (sans mouvement IN) pour {} article(s).", n);
            }
        } catch (Exception ex) {
            log.warn("Réinitialisation stock orphelin : {}", ex.getMessage());
        }
    }

    private void backfillReceivedOrderMovements() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllReceivedWithLines();
        for (PurchaseOrder po : orders) {
            for (PurchaseOrderLine line : po.getLines()) {
                int received = line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
                if (received <= 0) {
                    continue;
                }
                int alreadyIn = stockMovementRepository.sumInQuantityByPurchaseOrderLineId(line.getId());
                int missing = received - alreadyIn;
                if (missing <= 0) {
                    continue;
                }
                String note = "Commande " + po.getOrderNumber() + " — ligne " + line.getArticle().getSku() + " (migration)";
                stockMovementService.recordPurchaseOrderReceipt(line, missing, note, "system");
                log.info(
                        "Mouvement stock créé pour {} +{} (commande {}).",
                        line.getArticle().getSku(),
                        missing,
                        po.getOrderNumber());
            }
        }
    }

    /** quantity_on_hand = somme des entrées − somme des sorties par article. */
    private void recalculateAllQuantitiesFromMovements() {
        try {
            jdbc.update(
                    "UPDATE articles a SET quantity_on_hand = COALESCE(("
                            + "  SELECT SUM(CASE "
                            + "    WHEN m.type IN ('IN','INVENTORY','ADJUSTMENT') THEN m.quantity "
                            + "    WHEN m.type IN ('OUT','TRANSFER') THEN -m.quantity "
                            + "    ELSE 0 END) "
                            + "  FROM stock_movements m WHERE m.article_id = a.id"
                            + "), 0)");
            log.debug("Quantités articles recalculées depuis les mouvements.");
        } catch (Exception ex) {
            log.warn("Recalcul stock depuis mouvements : {}", ex.getMessage());
        }
    }
}
