package com.stowflow.inventra.security;

/**
 * RBAC — moindre privilège par acteur (CDC §5.1).
 *
 * <pre>
 * SUPER_ADMIN     : plateforme SaaS, admin utilisateurs, lecture pilotage, annulation commandes
 * TENANT_ADMIN    : admin tenant (utilisateurs, paramètres), lecture pilotage, annulation commandes
 * STOCK_MANAGER   : opérations stock (articles, fournisseurs, commandes, réappro, mouvements)
 * MANAGER         : pilotage lecture + rapports + création demandes réappro
 * SALES           : point de vente + lecture articles/alertes (disponibilité)
 * </pre>
 */
public final class InventraPolicies {

    private InventraPolicies() {}

    private static final String R_SUPER = "'SUPER_ADMIN'";
    private static final String R_ADMIN = "'TENANT_ADMIN'";
    private static final String R_STOCK = "'STOCK_MANAGER'";
    private static final String R_MANAGER = "'MANAGER'";
    private static final String R_SALES = "'SALES'";

    /** Pilotage + disponibilité — tous les rôles tenant + super admin. */
    public static final String ARTICLES_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + "," + R_SALES + ")";

    /** Catalogue — gestionnaire de stock uniquement. */
    public static final String ARTICLES_WRITE = "hasRole(" + R_STOCK + ")";

    public static final String POS_SALE = "hasRole(" + R_SALES + ")";

    public static final String POS_SALES_READ =
            "hasAnyRole(" + R_SALES + "," + R_MANAGER + "," + R_ADMIN + "," + R_SUPER + ")";

    public static final String POS_COMMERCIAL_DIRECTORY =
            "hasAnyRole(" + R_MANAGER + "," + R_ADMIN + "," + R_SUPER + ")";

    public static final String MOVEMENTS_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + ")";

    public static final String MOVEMENTS_WRITE = "hasRole(" + R_STOCK + ")";

    public static final String REPLENISHMENT_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + ")";

    /** Demande réappro — gestionnaire stock ou manager. */
    public static final String REPLENISHMENT_CREATE =
            "hasAnyRole(" + R_STOCK + "," + R_MANAGER + ")";

    /** Validation / réception réappro — gestionnaire stock uniquement. */
    public static final String REPLENISHMENT_PROCESS = "hasRole(" + R_STOCK + ")";

    public static final String ORDERS_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + ")";

    public static final String ORDERS_WRITE = "hasRole(" + R_STOCK + ")";

    public static final String ORDERS_RECEIVE = "hasRole(" + R_STOCK + ")";

    public static final String ORDERS_CANCEL = "hasAnyRole(" + R_SUPER + "," + R_ADMIN + ")";

    public static final String SUPPLIERS_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + ")";

    public static final String SUPPLIERS_WRITE = "hasRole(" + R_STOCK + ")";

    public static final String DASHBOARD_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + ")";

    public static final String ALERTS_READ =
            "hasAnyRole(" + R_SUPER + "," + R_ADMIN + "," + R_STOCK + "," + R_MANAGER + "," + R_SALES + ")";

    public static final String ADMIN_USERS = "hasAnyRole(" + R_SUPER + "," + R_ADMIN + ")";

    public static final String PLATFORM_TENANTS = "hasRole(" + R_SUPER + ")";
}
