package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.OrderStatus;
import com.stowflow.inventra.domain.PurchaseOrder;
import com.stowflow.inventra.domain.PurchaseOrderLine;
import com.stowflow.inventra.domain.Supplier;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.OrderDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.PurchaseOrderRepository;
import com.stowflow.inventra.repo.SupplierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.stowflow.inventra.util.MoneyFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ArticleRepository articleRepository;
    private final StockMovementService stockMovementService;
    private final ProcurementPolicy procurementPolicy;

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> listForTenant(Tenant tenant, String statusFilter, String search) {
        return filterAndMap(purchaseOrderRepository.findByTenantWithSupplierAndLines(tenant.getId()), statusFilter, search);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> listForSupplier(Tenant tenant, Long supplierId, String statusFilter, String search) {
        loadSupplier(tenant, supplierId);
        return filterAndMap(purchaseOrderRepository.findBySupplierIdWithLines(supplierId), statusFilter, search);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderDetailResponse getForTenant(Tenant tenant, Long orderId) {
        PurchaseOrder po = purchaseOrderRepository
                .findDetailByIdAndTenantId(orderId, tenant.getId())
                .orElseThrow(() -> new BusinessException("Commande introuvable."));
        return toDetailResponse(po);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderSummary summary(Tenant tenant) {
        long total = purchaseOrderRepository.countByTenantId(tenant.getId());
        long pending = purchaseOrderRepository.countByTenantIdAndStatus(tenant.getId(), OrderStatus.PENDING);
        long shipped = purchaseOrderRepository.countByTenantIdAndStatus(tenant.getId(), OrderStatus.SHIPPED);
        long received = purchaseOrderRepository.countByTenantIdAndStatus(tenant.getId(), OrderStatus.RECEIVED);
        long cancelled = purchaseOrderRepository.countByTenantIdAndStatus(tenant.getId(), OrderStatus.CANCELLED);
        return new OrderDtos.OrderSummary(total, pending, shipped, received, cancelled);
    }

    @Transactional
    public OrderDtos.OrderDetailResponse create(Tenant tenant, OrderDtos.CreateOrderRequest req) {
        Supplier supplier = loadSupplier(tenant, req.supplierId());
        if (supplier.getStatus() != com.stowflow.inventra.domain.SupplierStatus.ACTIVE) {
            throw new BusinessException("Le fournisseur doit être actif pour recevoir une commande.");
        }
        List<OrderDtos.CreateOrderLineRequest> lineReqs = req.lines();
        if (lineReqs.isEmpty()) {
            throw new BusinessException("Ajoutez au moins une ligne produit à la commande.");
        }

        String orderNumber = nextOrderNumber(tenant);
        PurchaseOrder po = PurchaseOrder.builder()
                .tenant(tenant)
                .orderNumber(orderNumber)
                .supplier(supplier)
                .orderDate(req.orderDate() != null ? req.orderDate() : LocalDate.now())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .lineItemCount(0)
                .lines(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        int lineCount = 0;
        for (OrderDtos.CreateOrderLineRequest lr : lineReqs) {
            Article article = loadArticle(tenant, lr.articleId());
            String reject = procurementPolicy.purchaseOrderRejectionReason(article);
            if (reject != null) {
                throw new BusinessException(reject);
            }
            int qty = lr.quantity();
            if (qty < 1) {
                throw new BusinessException("Quantité commandée invalide pour " + article.getSku() + ".");
            }
            procurementPolicy.assertOrderQuantityWithinMax(article, qty);
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .purchaseOrder(po)
                    .article(article)
                    .quantity(qty)
                    .quantityReceived(0)
                    .unitPrice(lr.unitPrice())
                    .build();
            po.getLines().add(line);
            total = total.add(lr.unitPrice().multiply(BigDecimal.valueOf(qty)));
            lineCount += qty;
        }
        po.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        po.setLineItemCount(lineCount);
        po = purchaseOrderRepository.save(po);
        return toDetailResponse(po);
    }

    @Transactional
    public OrderDtos.OrderResponse ship(Tenant tenant, Long orderId, AppRole actorRole) {
        assertStockOperations(actorRole);
        PurchaseOrder po = loadForTenant(tenant, orderId);
        if (!PurchaseOrderWorkflow.canShip(po.getStatus())) {
            throw new BusinessException("Seules les commandes en attente peuvent être marquées expédiées.");
        }
        po.setStatus(OrderStatus.SHIPPED);
        return toResponse(purchaseOrderRepository.save(po));
    }

    @Transactional
    public OrderDtos.OrderResponse tenantCancel(Tenant tenant, Long orderId, AppRole actorRole) {
        PurchaseOrder po = loadForTenant(tenant, orderId);
        if (!PurchaseOrderWorkflow.isTenantCancelAllowed(po.getStatus(), OrderStatus.CANCELLED, actorRole)) {
            throw new BusinessException("Annulation non autorisée pour cette commande.");
        }
        po.setStatus(OrderStatus.CANCELLED);
        return toResponse(purchaseOrderRepository.save(po));
    }

    @Transactional
    public OrderDtos.OrderDetailResponse receive(
            Tenant tenant,
            Long orderId,
            OrderDtos.ReceiveOrderRequest req,
            String actorEmail,
            AppRole actorRole
    ) {
        assertStockOperations(actorRole);
        PurchaseOrder po = purchaseOrderRepository
                .findDetailByIdAndTenantId(orderId, tenant.getId())
                .orElseThrow(() -> new BusinessException("Commande introuvable."));
        if (!PurchaseOrderWorkflow.canReceive(po.getStatus())) {
            throw new BusinessException(
                    "La réception n’est possible que pour une commande expédiée (statut Shipped).");
        }
        if (po.getLines().isEmpty()) {
            throw new BusinessException("Commande sans lignes.");
        }

        List<OrderDtos.ReceiveOrderLineRequest> lineReqs = resolveReceiveLines(po, req);
        Map<Long, PurchaseOrderLine> lineById =
                po.getLines().stream().collect(Collectors.toMap(PurchaseOrderLine::getId, l -> l));
        int totalReceivedNow = 0;
        for (OrderDtos.ReceiveOrderLineRequest rl : lineReqs) {
            PurchaseOrderLine line = lineById.get(rl.lineId());
            if (line == null) {
                throw new BusinessException("Ligne de commande introuvable : " + rl.lineId());
            }
            int qty = rl.quantityReceived();
            if (qty < 0) {
                throw new BusinessException("Quantité reçue invalide.");
            }
            int already = line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
            int remaining = line.getQuantity() - already;
            if (qty > remaining) {
                throw new BusinessException(
                        "Quantité reçue supérieure au reste à recevoir pour " + line.getArticle().getSku() + ".");
            }
            if (qty == 0) {
                continue;
            }
            String note = "Commande " + po.getOrderNumber() + " — ligne " + line.getArticle().getSku();
            stockMovementService.recordPurchaseOrderReceipt(line, qty, note, actorEmail);
            line.setQuantityReceived(already + qty);
            totalReceivedNow += qty;
        }
        if (totalReceivedNow == 0) {
            throw new BusinessException("Indiquez au moins une quantité reçue.");
        }

        boolean allReceived = po.getLines().stream()
                .allMatch(l -> (l.getQuantityReceived() != null ? l.getQuantityReceived() : 0) >= l.getQuantity());
        if (allReceived) {
            po.setStatus(OrderStatus.RECEIVED);
            po.setTenantReceivedAt(Instant.now());
            po.setReceivedBy(actorEmail);
        }
        po = purchaseOrderRepository.save(po);
        return toDetailResponse(po);
    }

    /**
     * Si aucune ligne n’est fournie, réception automatique de toutes les quantités restantes
     * (mise à jour stock et résolution des alertes associées).
     */
    private static List<OrderDtos.ReceiveOrderLineRequest> resolveReceiveLines(
            PurchaseOrder po, OrderDtos.ReceiveOrderRequest req
    ) {
        if (req.lines() != null && !req.lines().isEmpty()) {
            return req.lines();
        }
        return po.getLines().stream()
                .map(l -> {
                    int already = l.getQuantityReceived() != null ? l.getQuantityReceived() : 0;
                    int remaining = l.getQuantity() - already;
                    return new OrderDtos.ReceiveOrderLineRequest(l.getId(), remaining);
                })
                .filter(r -> r.quantityReceived() > 0)
                .toList();
    }

    private static void assertStockOperations(AppRole role) {
        if (role == AppRole.STOCK_MANAGER) {
            return;
        }
        throw new BusinessException("Action réservée au gestionnaire de stock.");
    }

    private String nextOrderNumber(Tenant tenant) {
        long count = purchaseOrderRepository.countByTenantId(tenant.getId());
        return "ORD-" + String.format(Locale.ROOT, "%04d", count + 4821);
    }

    private List<OrderDtos.OrderResponse> filterAndMap(
            List<PurchaseOrder> orders, String statusFilter, String search
    ) {
        String sf = statusFilter == null || statusFilter.isBlank() || "All Orders".equalsIgnoreCase(statusFilter)
                ? null
                : statusFilter;
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return orders.stream()
                .filter(o -> sf == null || formatOrderStatus(o.getStatus()).equals(sf))
                .filter(o -> q.isEmpty()
                        || o.getOrderNumber().toLowerCase(Locale.ROOT).contains(q)
                        || o.getSupplier().getName().toLowerCase(Locale.ROOT).contains(q))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PurchaseOrder loadForTenant(Tenant tenant, Long orderId) {
        return purchaseOrderRepository
                .findByIdAndTenantId(orderId, tenant.getId())
                .orElseThrow(() -> new BusinessException("Commande introuvable."));
    }

    private Supplier loadSupplier(Tenant tenant, Long supplierId) {
        Supplier s = supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable."));
        if (!s.getTenant().getId().equals(tenant.getId()) || s.isArchived()) {
            throw new BusinessException("Fournisseur introuvable.");
        }
        return s;
    }

    private Article loadArticle(Tenant tenant, Long articleId) {
        Article a = articleRepository
                .findById(articleId)
                .orElseThrow(() -> new BusinessException("Article introuvable."));
        if (!a.getTenant().getId().equals(tenant.getId()) || a.isArchived()) {
            throw new BusinessException("Article introuvable.");
        }
        return a;
    }

    public static OrderStatus parseOrderStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Statut requis.");
        }
        String s = raw.trim();
        try {
            return OrderStatus.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "pending", "en attente" -> OrderStatus.PENDING;
            case "confirmed", "confirmée", "confirmee" -> OrderStatus.PENDING;
            case "processing", "en cours" -> OrderStatus.PENDING;
            case "shipped", "expédiée", "expediee" -> OrderStatus.SHIPPED;
            case "delivered", "received", "reçue", "recue", "réceptionnée", "receptionnee" -> OrderStatus.RECEIVED;
            case "cancelled", "annulée", "annulee" -> OrderStatus.CANCELLED;
            default -> throw new BusinessException("Statut inconnu: " + raw);
        };
    }

    private OrderDtos.OrderResponse toResponse(PurchaseOrder o) {
        return new OrderDtos.OrderResponse(
                o.getId(),
                "#" + o.getOrderNumber(),
                o.getSupplier().getName(),
                o.getSupplier().getId(),
                UI_DATE.format(o.getOrderDate()),
                o.getLineItemCount(),
                MoneyFormat.format(o.getTotalAmount()),
                formatOrderStatus(o.getStatus()),
                o.getTenantReceivedAt() != null
        );
    }

    private OrderDtos.OrderDetailResponse toDetailResponse(PurchaseOrder o) {
        List<OrderDtos.OrderLineResponse> lines = o.getLines().stream()
                .map(l -> new OrderDtos.OrderLineResponse(
                        l.getId(),
                        l.getArticle().getId(),
                        l.getArticle().getSku(),
                        l.getArticle().getName(),
                        l.getQuantity(),
                        l.getQuantityReceived() != null ? l.getQuantityReceived() : 0,
                        l.getUnitPrice()))
                .toList();
        return new OrderDtos.OrderDetailResponse(
                o.getId(),
                "#" + o.getOrderNumber(),
                o.getSupplier().getName(),
                o.getSupplier().getId(),
                UI_DATE.format(o.getOrderDate()),
                MoneyFormat.format(o.getTotalAmount()),
                formatOrderStatus(o.getStatus()),
                o.getTenantReceivedAt() != null,
                o.getReceivedBy() != null ? o.getReceivedBy() : "",
                lines
        );
    }

    static String formatOrderStatus(OrderStatus s) {
        return switch (s) {
            case PENDING, CONFIRMED, PROCESSING -> "Pending";
            case SHIPPED -> "Shipped";
            case RECEIVED -> "Received";
            case CANCELLED -> "Cancelled";
        };
    }
}
