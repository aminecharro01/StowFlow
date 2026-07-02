package com.stowflow.inventra.bootstrap;

import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.AuditEvent;
import com.stowflow.inventra.domain.MovementType;
import com.stowflow.inventra.domain.OrderStatus;
import com.stowflow.inventra.domain.PurchaseOrder;
import com.stowflow.inventra.domain.PurchaseOrderLine;
import com.stowflow.inventra.domain.ReplenishmentRequest;
import com.stowflow.inventra.domain.ReplenishmentStatus;
import com.stowflow.inventra.domain.Sale;
import com.stowflow.inventra.domain.SaleLine;
import com.stowflow.inventra.domain.StockMovement;
import com.stowflow.inventra.domain.Supplier;
import com.stowflow.inventra.domain.SupplierStatus;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.AuditEventRepository;
import com.stowflow.inventra.repo.PurchaseOrderRepository;
import com.stowflow.inventra.repo.ReplenishmentRequestRepository;
import com.stowflow.inventra.repo.SaleRepository;
import com.stowflow.inventra.repo.StockMovementRepository;
import com.stowflow.inventra.repo.SupplierRepository;
import com.stowflow.inventra.repo.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rich demo dataset: catalog, purchase orders, stock timeline, POS sales,
 * replenishment workflow, and audit trail for dashboard / reports demos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "inventra", name = "seed-demo", havingValue = "true")
public class DemoScenarioSeeder {

    private static final String ADMIN = "admin@default.demo";
    private static final String STOCK = "stock@default.demo";
    private static final String SALES = "sales@default.demo";
    private static final int MIN_DEMO_ARTICLES = 20;

    private final TenantRepository tenantRepository;
    private final ArticleRepository articleRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SaleRepository saleRepository;
    private final ReplenishmentRequestRepository replenishmentRequestRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuditEventRepository auditEventRepository;
    private final JdbcTemplate jdbc;

    @Transactional
    public void seedFresh() {
        Tenant tenant = tenantRepository.save(Tenant.builder().slug("default").name("StowFlow Demo").build());
        populateTenant(tenant);
        seedAcmeTenant();
    }

    @Transactional
    public void enrichIfNeeded() {
        tenantRepository.findBySlug("default").ifPresent(tenant -> {
            long articleCount = articleRepository.countByTenantIdAndArchivedFalse(tenant.getId());
            if (articleCount < MIN_DEMO_ARTICLES) {
                log.info(
                        "Base démo incomplète ({} articles) — réinitialisation du catalogue et des scénarios…",
                        articleCount);
                wipeTenantInventory(tenant.getId());
                populateTenant(tenant);
                return;
            }
            if (saleRepository.countByTenant_Id(tenant.getId()) > 0) {
                return;
            }
            log.info("Enrichissement des données démo (ventes, réappro, historique mouvements)…");
            List<Article> articles = articleRepository.findActiveByTenantId(tenant.getId());
            List<Supplier> suppliers = supplierRepository.findActiveByTenantId(tenant.getId());
            if (suppliers.isEmpty()) {
                suppliers = seedSuppliers(tenant);
            }
            seedReplenishments(tenant, suppliers, articles);
            seedSalesHistory(tenant, articles);
            seedMovementTimeline(tenant, articles);
            seedAuditTrail(tenant, articles);
            recalculateStock();
            log.info("Données démo enrichies pour le tenant « default ».");
        });
    }

    private void populateTenant(Tenant tenant) {
        List<Supplier> suppliers = seedSuppliers(tenant);
        List<Article> articles = seedArticles(tenant);
        seedPurchaseOrders(tenant, suppliers, articles);
        seedReplenishments(tenant, suppliers, articles);
        seedSalesHistory(tenant, articles);
        seedMovementTimeline(tenant, articles);
        seedAuditTrail(tenant, articles);
        recalculateStock();

        log.info(
                "Jeu de données démo — {} articles, {} commandes, {} ventes, {} demandes réappro.",
                articles.size(),
                purchaseOrderRepository.countByTenantId(tenant.getId()),
                saleRepository.countByTenant_Id(tenant.getId()),
                replenishmentRequestRepository.countByTenant_Id(tenant.getId()));
    }

