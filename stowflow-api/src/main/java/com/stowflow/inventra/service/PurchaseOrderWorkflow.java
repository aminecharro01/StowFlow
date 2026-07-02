package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.OrderStatus;

/**
 * Transitions de statut des commandes fournisseurs — workflow interne tenant.
 */
public final class PurchaseOrderWorkflow {

    private PurchaseOrderWorkflow() {}

    /** Marquer la commande comme expédiée (marchandise en route). */
    public static boolean canShip(OrderStatus status) {
        return status == OrderStatus.PENDING
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.PROCESSING;
    }

    /** Réception marchandise — uniquement après expédition. */
    public static boolean canReceive(OrderStatus status) {
        return status == OrderStatus.SHIPPED;
    }

    /** Annulation côté tenant (admin) avant réception complète. */
    public static boolean isTenantCancelAllowed(OrderStatus from, OrderStatus to, AppRole actor) {
        if (to != OrderStatus.CANCELLED) {
            return false;
        }
        boolean admin = actor == AppRole.SUPER_ADMIN || actor == AppRole.TENANT_ADMIN;
        if (!admin) {
            return false;
        }
        return from == OrderStatus.PENDING
                || from == OrderStatus.SHIPPED
                || from == OrderStatus.CONFIRMED
                || from == OrderStatus.PROCESSING;
    }

    public static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.RECEIVED || status == OrderStatus.CANCELLED;
    }
}
