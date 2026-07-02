package com.stowflow.inventra.domain;

/**
 * Rôles métier (CDC §5.1) — un acteur = un périmètre RBAC minimal.
 *
 * <ul>
 *   <li>{@link #SUPER_ADMIN} — Éditeur plateforme : tenants SaaS, admin utilisateurs (tout tenant),
 *       consultation pilotage, annulation commandes. Pas d’opérations stock ni vente.</li>
 *   <li>{@link #TENANT_ADMIN} — Admin client : comptes utilisateurs, paramètres tenant,
 *       consultation pilotage (dashboard, rapports, commandes…), annulation commandes.
 *       Pas d’opérations stock quotidiennes.</li>
 *   <li>{@link #STOCK_MANAGER} — Opérations stock : catalogue articles, fournisseurs, commandes
 *       (création, expédition, réception), réapprovisionnements, mouvements de stock.</li>
 *   <li>{@link #MANAGER} — Pilotage : dashboard, rapports, lecture stocks/commandes/fournisseurs,
 *       création de demandes de réapprovisionnement (sans validation ni réception).</li>
 *   <li>{@link #SALES} — Vente : point de vente (sortie stock), historique de ses ventes,
 *       consultation articles et alertes (disponibilité).</li>
 * </ul>
 *
 * <p>Les fournisseurs sont des fiches métier ({@link Supplier}), pas des utilisateurs.
 */
public enum AppRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    STOCK_MANAGER,
    SALES,
    MANAGER
}