    private void wipeTenantInventory(Long tenantId) {
        jdbc.update("DELETE FROM audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update(
                "DELETE FROM stock_movements WHERE article_id IN (SELECT id FROM articles WHERE tenant_id = ?)",
                tenantId);
        jdbc.update("DELETE FROM sale_lines WHERE sale_id IN (SELECT id FROM sales WHERE tenant_id = ?)", tenantId);
        jdbc.update("DELETE FROM sales WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM replenishment_requests WHERE tenant_id = ?", tenantId);
        jdbc.update(
                "DELETE FROM purchase_order_lines WHERE purchase_order_id IN "
                        + "(SELECT id FROM purchase_orders WHERE tenant_id = ?)",
                tenantId);
        jdbc.update("DELETE FROM purchase_orders WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM articles WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM suppliers WHERE tenant_id = ?", tenantId);
    }

    private List<Supplier> seedSuppliers(Tenant tenant) {
        return List.of(
                saveSupplier(tenant, "TechGlobe Ltd", "james@techglobe.com", "USA", 3, SupplierStatus.ACTIVE, 142),
                saveSupplier(tenant, "NordSupply AB", "anna@nordsupply.se", "Sweden", 7, SupplierStatus.ACTIVE, 88),
                saveSupplier(tenant, "AsiaTech Co.", "li@asiatech.cn", "China", 14, SupplierStatus.ACTIVE, 213),
                saveSupplier(tenant, "EuroParts GmbH", "hans@europarts.de", "Germany", 5, SupplierStatus.INACTIVE, 67),
                saveSupplier(tenant, "GlobalMart Inc", "sarah@globalmart.com", "USA", 4, SupplierStatus.ACTIVE, 95),
                saveSupplier(tenant, "PacificSource", "kim@pacificsource.kr", "Korea", 10, SupplierStatus.ON_HOLD, 54),
                saveSupplier(tenant, "MedSupplies SA", "carlos@medsup.es", "Spain", 6, SupplierStatus.ACTIVE, 38));
    }

    private List<Article> seedArticles(Tenant tenant) {
        List<ArticleSeed> catalog = List.of(
                new ArticleSeed("SKU-0021", "Wireless Earbuds Pro X", "Electronics", 120, 20, 200),
                new ArticleSeed("SKU-0045", "USB-C Hub 7-Port", "Accessories", 49, 25, 150),
                new ArticleSeed("SKU-0088", "Mechanical Keyboard RGB", "Electronics", 189, 15, 120),
                new ArticleSeed("SKU-0102", "Noise-Cancelling Headphones", "Electronics", 299, 30, 500),
                new ArticleSeed("SKU-0134", "Standing Desk Mat", "Office", 45, 20, 300),
                new ArticleSeed("SKU-0156", "4K Webcam Ultra", "Electronics", 159, 25, 200),
                new ArticleSeed("SKU-0178", "Ergonomic Mouse Pad XL", "Accessories", 29, 15, 250),
                new ArticleSeed("SKU-0199", "LED Desk Lamp Pro", "Office", 89, 10, 100),
                new ArticleSeed("SKU-0213", "Wireless Mouse Slim", "Accessories", 35, 30, 200),
                new ArticleSeed("SKU-0224", "Monitor Stand Deluxe", "Office", 55, 20, 150),
                new ArticleSeed("SKU-0236", "HDMI Cable 4K 2m", "Accessories", 18, 40, 300),
                new ArticleSeed("SKU-0248", "Portable SSD 1TB", "Electronics", 89, 12, 180),
                new ArticleSeed("SKU-0259", "Smart Watch Series 5", "Electronics", 349, 8, 80),
                new ArticleSeed("SKU-0267", "Bluetooth Speaker Mini", "Electronics", 79, 15, 120),
                new ArticleSeed("SKU-0271", "Laptop Sleeve 15\"", "Accessories", 45, 20, 200),
                new ArticleSeed("SKU-0282", "USB-C Cable 2m", "Accessories", 22, 50, 400),
                new ArticleSeed("SKU-0293", "Notebook A4 Pack (5)", "Office", 12, 30, 500),
                new ArticleSeed("SKU-0301", "Pen Set Premium", "Office", 28, 25, 300),
                new ArticleSeed("SKU-0310", "Office Chair Ergo", "Furniture", 890, 3, 25),
                new ArticleSeed("SKU-0318", "Standing Desk", "Furniture", 1290, 2, 15),
                new ArticleSeed("SKU-0325", "WiFi Router AX3000", "Networking", 199, 10, 60),
                new ArticleSeed("SKU-0332", "Ethernet Switch 8-Port", "Networking", 65, 12, 100),
                new ArticleSeed("SKU-0340", "Label Printer Pro", "Office", 249, 5, 40),
                new ArticleSeed("SKU-0351", "Document Scanner A4", "Electronics", 179, 6, 50));

        List<Article> saved = new ArrayList<>();
        for (ArticleSeed s : catalog) {
            saved.add(articleRepository.save(Article.builder()
                    .tenant(tenant)
                    .sku(s.sku())
                    .name(s.name())
                    .category(s.category())
                    .salePrice(BigDecimal.valueOf(s.salePrice()))
                    .purchasePrice(BigDecimal.valueOf(s.salePrice() * 0.58).setScale(2, RoundingMode.HALF_UP))
                    .quantityOnHand(0)
                    .minThreshold(s.min())
                    .maxThreshold(s.max())
                    .description("Article de démonstration — " + s.category())
                    .archived(false)
                    .build()));
        }
        return saved;
    }

