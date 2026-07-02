package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.ReplenishmentRequest;
import com.stowflow.inventra.domain.ReplenishmentStatus;
import com.stowflow.inventra.domain.Supplier;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.ReplenishmentDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.ReplenishmentRequestRepository;
import com.stowflow.inventra.repo.SupplierRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplenishmentRequestService {

    private static final DateTimeFormatter UI_INSTANT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final ReplenishmentRequestRepository replenishmentRepository;
    private final ArticleRepository articleRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementService stockMovementService;
    private final ProcurementPolicy procurementPolicy;

    @Transactional(readOnly = true)
    public List<ReplenishmentDtos.ReplenishmentRow> list(Tenant tenant, String statusFilter) {
        ReplenishmentStatus sf = parseStatusFilter(statusFilter);
        return replenishmentRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId()).stream()
                .filter(r -> sf == null || r.getStatus() == sf)
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReplenishmentDtos.ReplenishmentSummary summary(Tenant tenant) {
        return new ReplenishmentDtos.ReplenishmentSummary(
                replenishmentRepository.countByTenant_IdAndStatus(tenant.getId(), ReplenishmentStatus.PENDING),
                replenishmentRepository.countByTenant_IdAndStatus(tenant.getId(), ReplenishmentStatus.IN_PROGRESS),
                replenishmentRepository.countByTenant_IdAndStatus(tenant.getId(), ReplenishmentStatus.RECEIVED));
    }

    @Transactional
    public ReplenishmentDtos.ReplenishmentRow create(
            Tenant tenant,
            ReplenishmentDtos.CreateReplenishmentRequest req,
            String actorEmail,
            AppRole actorRole
    ) {
        assertCanCreate(actorRole);
        Article article = loadArticle(tenant, req.articleId());
        String reject = procurementPolicy.replenishmentRejectionReason(article);
        if (reject != null) {
            throw new BusinessException(reject);
        }
        int qty = req.quantity() != null && req.quantity() > 0
                ? req.quantity()
                : procurementPolicy.suggestedReplenishmentQuantity(article);
        if (qty < 1) {
            throw new BusinessException("Quantité de réapprovisionnement invalide pour cet article.");
        }
        procurementPolicy.assertOrderQuantityWithinMax(article, qty);
        Supplier supplier = loadSupplier(tenant, req.supplierId());
        ReplenishmentRequest r = ReplenishmentRequest.builder()
                .tenant(tenant)
                .article(article)
                .supplier(supplier)
                .quantity(qty)
                .status(ReplenishmentStatus.PENDING)
                .note(trimNote(req.note()))
                .requestedBy(actorEmail)
                .createdAt(Instant.now())
                .build();
        return toRow(replenishmentRepository.save(r));
    }

    @Transactional
    public ReplenishmentDtos.ReplenishmentRow updateStatus(
            Tenant tenant,
            Long id,
            String statusLabel,
            String actorEmail,
            AppRole actorRole
    ) {
        assertCanProcess(actorRole);
        ReplenishmentRequest r = loadOwned(tenant, id);
        ReplenishmentStatus next = parseStatus(statusLabel);
        if (next == ReplenishmentStatus.RECEIVED) {
            throw new BusinessException(
                    "Pour réceptionner la marchandise, utilisez l’action « Marquer comme reçue ».");
        }
        applyStatusChange(r, next, actorEmail);
        return toRow(replenishmentRepository.save(r));
    }

    @Transactional
    public ReplenishmentDtos.ReplenishmentRow receive(Tenant tenant, Long id, String actorEmail, AppRole actorRole) {
        assertCanProcess(actorRole);
        ReplenishmentRequest r = loadOwned(tenant, id);
        if (r.getStatus() == ReplenishmentStatus.RECEIVED) {
            throw new BusinessException("Cette demande est déjà réglée.");
        }
        if (r.getStatus() != ReplenishmentStatus.IN_PROGRESS && r.getStatus() != ReplenishmentStatus.PENDING) {
            throw new BusinessException("Seules les demandes en attente ou en cours peuvent être réceptionnées.");
        }
        int qty = r.getQuantity();
        if (qty < 1) {
            throw new BusinessException("Quantité de réapprovisionnement invalide pour cette demande.");
        }
        String ref = reference(r);
        String note = "Réappro. " + ref + (r.getNote() != null ? " — " + r.getNote() : "");
        stockMovementService.recordReplenishmentReceipt(r, qty, note, actorEmail);
        r.setStatus(ReplenishmentStatus.RECEIVED);
        r.setReceivedBy(actorEmail);
        r.setReceivedAt(Instant.now());
        if (r.getProcessedBy() == null) {
            r.setProcessedBy(actorEmail);
            r.setProcessedAt(Instant.now());
        }
        return toRow(replenishmentRepository.save(r));
    }

    private void applyStatusChange(ReplenishmentRequest r, ReplenishmentStatus next, String actorEmail) {
        if (r.getStatus() == ReplenishmentStatus.RECEIVED
                || r.getStatus() == ReplenishmentStatus.REJECTED
                || r.getStatus() == ReplenishmentStatus.CANCELLED) {
            throw new BusinessException("Cette demande est terminée et ne peut plus être modifiée.");
        }
        if (r.getStatus() == next) {
            return;
        }
        switch (next) {
            case PENDING -> throw new BusinessException("Impossible de revenir à « en attente ».");
            case IN_PROGRESS -> {
                if (r.getStatus() != ReplenishmentStatus.PENDING) {
                    throw new BusinessException("Transition non autorisée vers « en cours ».");
                }
                r.setProcessedBy(actorEmail);
                r.setProcessedAt(Instant.now());
            }
            case REJECTED, CANCELLED -> {
                if (r.getStatus() != ReplenishmentStatus.PENDING && r.getStatus() != ReplenishmentStatus.IN_PROGRESS) {
                    throw new BusinessException("Transition non autorisée.");
                }
            }
            case RECEIVED -> throw new BusinessException("Utilisez l’action de réception.");
            default -> throw new BusinessException("Statut inconnu.");
        }
        r.setStatus(next);
    }

    private static void assertCanCreate(AppRole role) {
        if (role == AppRole.STOCK_MANAGER || role == AppRole.MANAGER) {
            return;
        }
        throw new BusinessException("Vous ne pouvez pas créer de demande de réapprovisionnement.");
    }

    private static void assertCanProcess(AppRole role) {
        if (role == AppRole.STOCK_MANAGER) {
            return;
        }
        throw new BusinessException("Seul le gestionnaire de stock peut traiter une demande.");
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

    private Supplier loadSupplier(Tenant tenant, Long supplierId) {
        Supplier s = supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable."));
        if (!s.getTenant().getId().equals(tenant.getId()) || s.isArchived()) {
            throw new BusinessException("Fournisseur introuvable.");
        }
        return s;
    }

    private ReplenishmentRequest loadOwned(Tenant tenant, Long id) {
        return replenishmentRepository
                .findByIdAndTenant_Id(id, tenant.getId())
                .orElseThrow(() -> new BusinessException("Demande introuvable."));
    }

    private ReplenishmentDtos.ReplenishmentRow toRow(ReplenishmentRequest r) {
        return new ReplenishmentDtos.ReplenishmentRow(
                r.getId(),
                reference(r),
                r.getArticle().getId(),
                r.getArticle().getName(),
                r.getArticle().getSku(),
                r.getSupplier().getId(),
                r.getSupplier().getName(),
                r.getQuantity(),
                formatStatus(r.getStatus()),
                r.getNote() != null ? r.getNote() : "",
                r.getRequestedBy(),
                UI_INSTANT.format(r.getCreatedAt()),
                r.getProcessedBy() != null ? r.getProcessedBy() : "",
                r.getReceivedBy() != null ? r.getReceivedBy() : "");
    }

    static String reference(ReplenishmentRequest r) {
        return "REP-" + String.format(Locale.ROOT, "%05d", r.getId());
    }

    static String formatStatus(ReplenishmentStatus s) {
        return switch (s) {
            case PENDING -> "Pending";
            case IN_PROGRESS -> "In Progress";
            case RECEIVED -> "Received";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    private static ReplenishmentStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Statut requis.");
        }
        String s = raw.trim();
        try {
            return ReplenishmentStatus.valueOf(s.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "pending", "en attente" -> ReplenishmentStatus.PENDING;
            case "in progress", "in_progress", "en cours" -> ReplenishmentStatus.IN_PROGRESS;
            case "received", "reçue", "recue", "réglée", "reglee" -> ReplenishmentStatus.RECEIVED;
            case "rejected", "refusée", "refusee" -> ReplenishmentStatus.REJECTED;
            case "cancelled", "annulée", "annulee" -> ReplenishmentStatus.CANCELLED;
            default -> throw new BusinessException("Statut inconnu : " + raw);
        };
    }

    private static ReplenishmentStatus parseStatusFilter(String raw) {
        if (raw == null || raw.isBlank() || "All".equalsIgnoreCase(raw) || "Toutes".equalsIgnoreCase(raw)) {
            return null;
        }
        return parseStatus(raw);
    }

    private static String trimNote(String note) {
        if (note == null) {
            return null;
        }
        String t = note.trim();
        return t.isEmpty() ? null : t;
    }
}


