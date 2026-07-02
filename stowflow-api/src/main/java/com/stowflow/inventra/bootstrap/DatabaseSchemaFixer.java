package com.stowflow.inventra.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Correctifs SQL pour bases déjà peuplées (migration portail fournisseur, statuts commandes).
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "inventra", name = "legacy-schema-fixer", havingValue = "true")
public class DatabaseSchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        relaxAppUserRoleConstraint();
        disableSupplierPortalUsers();
        migrateLegacyOrderStatuses();
        fixPurchaseOrderLineQuantityReceived();
        migrateDeliveredOrderStatus();
        ensurePurchaseOrderReceiptColumns();
    }

    private void relaxAppUserRoleConstraint() {
        try {
            jdbc.execute("ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check");
        } catch (Exception ex) {
            log.warn("Impossible de modifier app_users_role_check : {}", ex.getMessage());
        }
    }

    /** Supprime les comptes portail fournisseur — les fournisseurs ne se connectent plus. */
    private void disableSupplierPortalUsers() {
        try {
            int n = jdbc.update("DELETE FROM app_users WHERE role = 'SUPPLIER'");
            if (n > 0) {
                log.info("Comptes portail fournisseur supprimés : {}", n);
            }
        } catch (Exception ex) {
            log.warn("Migration comptes SUPPLIER : {}", ex.getMessage());
        }
    }

    /** Anciens statuts portail → en attente ; SHIPPED conservé. */
    private void migrateLegacyOrderStatuses() {
        try {
            jdbc.execute("ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS purchase_orders_status_check");
            int n = jdbc.update(
                    "UPDATE purchase_orders SET status = 'PENDING' "
                            + "WHERE status IN ('CONFIRMED', 'PROCESSING')");
            if (n > 0) {
                log.info("Statuts commande migrés vers PENDING : {}", n);
            }
        } catch (Exception ex) {
            log.warn("Migration statuts commandes legacy : {}", ex.getMessage());
        }
    }

    private void fixPurchaseOrderLineQuantityReceived() {
        try {
            jdbc.execute("ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS quantity_received integer");
            jdbc.execute("UPDATE purchase_order_lines SET quantity_received = 0 WHERE quantity_received IS NULL");
            jdbc.execute("ALTER TABLE purchase_order_lines ALTER COLUMN quantity_received SET DEFAULT 0");
            jdbc.execute("ALTER TABLE purchase_order_lines ALTER COLUMN quantity_received SET NOT NULL");
        } catch (Exception ex) {
            log.warn("Migration quantity_received : {}", ex.getMessage());
        }
    }

    private void migrateDeliveredOrderStatus() {
        try {
            int n = jdbc.update("UPDATE purchase_orders SET status = 'RECEIVED' WHERE status = 'DELIVERED'");
            if (n > 0) {
                log.info("Statuts commande migrés DELIVERED → RECEIVED : {}", n);
            }
        } catch (Exception ex) {
            log.warn("Migration statuts commandes : {}", ex.getMessage());
        }
    }

    private void ensurePurchaseOrderReceiptColumns() {
        try {
            jdbc.execute("ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS supplier_confirmed_at timestamp with time zone");
            jdbc.execute("ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS tenant_received_at timestamp with time zone");
            jdbc.execute("ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS received_by varchar(255)");
        } catch (Exception ex) {
            log.warn("Colonnes réception commande : {}", ex.getMessage());
        }
    }
}