    private void seedPurchaseOrders(Tenant tenant, List<Supplier> suppliers, List<Article> articles) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Received — stock in (spread over past months for dashboard chart)
        createReceivedOrder(
                tenant,
                "PO-2026-0142",
                suppliers.get(2),
                today.minusMonths(5).withDayOfMonth(8),
                List.of(line(articles.get(7), 40, 29), line(articles.get(11), 30, 89)),
                ADMIN);
        createReceivedOrder(
                tenant,
                "PO-2026-0158",
                suppliers.get(0),
                today.minusMonths(4).withDayOfMonth(12),
                List.of(line(articles.get(2), 45, 189), line(articles.get(5), 25, 159)),
                ADMIN);
        createReceivedOrder(
                tenant,
                "PO-2026-0171",
                suppliers.get(4),
                today.minusMonths(3).withDayOfMonth(5),
                List.of(line(articles.get(9), 35, 55), line(articles.get(16), 80, 12)),
                STOCK);
        createReceivedOrder(
                tenant,
                "PO-2026-0189",
                suppliers.get(1),
                today.minusMonths(2).withDayOfMonth(18),
                List.of(line(articles.get(20), 18, 199), line(articles.get(21), 22, 65)),
                ADMIN);
        createReceivedOrder(
                tenant,
                "PO-2026-0203",
                suppliers.get(6),
                today.minusMonths(1).withDayOfMonth(9),
                List.of(line(articles.get(12), 12, 349), line(articles.get(13), 28, 79)),
                STOCK);
        createReceivedOrder(
                tenant,
                "PO-2026-0217",
                suppliers.get(0),
                today.minusDays(20),
                List.of(line(articles.get(4), 50, 45), line(articles.get(8), 60, 35), line(articles.get(10), 100, 18)),
                ADMIN);

