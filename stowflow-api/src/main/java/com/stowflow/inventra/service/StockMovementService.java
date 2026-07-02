package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.MovementType;
import com.stowflow.inventra.domain.PurchaseOrderLine;
import com.stowflow.inventra.domain.ReplenishmentRequest;
import com.stowflow.inventra.domain.Sale;
import com.stowflow.inventra.domain.SaleLine;
import com.stowflow.inventra.domain.StockMovement;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.MovementDtos;
import com.stowflow.inventra.dto.PageDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.StockMovementRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter ISO_UI = DateTimeFormatter.ISO_INSTANT;

    private final StockMovementRepository movementRepository;
    private final ArticleRepository articleRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageDtos.PageResponse<MovementDtos.MovementResponse> list(Tenant tenant, int page, int size) {
        int cappedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<StockMovement> result =
                movementRepository.findPageByTenantOrderByCreatedAtDesc(tenant.getId(), PageRequest.of(page, cappedSize));
        return PageDtos.PageResponse.from(result.map(this::toResponse));
    }

    @Transactional
    public MovementDtos.MovementResponse record(
            Tenant tenant,
            MovementDtos.CreateMovementRequest req,
            String username
    ) {
        if (req.type() == MovementType.IN) {
            throw new BusinessException(
                    "Entrée stock directe désactivée. Créez une demande de réapprovisionnement, puis marquez-la comme reçue.");
        }
        Article article = articleRepository.findById(req.articleId()).orElseThrow(() -> new BusinessException("Article introuvable."));
        if (!article.getTenant().getId().equals(tenant.getId()) || article.isArchived()) {
            throw new BusinessException("Article introuvable.");
        }
        int delta = switch (req.type()) {
            case IN, INVENTORY, ADJUSTMENT -> req.quantity();
            case OUT, TRANSFER -> -req.quantity();
        };
        if (req.type() == MovementType.OUT || req.type() == MovementType.TRANSFER) {
            if (article.getQuantityOnHand() < req.quantity()) {
                throw new BusinessException("Stock insuffisant pour cette sortie (RG10).");
            }
        }
        int newQty = article.getQuantityOnHand() + delta;
        if (newQty < 0) {
            throw new BusinessException("Stock insuffisant pour cette opération (RG10).");
        }
        article.setQuantityOnHand(newQty);
        articleRepository.save(article);

        StockMovement m = StockMovement.builder()
                .article(article)
                .type(req.type())
                .quantity(req.quantity())
                .note(req.note())
                .createdAt(Instant.now())
                .createdBy(username != null && !username.isBlank() ? username : "system")
                .saleLine(null)
                .build();
        StockMovement saved = movementRepository.save(m);

        auditService.log(
                tenant,
                username,
                "STOCK_MOVEMENT_" + req.type().name(),
                "StockMovement",
                saved.getId(),
                "{\"articleId\":" + article.getId() + ",\"sku\":\"" + article.getSku() + "\",\"quantity\":" + req.quantity() + "}");

        return toResponse(saved);
    }

    /** Réception commande fournisseur — seule source d’entrée stock pour les commandes (traçabilité ligne PO). */
    @Transactional
    public void recordPurchaseOrderReceipt(
            PurchaseOrderLine orderLine,
            int quantity,
            String note,
            String username
    ) {
        if (quantity < 1) {
            throw new BusinessException("Quantité invalide.");
        }
        int alreadyIn = movementRepository.sumInQuantityByPurchaseOrderLineId(orderLine.getId());
        int ordered = orderLine.getQuantity();
        if (alreadyIn + quantity > ordered) {
            throw new BusinessException(
                    "Quantité reçue supérieure au reste commandé pour " + orderLine.getArticle().getSku() + ".");
        }
        Article fresh = articleRepository
                .findById(orderLine.getArticle().getId())
                .orElseThrow(() -> new BusinessException("Article introuvable."));
        int newQty = fresh.getQuantityOnHand() + quantity;
        if (newQty > fresh.getMaxThreshold()) {
            throw new BusinessException(
                    "Réception impossible pour « "
                            + fresh.getSku()
                            + " » : le stock passerait à "
                            + newQty
                            + " (plafond max "
                            + fresh.getMaxThreshold()
                            + ").");
        }
        fresh.setQuantityOnHand(newQty);
        articleRepository.save(fresh);
        StockMovement m = StockMovement.builder()
                .article(fresh)
                .type(MovementType.IN)
                .quantity(quantity)
                .note(note)
                .createdAt(Instant.now())
                .createdBy(username != null && !username.isBlank() ? username : "system")
                .purchaseOrderLine(orderLine)
                .build();
        movementRepository.save(m);
    }

    /** Réception réapprovisionnement — entrée stock liée à la demande (traçabilité). */
    @Transactional
    public void recordReplenishmentReceipt(
            ReplenishmentRequest request,
            int quantity,
            String note,
            String username
    ) {
        if (quantity < 1) {
            throw new BusinessException("Quantité invalide.");
        }
        if (movementRepository.existsByReplenishmentRequest_Id(request.getId())) {
            throw new BusinessException("Cette demande a déjà été réceptionnée en stock.");
        }
        Article fresh = articleRepository
                .findById(request.getArticle().getId())
                .orElseThrow(() -> new BusinessException("Article introuvable."));
        int newQty = fresh.getQuantityOnHand() + quantity;
        if (newQty > fresh.getMaxThreshold()) {
            throw new BusinessException(
                    "Réception impossible pour « "
                            + fresh.getSku()
                            + " » : le stock passerait à "
                            + newQty
                            + " (plafond max "
                            + fresh.getMaxThreshold()
                            + ").");
        }
        fresh.setQuantityOnHand(newQty);
        articleRepository.save(fresh);
        StockMovement m = StockMovement.builder()
                .article(fresh)
                .type(MovementType.IN)
                .quantity(quantity)
                .note(note)
                .createdAt(Instant.now())
                .createdBy(username != null && !username.isBlank() ? username : "system")
                .replenishmentRequest(request)
                .build();
        movementRepository.save(m);
    }

    /**
     * Sortie stock liée à une ligne de vente POS (traçabilité {@link StockMovement#getSaleLine()}).
     */
    @Transactional
    public void recordOutboundForSaleLine(SaleLine saleLine, String actorEmail) {
        Article article = saleLine.getArticle();
        int qty = saleLine.getQuantity();
        if (article.getQuantityOnHand() < qty) {
            throw new BusinessException("Stock insuffisant pour cette sortie (RG10).");
        }
        article.setQuantityOnHand(article.getQuantityOnHand() - qty);
        articleRepository.save(article);
        Sale sale = saleLine.getSale();
        String note = "Vente " + sale.getSaleNumber();
        StockMovement m = StockMovement.builder()
                .article(article)
                .type(MovementType.OUT)
                .quantity(qty)
                .note(note)
                .createdAt(Instant.now())
                .createdBy(actorEmail != null && !actorEmail.isBlank() ? actorEmail : "system")
                .saleLine(saleLine)
                .build();
        movementRepository.save(m);
    }

    private MovementDtos.MovementResponse toResponse(StockMovement m) {
        Article a = m.getArticle();
        Long saleId = null;
        String saleNumber = null;
        if (m.getSaleLine() != null && m.getSaleLine().getSale() != null) {
            saleId = m.getSaleLine().getSale().getId();
            saleNumber = m.getSaleLine().getSale().getSaleNumber();
        }
        return new MovementDtos.MovementResponse(
                m.getId(),
                a.getId(),
                a.getSku(),
                a.getName(),
                m.getType(),
                m.getQuantity(),
                m.getNote(),
                ISO_UI.format(m.getCreatedAt().atOffset(ZoneOffset.UTC)),
                m.getCreatedBy(),
                saleId,
                saleNumber
        );
    }
}
