package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.Sale;
import com.stowflow.inventra.domain.SaleLine;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.PosDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.AppUserRepository;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.SaleRepository;
import com.stowflow.inventra.security.InventraUserDetails;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PosService {

    private static final DateTimeFormatter ISO_UI = DateTimeFormatter.ISO_INSTANT;
    private static final int MAX_LINES = 100;

    private final SaleRepository saleRepository;
    private final ArticleRepository articleRepository;
    private final StockMovementService stockMovementService;
    private final SaleInvoicePdfService saleInvoicePdfService;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<PosDtos.SellerOption> listCommercialSellerOptions(Tenant tenant) {
        return appUserRepository.findByTenant_IdAndRoleOrderByEmailAsc(tenant.getId(), AppRole.SALES).stream()
                .filter(AppUser::isEnabled)
                .map(u -> new PosDtos.SellerOption(u.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PosDtos.SaleListItem> listSales(
            Tenant tenant, Pageable pageable, String soldByFilter, InventraUserDetails user
    ) {
        AppRole role = user.getRole();
        String email = user.getUsername();
        Page<Sale> page;
        if (role == AppRole.SALES) {
            page = saleRepository.findByTenant_IdAndSoldByEmailIgnoreCaseOrderByCreatedAtDesc(
                    tenant.getId(), email, pageable);
        } else if (role == AppRole.MANAGER || role == AppRole.TENANT_ADMIN || role == AppRole.SUPER_ADMIN) {
            String f = soldByFilter == null ? "" : soldByFilter.trim();
            page = saleRepository.findByTenantForManager(tenant.getId(), f, pageable);
        } else {
            throw new BusinessException("Accès refusé.");
        }
        return page.map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public PosDtos.PosSaleResponse getSale(Tenant tenant, Long saleId, InventraUserDetails user) {
        Sale sale = assertCanView(tenant, saleId, user);
        return toSaleResponse(sale);
    }

    @Transactional(readOnly = true)
    public byte[] invoicePdf(Tenant tenant, Long saleId, InventraUserDetails user) {
        Sale sale = assertCanView(tenant, saleId, user);
        return saleInvoicePdfService.build(sale);
    }

    @Transactional
    public PosDtos.PosSaleResponse recordSale(Tenant tenant, PosDtos.PosSaleRequest req, String actorEmail) {
        Map<Long, Integer> merged = mergeLines(req.lines());
        if (merged.isEmpty()) {
            throw new BusinessException("La vente doit contenir au moins une ligne.");
        }
        if (merged.size() > MAX_LINES) {
            throw new BusinessException("Nombre maximum de lignes distinctes dépassé (" + MAX_LINES + ").");
        }

        List<SaleLine> lineEntities = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> e : merged.entrySet()) {
            Long articleId = e.getKey();
            int qty = e.getValue();
            Article article = articleRepository.findById(articleId).orElseThrow(() -> new BusinessException("Article introuvable."));
            if (!article.getTenant().getId().equals(tenant.getId()) || article.isArchived()) {
                throw new BusinessException("Article introuvable.");
            }
            if (article.getQuantityOnHand() < qty) {
                throw new BusinessException("Stock insuffisant pour « " + article.getName() + " » (RG10).");
            }
            BigDecimal unit = article.getSalePrice() != null ? article.getSalePrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty)).setScale(2, java.math.RoundingMode.HALF_UP);
            total = total.add(lineTotal);
            SaleLine sl = SaleLine.builder()
                    .article(article)
                    .skuSnapshot(article.getSku())
                    .nameSnapshot(article.getName())
                    .unitPrice(unit)
                    .quantity(qty)
                    .lineTotal(lineTotal)
                    .build();
            lineEntities.add(sl);
        }
    // seuil des produits 
    
        total = total.setScale(2, java.math.RoundingMode.HALF_UP);
        String note = req.note() != null && !req.note().isBlank() ? req.note().trim() : null;

        Sale sale = Sale.builder()
                .tenant(tenant)
                .saleNumber("TMP-" + UUID.randomUUID())
                .createdAt(Instant.now())
                .soldByEmail(actorEmail)
                .totalAmount(total)
                .lineCount(lineEntities.size())
                .note(note)
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
            stockMovementService.recordOutboundForSaleLine(sl, actorEmail);
        }

        return toSaleResponse(saleRepository.findByIdAndTenant_Id(sale.getId(), tenant.getId()).orElse(sale));
    }

    private Map<Long, Integer> mergeLines(List<PosDtos.PosSaleLineRequest> lines) {
        Map<Long, Integer> m = new LinkedHashMap<>();
        for (PosDtos.PosSaleLineRequest l : lines) {
            m.merge(l.articleId(), l.quantity(), Integer::sum);
        }
        return m;
    }

    private Sale assertCanView(Tenant tenant, Long saleId, InventraUserDetails user) {
        Sale sale = saleRepository
                .findByIdAndTenant_Id(saleId, tenant.getId())
                .orElseThrow(() -> new BusinessException("Vente introuvable."));
        if (user.getRole() == AppRole.SALES
                && !sale.getSoldByEmail().equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException("Vente introuvable.");
        }
        if (user.getRole() != AppRole.SALES
                && user.getRole() != AppRole.MANAGER
                && user.getRole() != AppRole.TENANT_ADMIN
                && user.getRole() != AppRole.SUPER_ADMIN) {
            throw new BusinessException("Accès refusé.");
        }
        return sale;
    }

    private PosDtos.SaleListItem toListItem(Sale s) {
        return new PosDtos.SaleListItem(
                s.getId(),
                s.getSaleNumber(),
                s.getSoldByEmail(),
                ISO_UI.format(s.getCreatedAt().atOffset(ZoneOffset.UTC)),
                s.getTotalAmount(),
                s.getLineCount());
    }

    private PosDtos.PosSaleResponse toSaleResponse(Sale s) {
        List<PosDtos.PosSaleLineResponse> lines = s.getLines().stream()
                .map(l -> new PosDtos.PosSaleLineResponse(
                        l.getArticle().getId(),
                        l.getSkuSnapshot(),
                        l.getNameSnapshot(),
                        l.getUnitPrice(),
                        l.getQuantity(),
                        l.getLineTotal()))
                .toList();
        return new PosDtos.PosSaleResponse(
                s.getId(),
                s.getSaleNumber(),
                s.getTotalAmount(),
                ISO_UI.format(s.getCreatedAt().atOffset(ZoneOffset.UTC)),
                s.getSoldByEmail(),
                lines,
                s.getNote());
    }
}
