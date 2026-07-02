package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.MovementType;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.StockMovementRepository;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Règles d’approvisionnement :
 * <ul>
 *   <li><b>Commande fournisseur</b> — nouveau produit (jamais réceptionné) ou stock sain (pas d’alerte).</li>
 *   <li><b>Demande de réapprovisionnement</b> — produit déjà approvisionné, en alerte stock.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProcurementPolicy {

    private final StockMovementRepository stockMovementRepository;

    public record BatchContext(Set<Long> articleIdsWithInMovement) {

        public boolean hasInMovement(Long articleId) {
            return articleIdsWithInMovement.contains(articleId);
        }

        public static BatchContext empty() {
            return new BatchContext(Set.of());
        }
    }

    public BatchContext batchContext(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return BatchContext.empty();
        }
        Set<Long> withIn = stockMovementRepository.findArticleIdsWithInMovement(articleIds, MovementType.IN);
        return new BatchContext(withIn != null ? withIn : Set.of());
    }

    /** Produit jamais entré en stock (premier approvisionnement). */
    public boolean isNewProduct(Article article) {
        return !stockMovementRepository.existsByArticle_IdAndType(article.getId(), MovementType.IN);
    }

    public boolean isNewProduct(Article article, BatchContext context) {
        return !context.hasInMovement(article.getId());
    }

    /** Rupture, critique (&lt; 6) ou sous le seuil minimum. */
    public boolean isInStockAlert(Article article) {
        int q = article.getQuantityOnHand();
        if (q == 0) {
            return true;
        }
        if (q < 6) {
            return true;
        }
        return q < article.getMinThreshold();
    }

    public boolean eligibleForPurchaseOrder(Article article) {
        return isNewProduct(article) || !isInStockAlert(article);
    }

    public boolean eligibleForPurchaseOrder(Article article, BatchContext context) {
        return isNewProduct(article, context) || !isInStockAlert(article);
    }

    public boolean eligibleForReplenishment(Article article) {
        return !isNewProduct(article) && isInStockAlert(article);
    }

    public boolean eligibleForReplenishment(Article article, BatchContext context) {
        return !isNewProduct(article, context) && isInStockAlert(article);
    }

    /** Quantité max commandable sans dépasser le plafond de stock (stock actuel + commande ≤ max). */
    public int maxOrderableQuantity(Article article) {
        return Math.max(0, article.getMaxThreshold() - article.getQuantityOnHand());
    }

    public void assertOrderQuantityWithinMax(Article article, int orderQty) {
        if (orderQty < 1) {
            return;
        }
        int max = maxOrderableQuantity(article);
        if (orderQty > max) {
            throw new BusinessException(
                    "Pour « "
                            + article.getSku()
                            + " », la quantité "
                            + orderQty
                            + " dépasserait le stock max ("
                            + article.getMaxThreshold()
                            + "). Stock actuel : "
                            + article.getQuantityOnHand()
                            + " — maximum commandable : "
                            + max
                            + ".");
        }
    }

    /** Suggestion indicative pour une commande fournisseur. */
    public int suggestedPurchaseOrderQuantity(Article article) {
        return suggestedPurchaseOrderQuantity(article, batchContext(Set.of(article.getId())));
    }

    public int suggestedPurchaseOrderQuantity(Article article, BatchContext context) {
        int room = Math.max(0, article.getMaxThreshold() - article.getQuantityOnHand());
        if (isNewProduct(article, context)) {
            return Math.max(1, Math.max(article.getMinThreshold(), room));
        }
        return Math.max(1, room);
    }

    /** Suggestion indicative pour une demande de réapprovisionnement. */
    public int suggestedReplenishmentQuantity(Article article) {
        return suggestedReplenishmentQuantity(article, batchContext(Set.of(article.getId())));
    }

    public int suggestedReplenishmentQuantity(Article article, BatchContext context) {
        if (!eligibleForReplenishment(article, context)) {
            return 0;
        }
        int deficit = article.getMinThreshold() - article.getQuantityOnHand();
        int room = article.getMaxThreshold() - article.getQuantityOnHand();
        if (room <= 0) {
            return 0;
        }
        return Math.min(Math.max(deficit, 1), room);
    }

    public String purchaseOrderRejectionReason(Article article) {
        if (eligibleForPurchaseOrder(article)) {
            return null;
        }
        return "L’article « "
                + article.getSku()
                + " » est en alerte stock. Utilisez une demande de réapprovisionnement.";
    }

    public String replenishmentRejectionReason(Article article) {
        if (eligibleForReplenishment(article)) {
            return null;
        }
        if (isNewProduct(article)) {
            return "Produit nouveau : passez par une commande fournisseur pour le premier approvisionnement.";
        }
        return "Le stock de « "
                + article.getSku()
                + " » est suffisant. Utilisez une commande fournisseur pour un réassort planifié.";
    }
}
