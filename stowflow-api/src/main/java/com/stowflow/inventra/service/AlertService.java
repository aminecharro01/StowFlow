package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.AlertDtos;
import com.stowflow.inventra.repo.ArticleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final ArticleRepository articleRepository;
    private final ProcurementPolicy procurementPolicy;

    @Transactional(readOnly = true)
    public List<AlertDtos.AlertRow> list(Tenant tenant, String statusFilter) {
        String sf = statusFilter == null || statusFilter.isBlank() || "All".equalsIgnoreCase(statusFilter)
                ? null
                : statusFilter;
        return articleRepository.findActiveByTenantId(tenant.getId()).stream()
                .filter(this::needsAttention)
                .filter(a -> sf == null || alertStatus(a).equals(sf))
                .sorted(Comparator.comparingInt(Article::getQuantityOnHand))
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertDtos.AlertSummary summary(Tenant tenant) {
        List<Article> all = articleRepository.findActiveByTenantId(tenant.getId());
        long out = all.stream().filter(a -> a.getQuantityOnHand() == 0).count();
        long crit = all.stream().filter(a -> a.getQuantityOnHand() > 0 && a.getQuantityOnHand() < 6).count();
        long low = all.stream()
                .filter(a -> a.getQuantityOnHand() >= 6 && a.getQuantityOnHand() < a.getMinThreshold())
                .count();
        long healthy = all.stream().filter(a -> a.getQuantityOnHand() >= a.getMinThreshold()).count();
        return new AlertDtos.AlertSummary(out, crit, low, healthy);
    }

    private boolean needsAttention(Article a) {
        return a.getQuantityOnHand() < a.getMinThreshold() || a.getQuantityOnHand() == 0;
    }

    private String alertStatus(Article a) {
        int q = a.getQuantityOnHand();
        if (q == 0) {
            return "Out of Stock";
        }
        if (q < 6) {
            return "Critical";
        }
        return "Low";
    }

    private AlertDtos.AlertRow toRow(Article a) {
        return new AlertDtos.AlertRow(
                a.getId(),
                a.getName(),
                a.getSku(),
                a.getCategory() != null ? a.getCategory() : "",
                a.getQuantityOnHand(),
                a.getMinThreshold(),
                alertStatus(a),
                procurementPolicy.eligibleForPurchaseOrder(a),
                procurementPolicy.eligibleForReplenishment(a)
        );
    }
}
