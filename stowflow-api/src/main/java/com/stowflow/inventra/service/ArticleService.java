package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.ArticleDtos;
import com.stowflow.inventra.dto.PageDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.service.ProcurementPolicy.BatchContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ArticleRepository articleRepository;
    private final ProcurementPolicy procurementPolicy;

    @Transactional(readOnly = true)
    public PageDtos.PageResponse<ArticleDtos.ArticleResponse> list(
            Tenant tenant,
            String search,
            String statusFilter,
            String category,
            int page,
            int size
    ) {
        int cappedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String sf = statusFilter == null || statusFilter.isBlank() || "All".equalsIgnoreCase(statusFilter)
                ? null
                : statusFilter;
        String cat = category == null || category.isBlank() ? null : category.trim();

        Page<Article> pageResult = articleRepository.findActiveByTenantIdFiltered(
                tenant.getId(), q.isEmpty() ? null : q, cat, PageRequest.of(page, cappedSize));

        List<Article> articles = pageResult.getContent().stream()
                .filter(a -> sf == null || catalogStatus(a).equals(sf))
                .collect(Collectors.toList());

        BatchContext batchContext = procurementPolicy.batchContext(
                articles.stream().map(Article::getId).collect(Collectors.toList()));

        List<ArticleDtos.ArticleResponse> content = articles.stream()
                .map(a -> toResponse(a, batchContext))
                .collect(Collectors.toList());

        return PageDtos.PageResponse.of(content, pageResult.getTotalElements(), page, cappedSize);
    }

    @Transactional(readOnly = true)
    public ArticleDtos.ArticleResponse get(Tenant tenant, Long id) {
        Article a = loadOwned(tenant, id);
        BatchContext batchContext = procurementPolicy.batchContext(List.of(a.getId()));
        return toResponse(a, batchContext);
    }

    @Transactional
    public ArticleDtos.ArticleResponse create(Tenant tenant, ArticleDtos.CreateArticleRequest req) {
        if (req.minThreshold() >= req.maxThreshold()) {
            throw new BusinessException("Le seuil minimum doit être strictement inférieur au maximum (RG02).");
        }
        String sku = req.sku().trim();
        if (articleRepository.existsByTenantIdAndSkuAndArchivedFalse(tenant.getId(), sku)) {
            throw new BusinessException("La référence article est unique par tenant (RG01).");
        }
        BigDecimal purchase = req.purchasePrice() != null ? req.purchasePrice() : BigDecimal.ZERO;
        String uom = req.unitOfMeasure() != null && !req.unitOfMeasure().isBlank() ? req.unitOfMeasure().trim() : "unit";
        Article a = Article.builder()
                .tenant(tenant)
                .sku(sku)
                .name(req.name().trim())
                .category(req.category() != null ? req.category().trim() : "")
                .unitOfMeasure(uom)
                .purchasePrice(purchase)
                .salePrice(req.salePrice())
                .minThreshold(req.minThreshold())
                .maxThreshold(req.maxThreshold())
                .quantityOnHand(0)
                .description(req.description())
                .archived(false)
                .build();
        BatchContext batchContext = procurementPolicy.batchContext(List.of());
        return toResponse(articleRepository.save(a), batchContext);
    }

    @Transactional
    public ArticleDtos.ArticleResponse update(Tenant tenant, Long id, ArticleDtos.UpdateArticleRequest req) {
        Article a = loadOwned(tenant, id);
        if (req.name() != null && !req.name().isBlank()) {
            a.setName(req.name().trim());
        }
        if (req.category() != null) {
            a.setCategory(req.category().trim());
        }
        if (req.salePrice() != null) {
            a.setSalePrice(req.salePrice());
        }
        if (req.purchasePrice() != null) {
            a.setPurchasePrice(req.purchasePrice());
        }
        if (req.minThreshold() != null) {
            a.setMinThreshold(req.minThreshold());
        }
        if (req.maxThreshold() != null) {
            a.setMaxThreshold(req.maxThreshold());
        }
        if (req.description() != null) {
            a.setDescription(req.description());
        }
        if (a.getMinThreshold() >= a.getMaxThreshold()) {
            throw new BusinessException("Le seuil minimum doit être strictement inférieur au maximum (RG02).");
        }
        BatchContext batchContext = procurementPolicy.batchContext(List.of(a.getId()));
        return toResponse(articleRepository.save(a), batchContext);
    }

    @Transactional
    public void archive(Tenant tenant, Long id) {
        Article a = loadOwned(tenant, id);
        a.setArchived(true);
    }

    public static String catalogStatus(Article a) {
        int q = a.getQuantityOnHand();
        if (q == 0) {
            return "Out of Stock";
        }
        if (q < a.getMinThreshold()) {
            return "Low Stock";
        }
        return "In Stock";
    }

    private Article loadOwned(Tenant tenant, Long id) {
        Article a = articleRepository.findById(id).orElseThrow(() -> new BusinessException("Article introuvable."));
        if (!a.getTenant().getId().equals(tenant.getId())) {
            throw new BusinessException("Article introuvable.");
        }
        if (a.isArchived()) {
            throw new BusinessException("Article archivé.");
        }
        return a;
    }

    private ArticleDtos.ArticleResponse toResponse(Article a, BatchContext batchContext) {
        return new ArticleDtos.ArticleResponse(
                a.getId(),
                a.getName(),
                a.getSku(),
                a.getCategory(),
                a.getQuantityOnHand(),
                a.getSalePrice(),
                catalogStatus(a),
                a.getMinThreshold(),
                a.getMaxThreshold(),
                a.getDescription(),
                procurementPolicy.isNewProduct(a, batchContext),
                procurementPolicy.eligibleForPurchaseOrder(a, batchContext),
                procurementPolicy.eligibleForReplenishment(a, batchContext),
                procurementPolicy.suggestedPurchaseOrderQuantity(a, batchContext),
                procurementPolicy.suggestedReplenishmentQuantity(a, batchContext)
        );
    }
}
