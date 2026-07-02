package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.OrderStatus;
import com.stowflow.inventra.domain.Supplier;
import com.stowflow.inventra.domain.SupplierStatus;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.OrderDtos;
import com.stowflow.inventra.dto.SupplierDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.SupplierRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderService purchaseOrderService;

    @Transactional(readOnly = true)
    public List<SupplierDtos.SupplierResponse> list(Tenant tenant, String search) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return supplierRepository.findActiveByTenantId(tenant.getId()).stream()
                .filter(s -> q.isEmpty()
                        || s.getName().toLowerCase(Locale.ROOT).contains(q)
                        || (s.getContactEmail() != null && s.getContactEmail().toLowerCase(Locale.ROOT).contains(q)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierDtos.SupplierDetailResponse getDetail(Tenant tenant, Long id) {
        Supplier s = loadOwned(tenant, id);
        List<OrderDtos.OrderResponse> orders = purchaseOrderService.listForSupplier(tenant, id, null, null);
        long pending = orders.stream().filter(o -> "Pending".equals(o.status())).count();
        long received = orders.stream().filter(o -> "Received".equals(o.status())).count();
        long cancelled = orders.stream().filter(o -> "Cancelled".equals(o.status())).count();
        return new SupplierDtos.SupplierDetailResponse(
                s.getId(),
                s.getName(),
                s.getContactEmail() != null ? s.getContactEmail() : "",
                s.getCountry() != null ? s.getCountry() : "",
                s.getLinkedProductCount(),
                s.getLeadTimeDays() + "d",
                formatSupplierStatus(s.getStatus()),
                orders.size(),
                pending,
                received,
                cancelled,
                orders
        );
    }

    @Transactional
    public SupplierDtos.SupplierResponse create(Tenant tenant, SupplierDtos.CreateSupplierRequest req) {
        SupplierStatus st = req.status() != null ? req.status() : SupplierStatus.ACTIVE;
        Supplier s = Supplier.builder()
                .tenant(tenant)
                .name(req.name().trim())
                .contactEmail(req.contactEmail() != null ? req.contactEmail().trim() : null)
                .country(req.country() != null ? req.country().trim() : "")
                .leadTimeDays(req.leadTimeDays())
                .status(st)
                .linkedProductCount(0)
                .archived(false)
                .build();
        return toResponse(supplierRepository.save(s));
    }

    @Transactional
    public SupplierDtos.SupplierResponse update(Tenant tenant, Long id, SupplierDtos.UpdateSupplierRequest req) {
        Supplier s = loadOwned(tenant, id);
        s.setName(req.name().trim());
        s.setContactEmail(req.contactEmail() != null ? req.contactEmail().trim() : null);
        s.setCountry(req.country() != null ? req.country().trim() : "");
        s.setLeadTimeDays(req.leadTimeDays());
        s.setStatus(req.status());
        return toResponse(supplierRepository.save(s));
    }

    @Transactional
    public void archive(Tenant tenant, Long id) {
        Supplier s = loadOwned(tenant, id);
        s.setArchived(true);
        supplierRepository.save(s);
    }

    private Supplier loadOwned(Tenant tenant, Long id) {
        Supplier s = supplierRepository.findById(id).orElseThrow(() -> new BusinessException("Fournisseur introuvable."));
        if (!s.getTenant().getId().equals(tenant.getId()) || s.isArchived()) {
            throw new BusinessException("Fournisseur introuvable.");
        }
        return s;
    }

    private SupplierDtos.SupplierResponse toResponse(Supplier s) {
        return new SupplierDtos.SupplierResponse(
                s.getId(),
                s.getName(),
                s.getContactEmail() != null ? s.getContactEmail() : "",
                s.getCountry() != null ? s.getCountry() : "",
                s.getLinkedProductCount(),
                s.getLeadTimeDays() + "d",
                formatSupplierStatus(s.getStatus())
        );
    }

    private static String formatSupplierStatus(SupplierStatus st) {
        return switch (st) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case ON_HOLD -> "On Hold";
        };
    }
}