        // Pipeline orders
        createOpenOrder(
                tenant,
                "PO-2026-0224",
                suppliers.get(2),
                today.minusDays(5),
                OrderStatus.PENDING,
                List.of(line(articles.get(0), 40, 120), line(articles.get(3), 20, 299)));
        createOpenOrder(
                tenant,
                "PO-2026-0225",
                suppliers.get(4),
                today.minusDays(3),
                OrderStatus.PENDING,
                List.of(line(articles.get(1), 50, 49), line(articles.get(6), 40, 29)));
        createOpenOrder(
                tenant,
                "PO-2026-0226",
                suppliers.get(1),
                today.minusDays(8),
                OrderStatus.SHIPPED,
                List.of(line(articles.get(18), 4, 890), line(articles.get(22), 8, 249)));
        createOpenOrder(
                tenant,
                "PO-2026-0227",
                suppliers.get(3),
                today.minusDays(12),
                OrderStatus.SHIPPED,
                List.of(line(articles.get(19), 3, 1290)));
        createOpenOrder(
                tenant,
                "PO-2026-0210",
                suppliers.get(5),
                today.minusMonths(1).withDayOfMonth(22),
                OrderStatus.CANCELLED,
                List.of(line(articles.get(15), 30, 45)));
    }

    private void seedReplenishments(Tenant tenant, List<Supplier> suppliers, List<Article> articles) {
        Instant now = Instant.now();

        // Pending — out-of-stock items
        saveReplenishment(
                tenant,
                articles.get(0),
                suppliers.get(0),
                40,
                ReplenishmentStatus.PENDING,
                "Rupture — commande client en attente",
                STOCK,
                now.minusSeconds(86_400 * 2));
        saveReplenishment(
                tenant,
                articles.get(3),
                suppliers.get(2),
                25,
                ReplenishmentStatus.PENDING,
                "Stock critique showroom",
                STOCK,
                now.minusSeconds(86_400));
        saveReplenishment(
                tenant,
                articles.get(1),
                suppliers.get(4),
                35,
                ReplenishmentStatus.IN_PROGRESS,
                "Confirmé fournisseur — livraison J+5",
                ADMIN,
                now.minusSeconds(86_400 * 4));

        // Received via replenishment (with stock IN)
        ReplenishmentRequest received = saveReplenishment(
                tenant,
                articles.get(14),
                suppliers.get(1),
                30,
                ReplenishmentStatus.RECEIVED,
                "Réception entrepôt B",
                STOCK,
                now.minusSeconds(86_400 * 14));
        received.setProcessedBy(ADMIN);
        received.setProcessedAt(now.minusSeconds(86_400 * 10));
        received.setReceivedBy(STOCK);
        received.setReceivedAt(now.minusSeconds(86_400 * 9));
        replenishmentRequestRepository.save(received);
        saveInMovement(
                articles.get(14),
                30,
                now.minusSeconds(86_400 * 9),
                "Réappro " + received.getId() + " — Laptop Sleeve",
                STOCK,
                null,
                received);

        // Rejected / cancelled
        saveReplenishment(
                tenant,
                articles.get(19),
                suppliers.get(3),
                2,
                ReplenishmentStatus.REJECTED,
                "Budget Q2 dépassé",
                ADMIN,
                now.minusSeconds(86_400 * 20));
        saveReplenishment(
                tenant,
                articles.get(18),
                suppliers.get(5),
                3,
                ReplenishmentStatus.CANCELLED,
                "Doublon avec commande PO-2026-0226",
                STOCK,
                now.minusSeconds(86_400 * 6));
    }

    private void seedSalesHistory(Tenant tenant, List<Article> articles) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        recalculateStock();

        // Sales spread over ~3 months — only articles with available stock
        recordDemoSale(tenant, SALES, today.minusDays(75), List.of(saleLine(articles.get(2), 3), saleLine(articles.get(7), 5)));
        recordDemoSale(tenant, SALES, today.minusDays(68), List.of(saleLine(articles.get(16), 12), saleLine(articles.get(17), 4)));
        recordDemoSale(tenant, SALES, today.minusDays(55), List.of(saleLine(articles.get(5), 2), saleLine(articles.get(13), 3)));
        recordDemoSale(tenant, SALES, today.minusDays(42), List.of(saleLine(articles.get(9), 4), saleLine(articles.get(4), 6)));
        recordDemoSale(tenant, SALES, today.minusDays(35), List.of(saleLine(articles.get(20), 2), saleLine(articles.get(21), 3)));
        recordDemoSale(tenant, SALES, today.minusDays(28), List.of(saleLine(articles.get(8), 8), saleLine(articles.get(10), 15)));
        recordDemoSale(tenant, SALES, today.minusDays(21), List.of(saleLine(articles.get(12), 1), saleLine(articles.get(2), 2)));
        recordDemoSale(tenant, SALES, today.minusDays(14), List.of(saleLine(articles.get(11), 4), saleLine(articles.get(7), 6)));
        recordDemoSale(tenant, SALES, today.minusDays(7), List.of(saleLine(articles.get(4), 10), saleLine(articles.get(16), 20)));
        recordDemoSale(tenant, SALES, today.minusDays(3), List.of(saleLine(articles.get(13), 5), saleLine(articles.get(5), 1)));
        recordDemoSale(tenant, SALES, today.minusDays(1), List.of(saleLine(articles.get(9), 2), saleLine(articles.get(17), 6)));
        recordDemoSale(tenant, SALES, today, List.of(saleLine(articles.get(10), 8), saleLine(articles.get(8), 4)));
    }

    /** Backdated IN/OUT pairs to populate the 6-month movement chart without skewing net stock. */
    private void seedMovementTimeline(Tenant tenant, List<Article> articles) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int monthsAgo = 5; monthsAgo >= 1; monthsAgo--) {
            LocalDate month = today.minusMonths(monthsAgo);
            Article a1 = articles.get((monthsAgo * 3) % articles.size());
            Article a2 = articles.get((monthsAgo * 5 + 2) % articles.size());
            int inQty = 30 + monthsAgo * 12;
            int outQty = 10 + monthsAgo * 5;

            Instant inAt = at(month.withDayOfMonth(4), 10);
            Instant outAt = at(month.withDayOfMonth(16), 14);

            saveInMovement(a1, inQty, inAt, "Réception mensuelle démo", STOCK, null, null);
            saveInMovement(a2, inQty / 2, inAt.plusSeconds(3600), "Réception mensuelle démo", STOCK, null, null);
            recalculateStock();

            Article fresh1 = reload(a1);
            int out1 = Math.min(outQty, fresh1.getQuantityOnHand());
            if (out1 > 0) {
                saveOutMovement(fresh1, out1, outAt, "Sortie mensuelle démo", SALES, null);
            }
            Article fresh2 = reload(a2);
            int out2 = Math.min(outQty / 2, fresh2.getQuantityOnHand());
            if (out2 > 0) {
                saveOutMovement(fresh2, out2, outAt.plusSeconds(7200), "Sortie mensuelle démo", SALES, null);
            }
            recalculateStock();
        }
    }

    private void seedAuditTrail(Tenant tenant, List<Article> articles) {
        Instant now = Instant.now();
        List<AuditSeed> events = List.of(
                new AuditSeed("USER_LOGIN", "AppUser", 1L, now.minusSeconds(120), ADMIN),
                new AuditSeed("ARTICLE_UPDATED", "Article", articles.get(0).getId(), now.minusSeconds(3600), STOCK),
                new AuditSeed("PURCHASE_ORDER_CREATED", "PurchaseOrder", 1L, now.minusSeconds(7200), ADMIN),
                new AuditSeed("STOCK_MOVEMENT_OUT", "StockMovement", 1L, now.minusSeconds(10_000), SALES),
                new AuditSeed("REPLENISHMENT_CREATED", "ReplenishmentRequest", 1L, now.minusSeconds(15_000), STOCK),
                new AuditSeed("ARTICLE_UPDATED", "Article", articles.get(4).getId(), now.minusSeconds(20_000), ADMIN),
                new AuditSeed("PURCHASE_ORDER_RECEIVED", "PurchaseOrder", 2L, now.minusSeconds(50_000), STOCK),
                new AuditSeed("USER_LOGIN", "AppUser", 2L, now.minusSeconds(80_000), SALES),
                new AuditSeed("STOCK_MOVEMENT_OUT", "StockMovement", 2L, now.minusSeconds(100_000), SALES),
                new AuditSeed("ARTICLE_CREATED", "Article", articles.get(articles.size() - 1).getId(), now.minusSeconds(200_000), ADMIN));
        for (AuditSeed e : events) {
            auditEventRepository.save(AuditEvent.builder()
                    .tenant(tenant)
                    .user(null)
                    .action(e.action())
                    .entityType(e.entityType())
                    .entityId(e.entityId())
                    .payloadJson("{\"demo\":true,\"actor\":\"" + e.actor() + "\"}")
                    .createdAt(e.at())
                    .build());
        }
    }

    private void seedAcmeTenant() {
        if (tenantRepository.findBySlug("acme").isPresent()) {
            return;
        }
        tenantRepository.save(Tenant.builder().slug("acme").name("Acme Retail").build());
    }

    private void createReceivedOrder(
            Tenant tenant,
            String orderNumber,
            Supplier supplier,
            LocalDate orderDate,
            List<LineSeed> lines,
            String receivedBy
    ) {
        PurchaseOrder po = buildOrder(tenant, orderNumber, supplier, orderDate, OrderStatus.RECEIVED, lines);
        po.setTenantReceivedAt(at(orderDate.plusDays(3), 9));
        po.setReceivedBy(receivedBy);
        po = purchaseOrderRepository.save(po);

        Instant receiptAt = at(orderDate.plusDays(3), 10);
        for (PurchaseOrderLine line : po.getLines()) {
            int qty = line.getQuantity();
            line.setQuantityReceived(qty);
            saveInMovement(
                    line.getArticle(),
                    qty,
                    receiptAt,
                    "Commande " + orderNumber + " — " + line.getArticle().getSku(),
                    receivedBy,
                    line,
                    null);
            receiptAt = receiptAt.plusSeconds(300);
        }
        purchaseOrderRepository.save(po);
    }

    private void createOpenOrder(
            Tenant tenant,
            String orderNumber,
            Supplier supplier,
            LocalDate orderDate,
            OrderStatus status,
            List<LineSeed> lines
    ) {
        purchaseOrderRepository.save(buildOrder(tenant, orderNumber, supplier, orderDate, status, lines));
    }

    private PurchaseOrder buildOrder(
            Tenant tenant,
            String orderNumber,
            Supplier supplier,
            LocalDate orderDate,
            OrderStatus status,
            List<LineSeed> lines
    ) {
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;
        List<PurchaseOrderLine> lineEntities = new ArrayList<>();
        for (LineSeed ls : lines) {
            BigDecimal lineTotal = ls.unitPrice().multiply(BigDecimal.valueOf(ls.qty()));
            total = total.add(lineTotal);
            itemCount += ls.qty();
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .article(ls.article())
                    .quantity(ls.qty())
                    .quantityReceived(0)
                    .unitPrice(ls.unitPrice())
                    .build();
            lineEntities.add(line);
        }
        PurchaseOrder po = PurchaseOrder.builder()
                .tenant(tenant)
                .orderNumber(orderNumber)
                .supplier(supplier)
                .orderDate(orderDate)
                .status(status)
                .totalAmount(total.setScale(2, RoundingMode.HALF_UP))
                .lineItemCount(itemCount)
                .lines(new ArrayList<>())
                .build();
        for (PurchaseOrderLine line : lineEntities) {
            line.setPurchaseOrder(po);
            po.getLines().add(line);
        }
        return po;
    }

    private void recordDemoSale(Tenant tenant, String seller, LocalDate date, List<SaleLineSeed> lineSeeds) {
        recalculateStock();
        List<SaleLine> lineEntities = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Instant at = at(date, 11);

        for (SaleLineSeed ls : lineSeeds) {
            Article fresh = reload(ls.article());
            int qty = Math.min(ls.qty(), fresh.getQuantityOnHand());
            if (qty <= 0) {
                continue;
            }
            BigDecimal unit = fresh.getSalePrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            total = total.add(lineTotal);
            lineEntities.add(SaleLine.builder()
                    .article(fresh)
                    .skuSnapshot(fresh.getSku())
                    .nameSnapshot(fresh.getName())
                    .unitPrice(unit)
                    .quantity(qty)
                    .lineTotal(lineTotal)
                    .build());
        }
        if (lineEntities.isEmpty()) {
            return;
        }

        Sale sale = Sale.builder()
                .tenant(tenant)
                .saleNumber("TMP-" + UUID.randomUUID())
                .createdAt(at)
                .soldByEmail(seller)
                .totalAmount(total)
                .lineCount(lineEntities.size())
                .note("Vente démo POS")
                .lines(new ArrayList<>())
                .build();
        for (SaleLine sl : lineEntities) {
            sl.setSale(sale);
            sale.getLines().add(sl);
        }
        saleRepository.saveAndFlush(sale);
        sale.setSaleNumber("INV-" + sale.getId());
        saleRepository.save(sale);

        for (SaleLine sl : sale.getLines()) {
            Article fresh = reload(sl.getArticle());
            fresh.setQuantityOnHand(fresh.getQuantityOnHand() - sl.getQuantity());
            articleRepository.save(fresh);
            stockMovementRepository.save(StockMovement.builder()
                    .article(fresh)
                    .type(MovementType.OUT)
                    .quantity(sl.getQuantity())
                    .note("Vente " + sale.getSaleNumber())
                    .createdAt(at)
                    .createdBy(seller)
                    .saleLine(sl)
                    .build());
        }
    }

    private ReplenishmentRequest saveReplenishment(
            Tenant tenant,
            Article article,
            Supplier supplier,
            int qty,
            ReplenishmentStatus status,
            String note,
            String requestedBy,
            Instant createdAt
    ) {
        return replenishmentRequestRepository.save(ReplenishmentRequest.builder()
                .tenant(tenant)
                .article(article)
                .supplier(supplier)
                .quantity(qty)
                .status(status)
                .note(note)
                .requestedBy(requestedBy)
                .createdAt(createdAt)
                .build());
    }

    private void saveInMovement(
            Article article,
            int qty,
            Instant at,
            String note,
            String by,
            PurchaseOrderLine poLine,
            ReplenishmentRequest replenishment
    ) {
        Article fresh = reload(article);
        fresh.setQuantityOnHand(fresh.getQuantityOnHand() + qty);
        articleRepository.save(fresh);
        stockMovementRepository.save(StockMovement.builder()
                .article(fresh)
                .type(MovementType.IN)
                .quantity(qty)
                .note(note)
                .createdAt(at)
                .createdBy(by)
                .purchaseOrderLine(poLine)
                .replenishmentRequest(replenishment)
                .build());
    }

    private void saveOutMovement(
            Article article,
            int qty,
            Instant at,
            String note,
            String by,
            SaleLine saleLine
    ) {
        Article fresh = reload(article);
        fresh.setQuantityOnHand(Math.max(0, fresh.getQuantityOnHand() - qty));
        articleRepository.save(fresh);
        stockMovementRepository.save(StockMovement.builder()
                .article(fresh)
                .type(MovementType.OUT)
                .quantity(qty)
                .note(note)
                .createdAt(at)
                .createdBy(by)
                .saleLine(saleLine)
                .build());
    }

    private Article reload(Article article) {
        return articleRepository.findById(article.getId()).orElseThrow();
    }

    private void recalculateStock() {
        jdbc.update(
                "UPDATE articles a SET quantity_on_hand = COALESCE(("
                        + "  SELECT SUM(CASE "
                        + "    WHEN m.type IN ('IN','INVENTORY','ADJUSTMENT') THEN m.quantity "
                        + "    WHEN m.type IN ('OUT','TRANSFER') THEN -m.quantity "
                        + "    ELSE 0 END) "
                        + "  FROM stock_movements m WHERE m.article_id = a.id"
                        + "), 0)");
    }

    private static Instant at(LocalDate date, int hour) {
        return date.atTime(hour, 0).toInstant(ZoneOffset.UTC);
    }

    private static LineSeed line(Article article, int qty, double unitPrice) {
        return new LineSeed(article, qty, BigDecimal.valueOf(unitPrice));
    }

    private static SaleLineSeed saleLine(Article article, int qty) {
        return new SaleLineSeed(article, qty);
    }

    private Supplier saveSupplier(
            Tenant tenant,
            String name,
            String email,
            String country,
            int lead,
            SupplierStatus status,
            int products
    ) {
        return supplierRepository.save(Supplier.builder()
                .tenant(tenant)
                .name(name)
                .contactEmail(email)
                .country(country)
                .leadTimeDays(lead)
                .status(status)
                .linkedProductCount(products)
                .archived(false)
                .build());
    }

    private record ArticleSeed(String sku, String name, String category, double salePrice, int min, int max) {}

    private record LineSeed(Article article, int qty, BigDecimal unitPrice) {}

    private record SaleLineSeed(Article article, int qty) {}

    private record AuditSeed(String action, String entityType, Long entityId, Instant at, String actor) {}
}
