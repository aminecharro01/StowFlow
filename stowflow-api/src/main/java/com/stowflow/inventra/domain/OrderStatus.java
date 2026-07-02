package com.stowflow.inventra.domain;

public enum OrderStatus {
    /** Commande créée — en attente d’expédition. */
    PENDING,
    /** Marchandise expédiée — en attente de réception et mise à jour stock. */
    SHIPPED,
    /** Réception validée — stock synchronisé automatiquement. */
    RECEIVED,
    CANCELLED,
    /** @deprecated Ancien statut portail — migré vers {@link #PENDING}. */
    @Deprecated
    CONFIRMED,
    /** @deprecated Ancien statut portail — migré vers {@link #PENDING}. */
    @Deprecated
    PROCESSING
}
